package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.*;
import com.tornillos.model.*;
import com.tornillos.service.AlertaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.util.FolioGenerator;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;

public class SalidasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar, txtDesde, txtHasta;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    private final SalidaDAO salidaDAO     = new SalidaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final AlertaService alertaService = new AlertaService();

    private final String[] MOTIVOS = {"Venta","Uso Interno","Muestra","Devolucion","Merma","Otro"};

    public SalidasPanel(MainFrame frame) {
        this.mainFrame = frame;
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(AppTheme.BG_SURFACE);
        main.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(buildContent(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        JLabel title = new JLabel("Modulo de Salidas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        lblConteo = new JLabel("");
        lblConteo.setFont(AppTheme.FONT_SMALL);
        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        left.add(title); left.add(lblConteo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton btnNueva   = AppTheme.dangerButton("+ Nueva Salida");
        btnNueva.addActionListener(e -> abrirFormularioSalida());
        right.add(btnNueva);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        // Filtros
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        txtBuscar = AppTheme.styledField("Buscar por folio, tornillo, cliente...");
        txtBuscar.setPreferredSize(new Dimension(260, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { buscar(); }
        });
        txtDesde = AppTheme.styledField("Desde YYYY-MM-DD");
        txtDesde.setPreferredSize(new Dimension(148, 34));
        txtDesde.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });
        txtHasta = AppTheme.styledField("Hasta YYYY-MM-DD");
        txtHasta.setPreferredSize(new Dimension(148, 34));
        txtHasta.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });
        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        JButton btnLimpiar = AppTheme.secondaryButton("X Limpiar");
        btnFiltrar.addActionListener(e -> refresh());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText(""); txtDesde.setText(""); txtHasta.setText(""); refresh();
        });
        bar.add(txtBuscar);
        bar.add(AppTheme.label("Desde:")); bar.add(txtDesde);
        bar.add(AppTheme.label("Hasta:")); bar.add(txtHasta);
        bar.add(btnFiltrar); bar.add(btnLimpiar);
        p.add(bar, BorderLayout.NORTH);

        // Tabla
        String[] cols = {"ID","Folio","Tornillo","Codigo","Motivo","Cliente","Cantidad","P.Unitario","Total","Usuario","Fecha"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? AppTheme.BG_CARD : AppTheme.BG_SURFACE);
                    c.setForeground(AppTheme.TEXT_PRIMARY);
                } else {
                    c.setBackground(AppTheme.ACCENT);
                    c.setForeground(AppTheme.GOLD_LIGHT);
                }
                return c;
            }
        };
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) table.setRowSelectionInterval(row, row);
                }
            }
        });

        // Menu contextual
        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEliminar = AppTheme.darkMenuItem("Eliminar salida (revierte stock)", null);
        itemEliminar.addActionListener(e -> eliminarSeleccionada());
        if (SessionManager.getInstance().isGerente()) {
            popup.add(itemEliminar);
        }
        table.setComponentPopupMenu(popup);

        p.add(AppTheme.darkScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void buscar() {
        refresh();
    }

    private void eliminarSeleccionada() {
        if (!SessionManager.getInstance().isGerente()) return;
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona una salida."); return; }
        String folio = tableModel.getValueAt(row, 1).toString();
        int opt = JOptionPane.showConfirmDialog(this,
            "Eliminar salida " + folio + "?\nEsto revertira el stock del tornillo.",
            "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                salidaDAO.eliminar((int) tableModel.getValueAt(row, 0));
                refresh();
                JOptionPane.showMessageDialog(this, "Salida eliminada y stock revertido.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirFormularioSalida() {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
            "Registrar Salida", true);
        dlg.setSize(540, 480);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AppTheme.BG_CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        List<Tornillo> tornillos;
        try { tornillos = tornilloDAO.listarTodos(); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); return; }

        JComboBox<Tornillo> cmbTornillo = new JComboBox<>(tornillos.toArray(new Tornillo[0]));
        cmbTornillo.setBackground(AppTheme.BG_CARD_HOVER);
        cmbTornillo.setForeground(AppTheme.TEXT_PRIMARY);
        JComboBox<String> cmbMotivo = AppTheme.styledCombo(MOTIVOS);
        JTextField txtCantidad = AppTheme.styledField("0");
        JTextField txtPrecio   = AppTheme.styledField("0.00");
        JTextField txtCliente  = AppTheme.styledField("Nombre del cliente (opcional)");
        JTextArea txtObs       = AppTheme.styledTextArea(); txtObs.setRows(2);

        // Label stock disponible
        JLabel lblStock = new JLabel("Stock disponible: --");
        lblStock.setFont(AppTheme.FONT_SMALL);
        lblStock.setForeground(AppTheme.SUCCESS_TEXT);
        cmbTornillo.addActionListener(e -> {
            Tornillo t = (Tornillo) cmbTornillo.getSelectedItem();
            if (t != null) {
                int stock = t.getStockActual();
                lblStock.setText("Stock disponible: " + stock + " " + t.getUnidadMedida());
                lblStock.setForeground(stock <= t.getStockMinimo() ? AppTheme.WARNING : AppTheme.SUCCESS_TEXT);
                if (t.getPrecioVenta() != null) txtPrecio.setText(t.getPrecioVenta().toString());
            }
        });
        if (cmbTornillo.getItemCount() > 0) cmbTornillo.setSelectedIndex(0);

        String folio = FolioGenerator.generarSalida();
        JTextField txtFolio = AppTheme.styledField(folio);
        txtFolio.setText(folio); txtFolio.setEditable(false);
        txtFolio.setForeground(AppTheme.TEXT_MUTED);

        addRow(form, gbc, 0, "Folio:", txtFolio);
        addRow(form, gbc, 1, "Tornillo:", cmbTornillo);
        gbc.gridx = 1; gbc.gridy = 2; form.add(lblStock, gbc);
        addRow(form, gbc, 3, "Motivo:", cmbMotivo);
        addRow(form, gbc, 4, "Cliente:", txtCliente);
        addRow(form, gbc, 5, "Cantidad:", txtCantidad);
        addRow(form, gbc, 6, "Precio Unitario:", txtPrecio);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnCancel  = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.dangerButton("Registrar Salida");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            try {
                Tornillo t   = (Tornillo) cmbTornillo.getSelectedItem();
                int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
                if (cantidad <= 0) throw new IllegalArgumentException("Cantidad debe ser mayor a 0");

                Salida s = new Salida();
                s.setFolio(folio);
                s.setTornilloId(t.getId());
                s.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
                s.setCantidad(cantidad);
                s.setPrecioUnitario(precio);
                s.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)));
                s.setMotivo(cmbMotivo.getSelectedItem().toString());
                s.setCliente(txtCliente.getText().trim());
                s.setObservaciones(txtObs.getText().trim());

                salidaDAO.registrar(s);
                dlg.dispose();

                // El sistema dispara alertas automaticamente (caso de uso: actor Sistema)
                new Thread(() -> {
                    alertaService.verificarAlertas();
                    SwingUtilities.invokeLater(() -> mainFrame.actualizarBadgeAlertas());
                }).start();

                refresh();
                JOptionPane.showMessageDialog(this,
                    "Salida registrada. Folio: " + folio, "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Datos numericos invalidos.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        form.add(btns, gbc);
        btns.add(btnCancel); btns.add(btnGuardar);
        dlg.add(new JScrollPane(form));
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.3;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        p.add(field, gbc);
    }

    private void poblarTabla(List<Salida> lista) {
        tableModel.setRowCount(0);
        for (Salida s : lista) {
            tableModel.addRow(new Object[]{
                s.getId(), s.getFolio(),
                s.getTornilloNombre(), s.getTornilloCodigo(),
                s.getMotivo(), s.getCliente(),
                s.getCantidad(), s.getPrecioUnitario(), s.getTotal(),
                s.getUsuarioNombre(),
                s.getFecha() != null ? s.getFecha().toString().substring(0, 16) : ""
            });
        }
        lblConteo.setText(lista.size() + " salida(s)");
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Salida>, Void>() {
            @Override protected List<Salida> doInBackground() throws Exception {
                String termino = txtBuscar.getText().trim();
                String desde = txtDesde.getText().trim().isEmpty() ? null : txtDesde.getText().trim();
                String hasta = txtHasta.getText().trim().isEmpty() ? null : txtHasta.getText().trim();
                return salidaDAO.buscar(termino, desde, hasta);
            }
            @Override protected void done() {
                try { poblarTabla(get()); } catch (Exception ex) { /* ignored */ }
            }
        };
        currentWorker.execute();
    }
}
