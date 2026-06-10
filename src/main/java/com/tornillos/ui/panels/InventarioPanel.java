package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Tornillo;
import com.tornillos.service.AlertaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.ui.dialogs.TornilloDialog;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class InventarioPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private JComboBox<String> cmbEstado;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    private final MainFrame mainFrame;
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final AlertaService alertaService = new AlertaService();

    public InventarioPanel(MainFrame frame) {
        this.mainFrame = frame;
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
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
        JLabel title = new JLabel("Inventario de Tornillos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        lblConteo = new JLabel("Cargando...");
        lblConteo.setFont(AppTheme.FONT_SMALL);
        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        left.add(title);
        left.add(lblConteo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        if (SessionManager.getInstance().isGerente()) {
            JButton btnAgregar = AppTheme.primaryButton("+ Agregar Tornillo");
            btnAgregar.addActionListener(e -> abrirDialogo(null));
            right.add(btnAgregar);
        }

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.add(buildFilterBar(), BorderLayout.NORTH);
        p.add(buildTable(), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        txtBuscar = AppTheme.styledField("Buscar por código, nombre, material...");
        txtBuscar.setPreferredSize(new Dimension(300, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                buscarConFiltro();
            }
        });

        cmbEstado = AppTheme
                .styledCombo(new String[] { "Todos los estados", "Normal", "Stock Bajo", "Crítico", "Sin Stock" });
        cmbEstado.setPreferredSize(new Dimension(160, 34));
        cmbEstado.addActionListener(e -> buscarConFiltro());

        JButton btnLimpiar = AppTheme.secondaryButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            cmbEstado.setSelectedIndex(0);
            buscarConFiltro();
        });

        bar.add(txtBuscar);
        bar.add(cmbEstado);
        bar.add(btnLimpiar);
        return bar;
    }

    private JScrollPane buildTable() {
        String[] cols = { "ID", "Código", "Nombre", "Material", "Diam.mm", "Long.mm", "Stock", "Mín.",
                "P.Venta", "Estado", "Ubicación" };
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
                    if (col == 9) {
                        Object val = getValueAt(row, col);
                        if (val != null) {
                            String sv = val.toString();
                            if ("SIN STOCK".equals(sv))
                                c.setForeground(AppTheme.DANGER_TEXT);
                            else if ("CRÍTICO".equals(sv))
                                c.setForeground(AppTheme.WARNING_TEXT);
                            else if ("BAJO".equals(sv))
                                c.setForeground(AppTheme.WARNING_TEXT);
                            else
                                c.setForeground(AppTheme.SUCCESS_TEXT);
                        }
                    } else {
                        c.setForeground(AppTheme.TEXT_PRIMARY);
                    }
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

        int[] widths = { 0, 90, 200, 100, 65, 70, 55, 45, 90, 80, 110 };
        for (int i = 1; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SessionManager.getInstance().isGerente()) {
                    int row = table.getSelectedRow();
                    if (row >= 0)
                        abrirDialogoPorId((int) tableModel.getValueAt(row, 0));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0)
                    table.setRowSelectionInterval(row, row);
            }
        });

        buildContextMenu();

        return AppTheme.darkScrollPane(table);
    }

    private void buildContextMenu() {
        JPopupMenu popup = AppTheme.darkPopup();

        JMenuItem verItem = AppTheme.darkMenuItem("Ver stock actual", null);
        verItem.addActionListener(e -> verStock());
        popup.add(verItem);

        if (SessionManager.getInstance().isGerente()) {
            JMenuItem editItem = AppTheme.darkMenuItem("Editar tornillo", null);
            editItem.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0)
                    abrirDialogoPorId((int) tableModel.getValueAt(row, 0));
            });
            popup.add(AppTheme.darkSeparator());
            popup.add(editItem);

            JMenuItem bajaItem = AppTheme.darkMenuItem("Dar de baja tornillo", null);
            bajaItem.setForeground(AppTheme.WARNING_TEXT);
            bajaItem.addActionListener(e -> darDeBaja());
            popup.add(bajaItem);

            popup.add(AppTheme.darkSeparator());
            JMenuItem eliminarItem = AppTheme.darkMenuItem("Eliminar permanentemente", null);
            eliminarItem.setForeground(AppTheme.DANGER_TEXT);
            eliminarItem.addActionListener(e -> eliminarSeleccionado());
            popup.add(eliminarItem);
        }

        table.setComponentPopupMenu(popup);
    }

    private void verStock() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        String nombre = tableModel.getValueAt(row, 2).toString();
        int stock = (int) tableModel.getValueAt(row, 6);
        int minimo = (int) tableModel.getValueAt(row, 7);
        String estado = tableModel.getValueAt(row, 9).toString();

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.setBackground(AppTheme.BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        panel.add(AppTheme.label("Tornillo:"));
        JLabel lNombre = new JLabel(nombre);
        lNombre.setForeground(AppTheme.TEXT_PRIMARY);
        lNombre.setFont(AppTheme.FONT_BOLD);
        panel.add(lNombre);

        panel.add(AppTheme.label("Stock actual:"));
        JLabel lStock = new JLabel(String.valueOf(stock));
        lStock.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lStock.setForeground(stock == 0 ? AppTheme.DANGER : stock <= minimo ? AppTheme.WARNING : AppTheme.SUCCESS);
        panel.add(lStock);

        panel.add(AppTheme.label("Stock mínimo:"));
        JLabel lMin = new JLabel(String.valueOf(minimo));
        lMin.setForeground(AppTheme.TEXT_PRIMARY);
        lMin.setFont(AppTheme.FONT_BODY);
        panel.add(lMin);

        panel.add(AppTheme.label("Estado:"));
        JLabel lEstado = new JLabel(estado);
        lEstado.setFont(AppTheme.FONT_BOLD);
        lEstado.setForeground(
                "SIN STOCK".equals(estado) ? AppTheme.DANGER
                        : "CRÍTICO".equals(estado) ? AppTheme.WARNING
                                : "BAJO".equals(estado) ? AppTheme.WARNING : AppTheme.SUCCESS);
        panel.add(lEstado);

        JOptionPane.showMessageDialog(this, panel, "Consultar Stock", JOptionPane.PLAIN_MESSAGE);
    }

    private void darDeBaja() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        int id = (int) tableModel.getValueAt(row, 0);
        String nombre = tableModel.getValueAt(row, 2).toString();
        int opt = JOptionPane.showConfirmDialog(this,
                "Dar de baja a '" + nombre + "'?\n" +
                        "El tornillo quedará inactivo pero su historial se conserva.\n" +
                        "Puedes reactivarlo editando el registro.",
                "Dar de baja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION)
            return;
        try {
            tornilloDAO.darDeBaja(id);
            refresh();
            JOptionPane.showMessageDialog(this,
                    "'" + nombre + "' dado de baja correctamente.", "Listo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        int id = (int) tableModel.getValueAt(row, 0);
        String nombre = tableModel.getValueAt(row, 2).toString();
        int opt = JOptionPane.showConfirmDialog(this,
                "ELIMINAR PERMANENTEMENTE '" + nombre + "'?\n" +
                        "Esta acción no se puede deshacer.",
                "Eliminar", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (opt != JOptionPane.YES_OPTION)
            return;
        try {
            tornilloDAO.eliminar(id);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirDialogo(Tornillo tornillo) {
        TornilloDialog dlg = new TornilloDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), tornillo);
        dlg.setVisible(true);
        if (dlg.isGuardado()) {
            refresh();
            alertaService.verificarAlertas();
            mainFrame.actualizarBadgeAlertas();
        }
    }

    private void abrirDialogoPorId(int id) {
        try {
            Tornillo t = tornilloDAO.obtenerPorId(id);
            abrirDialogo(t);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarConFiltro() {
        String termino = txtBuscar.getText().trim();
        String[] estadoMap = { null, "NORMAL", "BAJO", "CRÍTICO", "SIN_STOCK" };
        String estado = estadoMap[Math.min(cmbEstado.getSelectedIndex(), estadoMap.length - 1)];

        final String ft = termino;
        final String fe = estado;

        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Tornillo>, Void>() {
            @Override
            protected List<Tornillo> doInBackground() throws Exception {
                return tornilloDAO.listarConFiltro(ft, fe);
            }

            @Override
            protected void done() {
                try {
                    poblarTabla(get());
                } catch (Exception e) {
                }
            }
        };
        currentWorker.execute();
    }

    private void poblarTabla(List<Tornillo> lista) {
        tableModel.setRowCount(0);
        for (Tornillo t : lista) {
            String est0 = t.getEstadoStock();
            String estado;
            if ("SIN_STOCK".equals(est0))
                estado = "SIN STOCK";
            else if ("CRÍTICO".equals(est0))
                estado = "CRÍTICO";
            else if ("BAJO".equals(est0))
                estado = "BAJO";
            else
                estado = "NORMAL";

            tableModel.addRow(new Object[] {
                    t.getId(), t.getCodigo(), t.getNombre(),
                    t.getMaterial(),
                    t.getDiametroMm() != null ? t.getDiametroMm() : "",
                    t.getLongitudMm() != null ? t.getLongitudMm() : "",
                    t.getStockActual(), t.getStockMinimo(),
                    t.getPrecioVenta(), estado, t.getUbicacion()
            });
        }
        lblConteo.setText(lista.size() + " producto(s) encontrado(s)");
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Tornillo>, Void>() {
            @Override
            protected List<Tornillo> doInBackground() throws Exception {
                return tornilloDAO.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    poblarTabla(get());
                } catch (Exception e) {
                }
            }
        };
        currentWorker.execute();
    }
}
