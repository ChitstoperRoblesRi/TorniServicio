package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.*;
import com.tornillos.model.*;
import com.tornillos.service.AlertaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.ui.dialogs.TornilloDialog;
import com.tornillos.util.FolioGenerator;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

public class EntradasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar, txtDesde, txtHasta;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    private final EntradaDAO entradaDAO = new EntradaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final AlertaService alertaService = new AlertaService();

    public EntradasPanel(MainFrame frame) {
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
        JLabel title = new JLabel("Módulo de Entradas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        lblConteo = new JLabel("");
        lblConteo.setFont(AppTheme.FONT_SMALL);
        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        left.add(title);
        left.add(lblConteo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton btnNueva = AppTheme.successButton("+ Nueva Entrada");
        btnNueva.addActionListener(e -> abrirFormularioEntrada());
        right.add(btnNueva);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        txtBuscar = AppTheme.styledField("Buscar por folio, tornillo...");
        txtBuscar.setPreferredSize(new Dimension(260, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                buscar();
            }
        });
        txtDesde = AppTheme.styledField("Desde YYYY-MM-DD");
        txtDesde.setPreferredSize(new Dimension(150, 34));
        txtDesde.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });
        txtHasta = AppTheme.styledField("Hasta YYYY-MM-DD");
        txtHasta.setPreferredSize(new Dimension(150, 34));
        txtHasta.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });

        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        JButton btnLimpiar = AppTheme.secondaryButton("X Limpiar");
        btnFiltrar.addActionListener(e -> refresh());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            txtDesde.setText("");
            txtHasta.setText("");
            refresh();
        });

        bar.add(txtBuscar);
        bar.add(AppTheme.label("Desde:"));
        bar.add(txtDesde);
        bar.add(AppTheme.label("Hasta:"));
        bar.add(txtHasta);
        bar.add(btnFiltrar);
        bar.add(btnLimpiar);
        p.add(bar, BorderLayout.NORTH);

        String[] cols = { "ID", "Folio", "Tornillo", "Codigo", "Cantidad", "P.Unitario", "Total", "Usuario", "Fecha" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
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
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0)
                        table.setRowSelectionInterval(row, row);
                }
            }
        });

        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEliminar = AppTheme.darkMenuItem("Eliminar entrada (revierte stock)", null);
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
        if (!SessionManager.getInstance().isGerente())
            return;
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una entrada.");
            return;
        }
        String folio = tableModel.getValueAt(row, 1).toString();
        int opt = JOptionPane.showConfirmDialog(this,
                "Eliminar entrada " + folio + "?\nEsto revertira el stock del tornillo.",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                entradaDAO.eliminar((int) tableModel.getValueAt(row, 0));
                alertaService.verificarAlertas();
                mainFrame.actualizarBadgeAlertas();
                refresh();
                JOptionPane.showMessageDialog(this, "Entrada eliminada y stock revertido.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirFormularioEntrada() {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                "Registrar Entrada", true);
        dlg.setSize(520, 480);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AppTheme.BG_CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        List<Tornillo> tornillos;
        try {
            tornillos = tornilloDAO.listarTodos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando datos: " + e.getMessage());
            return;
        }

        JComboBox<Tornillo> cmbTornillo = new JComboBox<>(tornillos.toArray(new Tornillo[0]));
        cmbTornillo.setBackground(AppTheme.BG_CARD_HOVER);
        cmbTornillo.setForeground(AppTheme.TEXT_PRIMARY);

        JTextField txtCantidad = AppTheme.styledField("0");
        JTextField txtPrecio = AppTheme.styledField("0.00");
        JTextField txtFactura = AppTheme.styledField("Numero de factura (opcional)");
        JTextArea txtObs = AppTheme.styledTextArea();
        txtObs.setRows(2);

        String folio = FolioGenerator.generarEntrada();
        JTextField txtFolio = AppTheme.styledField(folio);
        txtFolio.setText(folio);
        txtFolio.setEditable(false);
        txtFolio.setForeground(AppTheme.TEXT_MUTED);

        addRow(form, gbc, 0, "Folio:", txtFolio);
        addRow(form, gbc, 1, "Tornillo:", cmbTornillo);
        addRow(form, gbc, 2, "Cantidad:", txtCantidad);
        addRow(form, gbc, 3, "Precio Unitario:", txtPrecio);
        addRow(form, gbc, 4, "No. Factura:", txtFactura);
        gbc.gridx = 0;
        gbc.gridy = 5;
        form.add(AppTheme.label("Observaciones:"), gbc);
        gbc.gridx = 1;
        form.add(AppTheme.darkScrollPane(txtObs), gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnCancel = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.successButton("Registrar Entrada");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            try {
                Tornillo t = (Tornillo) cmbTornillo.getSelectedItem();
                int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
                if (cantidad <= 0)
                    throw new IllegalArgumentException("Cantidad debe ser mayor a 0");

                Entrada entrada = new Entrada();
                entrada.setFolio(folio);
                entrada.setTornilloId(t.getId());
                entrada.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
                entrada.setCantidad(cantidad);
                entrada.setPrecioUnitario(precio);
                entrada.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)));
                entrada.setNumeroFactura(txtFactura.getText().trim());
                entrada.setObservaciones(txtObs.getText().trim());

                entradaDAO.registrar(entrada);
                dlg.dispose();
                refresh();

                alertaService.verificarAlertas();
                mainFrame.actualizarBadgeAlertas();

                JOptionPane.showMessageDialog(this,
                        "Entrada registrada. Folio: " + folio, "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Cantidad y precio deben ser numeros validos.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        form.add(btns, gbc);
        btnCancel.addActionListener(ev -> dlg.dispose());
        btns.add(btnCancel);
        btns.add(btnGuardar);
        dlg.add(new JScrollPane(form));
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        p.add(field, gbc);
    }

    private void poblarTabla(List<Entrada> lista) {
        tableModel.setRowCount(0);
        for (Entrada e : lista) {
            tableModel.addRow(new Object[] {
                    e.getId(), e.getFolio(),
                    e.getTornilloNombre(), e.getTornilloCodigo(),
                    e.getCantidad(), e.getPrecioUnitario(), e.getTotal(),
                    e.getUsuarioNombre(),
                    e.getFecha() != null ? e.getFecha().toString().substring(0, 16) : ""
            });
        }
        lblConteo.setText(lista.size() + " entrada(s)");
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Entrada>, Void>() {
            @Override
            protected List<Entrada> doInBackground() throws Exception {
                String termino = txtBuscar.getText().trim();
                String desde = txtDesde.getText().trim().isEmpty() ? null : txtDesde.getText().trim();
                String hasta = txtHasta.getText().trim().isEmpty() ? null : txtHasta.getText().trim();
                return entradaDAO.buscar(termino, desde, hasta);
            }

            @Override
            protected void done() {
                try {
                    poblarTabla(get());
                } catch (Exception ex) {
                }
            }
        };
        currentWorker.execute();
    }
}
