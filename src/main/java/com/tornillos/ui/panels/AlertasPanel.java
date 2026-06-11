package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.AlertaDAO;
import com.tornillos.model.Alerta;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class AlertasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;
    private final AlertaDAO alertaDAO = new AlertaDAO();

    public AlertasPanel(MainFrame frame) {
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
        JLabel title = new JLabel("Centro de Alertas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        lblConteo = new JLabel("Cargando...");
        lblConteo.setFont(AppTheme.FONT_SMALL);
        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        left.add(title);
        left.add(lblConteo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btnHistorial = AppTheme.secondaryButton("Ver historial");
        btnHistorial.addActionListener(e -> mostrarHistorial());

        if (SessionManager.getInstance().isGerente()) {
            JButton btnEliminarTodas = AppTheme.dangerButton("Eliminar todas");
            btnEliminarTodas.addActionListener(e -> eliminarTodas());
            right.add(btnEliminarTodas);
        }
        right.add(btnHistorial);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        txtBuscar = AppTheme.styledField("Buscar por tornillo, tipo o mensaje...");
        txtBuscar.setPreferredSize(new Dimension(340, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                buscar();
            }
        });
        JButton btnBuscar = AppTheme.primaryButton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        JButton btnLimpiar = AppTheme.secondaryButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            refresh();
        });
        bar.add(txtBuscar);
        bar.add(btnBuscar);
        bar.add(btnLimpiar);
        p.add(bar, BorderLayout.NORTH);

        String[] cols = { "ID", "Tipo", "Tornillo", "Codigo", "Mensaje", "Fecha" };
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
                Object tipo = getValueAt(row, 1);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? AppTheme.BG_CARD : AppTheme.BG_SURFACE);
                    if (tipo != null) {
                        String t = tipo.toString();
                        if ("SIN_STOCK".equals(t))
                            c.setForeground(AppTheme.DANGER_TEXT);
                        else if ("STOCK_CRITICO".equals(t))
                            c.setForeground(AppTheme.WARNING_TEXT);
                        else if ("STOCK_BAJO".equals(t))
                            c.setForeground(AppTheme.WARNING_TEXT);
                        else
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

        // Inicializar JPopupMenu local con retorno controlado
        final JPopupMenu menuContextual = buildContextMenu();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                evaluarClicContextual(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                evaluarClicContextual(e);
            }

            private void evaluarClicContextual(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    
                    // CORRECCIÓN: Desplegar el menú únicamente si golpea un registro real de la tabla
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        menuContextual.show(table, e.getX(), e.getY());
                    } else {
                        table.clearSelection();
                    }
                }
            }
        });

        p.add(AppTheme.darkScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEliminar = AppTheme.darkMenuItem("Eliminar alerta", null);
        itemEliminar.addActionListener(e -> eliminarSeleccionada());
        if (SessionManager.getInstance().isGerente()) {
            popup.add(itemEliminar);
        }
        
        // CORRECCIÓN: Removida la instrucción table.setComponentPopupMenu(popup) global.
        return popup;
    }

    private void buscar() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        String termino = txtBuscar.getText().trim();
        if (termino.isEmpty()) {
            refresh();
            return;
        }
        currentWorker = new SwingWorker<List<Alerta>, Void>() {
            @Override
            protected List<Alerta> doInBackground() throws Exception {
                return alertaDAO.buscar(termino);
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

    private void eliminarSeleccionada() {
        if (!SessionManager.getInstance().isGerente())
            return;
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Eliminar esta alerta permanentemente?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                alertaDAO.eliminar((int) tableModel.getValueAt(row, 0));
                mainFrame.actualizarBadgeAlertas();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarTodas() {
        if (!SessionManager.getInstance().isGerente())
            return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Eliminar TODAS las alertas permanentemente?\nEsta accion no se puede deshacer.",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                alertaDAO.eliminarTodas();
                mainFrame.actualizarBadgeAlertas();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void mostrarHistorial() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Historial de alertas",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Historial de alertas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(titulo, BorderLayout.NORTH);

        String[] cols = { "ID", "Tipo", "Tornillo", "Codigo", "Mensaje", "Email", "Fecha" };
        DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable histTable = new JTable(histModel);
        AppTheme.styleTable(histTable);
        histTable.getColumnModel().getColumn(0).setMinWidth(0);
        histTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane scroll = AppTheme.darkScrollPane(histTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnCerrar = AppTheme.primaryButton("Cerrar");
        btnCerrar.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        footer.add(btnCerrar);
        panel.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(panel);

        SwingWorker<List<Alerta>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Alerta> doInBackground() throws Exception {
                return alertaDAO.listarHistorial();
            }

            @Override
            protected void done() {
                try {
                    List<Alerta> lista = get();
                    for (Alerta a : lista) {
                        histModel.addRow(new Object[] {
                                a.getId(), a.getTipo(),
                                a.getTornilloNombre(), a.getTornilloCodigo(),
                                a.getMensaje(),
                                a.isEnviadaEmail() ? "Si" : "No",
                                a.getCreadaEn() != null ? a.getCreadaEn().toString().substring(0, 16) : ""
                        });
                    }
                } catch (Exception ex) {
                }
            }
        };
        w.execute();

        dialog.setVisible(true);
    }

    private void poblarTabla(List<Alerta> lista) {
        tableModel.setRowCount(0);
        for (Alerta a : lista) {
            tableModel.addRow(new Object[] {
                    a.getId(), a.getTipo(),
                    a.getTornilloNombre(), a.getTornilloCodigo(),
                    a.getMensaje(),
                    a.getCreadaEn() != null ? a.getCreadaEn().toString().substring(0, 16) : ""
            });
        }
        lblConteo.setText(lista.size() + " alerta(s) activa(s)");
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Alerta>, Void>() {
            @Override
            protected List<Alerta> doInBackground() throws Exception {
                return alertaDAO.listarActivas();
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