package com.tornillos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Alerta;
import com.tornillos.service.AlertaService;
import com.tornillos.ui.MainFrame;

public class AlertasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    private String filtroTipoSelected = null; 
    private JButton chipTodos, chipSinStock, chipCriticos, chipBajos;

    private final java.time.format.DateTimeFormatter visualFormatter =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AlertaService alertaService = new AlertaService();

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

        right.add(btnHistorial);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel topContainer = new JPanel(new GridLayout(2, 1, 0, 8));
        topContainer.setOpaque(false);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        txtBuscar = AppTheme.styledField("Buscar por tornillo o código...");
        txtBuscar.setPreferredSize(new Dimension(380, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                buscar();
            }
        });

        JButton btnLimpiar = AppTheme.secondaryButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            cambiarSeleccionChip(null);
            buscar();
        });

        bar.add(txtBuscar);
        bar.add(btnLimpiar);
        topContainer.add(bar);

        JPanel panelChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelChips.setOpaque(false);
        panelChips.add(AppTheme.label("Filtros rápidos:"));

        chipTodos = AppTheme.secondaryButton("Todos");
        chipSinStock = AppTheme.secondaryButton("Sin Stock");
        chipCriticos = AppTheme.secondaryButton("Críticos");
        chipBajos = AppTheme.secondaryButton("Bajos");

        chipTodos.addActionListener(e -> { cambiarSeleccionChip(null); buscar(); });
        chipSinStock.addActionListener(e -> { cambiarSeleccionChip("SIN_STOCK"); buscar(); });
        chipCriticos.addActionListener(e -> { cambiarSeleccionChip("STOCK_CRITICO"); buscar(); });
        chipBajos.addActionListener(e -> { cambiarSeleccionChip("STOCK_BAJO"); buscar(); });

        panelChips.add(chipTodos); panelChips.add(chipSinStock); 
        panelChips.add(chipCriticos); panelChips.add(chipBajos);
        topContainer.add(panelChips);

        cambiarSeleccionChip(null);

        p.add(topContainer, BorderLayout.NORTH);

        String[] cols = { "ID", "Tipo", "Tornillo", "Código", "Mensaje", "Fecha" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                Object tipo = getValueAt(row, 1);

                if (!isRowSelected(row) && tipo != null) {
                    String t = tipo.toString();
                    if ("Sin Stock".equals(t) || "SIN_STOCK".equals(t)) {
                        c.setForeground(AppTheme.DANGER_TEXT);
                    } else if ("Crítico".equals(t) || "STOCK_CRITICO".equals(t)) {
                        c.setForeground(AppTheme.WARNING_TEXT);
                    } else if ("Bajo".equals(t) || "STOCK_BAJO".equals(t)) {
                        c.setForeground(AppTheme.WARNING_TEXT);
                    }
                }

                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    if (col == 1 || col == 3 || col == 5) {
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    } else {
                        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    }
                }
                return c;
            }
        };

        AppTheme.styleTable(table);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int row = table.getSelectedRow();
                    String tornilloNombre = table.getValueAt(row, 2).toString();
                    String codigo = table.getValueAt(row, 3).toString();

                    int opt = javax.swing.JOptionPane.showConfirmDialog(
                        AlertasPanel.this,
                        "¿Deseas registrar una entrada de almacén para el producto:\n" + tornilloNombre + " (" + codigo + ")?",
                        "Acceso Rápido a Reabastecimiento",
                        javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.QUESTION_MESSAGE
                    );

                    if (opt == javax.swing.JOptionPane.YES_OPTION) {
                        mainFrame.IrAEntradasYReabastecer(codigo);
                    }
                }
            }
        });

        for (int col : new int[]{ 1, 3, 5 }) {
            table.getColumnModel().getColumn(col).setHeaderRenderer(
                (t, val, sel, focus, row, c) -> {
                    Component comp = t.getTableHeader().getDefaultRenderer()
                            .getTableCellRendererComponent(t, val, sel, focus, row, c);
                    if (comp instanceof JLabel) {
                        ((JLabel) comp).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    }
                    return comp;
                }
            );
        }

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(320);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);

        p.add(AppTheme.darkScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void cambiarSeleccionChip(String tipo) {
        this.filtroTipoSelected = tipo;
        chipTodos.setForeground(tipo == null ? AppTheme.GOLD_LIGHT : AppTheme.TEXT_SECONDARY);
        chipTodos.setBorder(tipo == null ? BorderFactory.createLineBorder(AppTheme.GOLD, 1) : BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        
        chipSinStock.setForeground("SIN_STOCK".equals(tipo) ? AppTheme.GOLD_LIGHT : AppTheme.TEXT_SECONDARY);
        chipSinStock.setBorder("SIN_STOCK".equals(tipo) ? BorderFactory.createLineBorder(AppTheme.GOLD, 1) : BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        
        chipCriticos.setForeground("STOCK_CRITICO".equals(tipo) ? AppTheme.GOLD_LIGHT : AppTheme.TEXT_SECONDARY);
        chipCriticos.setBorder("STOCK_CRITICO".equals(tipo) ? BorderFactory.createLineBorder(AppTheme.GOLD, 1) : BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        
        chipBajos.setForeground("STOCK_BAJO".equals(tipo) ? AppTheme.GOLD_LIGHT : AppTheme.TEXT_SECONDARY);
        chipBajos.setBorder("STOCK_BAJO".equals(tipo) ? BorderFactory.createLineBorder(AppTheme.BORDER, 1) : BorderFactory.createLineBorder(AppTheme.BORDER, 1));
    }

    private void buscar() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        
        final String termino = txtBuscar.getText().trim();
        final String tipoFiltro = this.filtroTipoSelected;

        currentWorker = new SwingWorker<List<Alerta>, Void>() {
            @Override
            protected List<Alerta> doInBackground() throws Exception {
                return alertaService.buscarAlertasCombinadas(termino, tipoFiltro);
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        poblarTabla(get());
                    }
                } catch (Exception ex) {
                }
            }
        };
        currentWorker.execute();
    }

    private void mostrarHistorial() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Historial de alertas", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1050, 580);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        JLabel titulo = new JLabel("Historial de Alertas Completo");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(AppTheme.TEXT_PRIMARY);
        topPanel.add(titulo, BorderLayout.NORTH);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        java.time.format.DateTimeFormatter filterFormatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDate hoy       = java.time.LocalDate.now();
        java.time.LocalDate haceUnMes = hoy.minusDays(30);

        javax.swing.text.MaskFormatter mascaraDesde = null;
        javax.swing.text.MaskFormatter mascaraHasta = null;
        try {
            mascaraDesde = new javax.swing.text.MaskFormatter("####-##-##");
            mascaraDesde.setPlaceholderCharacter('_');
            mascaraDesde.setAllowsInvalid(false);
            mascaraDesde.setOverwriteMode(true);

            mascaraHasta = new javax.swing.text.MaskFormatter("####-##-##");
            mascaraHasta.setPlaceholderCharacter('_');
            mascaraHasta.setAllowsInvalid(false);
            mascaraHasta.setOverwriteMode(true);
        } catch (java.text.ParseException ex) {
        }

        final javax.swing.JFormattedTextField txtDesde = (mascaraDesde != null)
                ? new javax.swing.JFormattedTextField(mascaraDesde)
                : new javax.swing.JFormattedTextField();

        final javax.swing.JFormattedTextField txtHasta = (mascaraHasta != null)
                ? new javax.swing.JFormattedTextField(mascaraHasta)
                : new javax.swing.JFormattedTextField();

        txtDesde.setText(haceUnMes.format(filterFormatter));
        txtDesde.setPreferredSize(new Dimension(130, 34));
        txtDesde.setBackground(AppTheme.BG_CARD_HOVER);
        txtDesde.setForeground(AppTheme.TEXT_PRIMARY);
        txtDesde.setCaretColor(AppTheme.GOLD_LIGHT);
        txtDesde.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));

        txtHasta.setText(hoy.format(filterFormatter));
        txtHasta.setPreferredSize(new Dimension(130, 34));
        txtHasta.setBackground(AppTheme.BG_CARD_HOVER);
        txtHasta.setForeground(AppTheme.TEXT_PRIMARY);
        txtHasta.setCaretColor(AppTheme.GOLD_LIGHT);
        txtHasta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));

        final JTextField txtBuscarHist = AppTheme.styledField("Filtrar por tornillo o código...");
        txtBuscarHist.setPreferredSize(new Dimension(220, 34));

        java.awt.event.FocusAdapter focusAdapter = new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                ((JComponent) e.getSource()).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.GOLD_LIGHT, 1),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                ((JComponent) e.getSource()).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            }
        };
        txtDesde.addFocusListener(focusAdapter);
        txtHasta.addFocusListener(focusAdapter);
        txtBuscarHist.addFocusListener(focusAdapter);

        String[] cols = { "ID", "Tipo", "Tornillo", "Código", "Mensaje", "Email", "Fecha" };
        final DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        txtBuscarHist.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String rawDesde = txtDesde.getText().trim();
                String rawHasta = txtHasta.getText().trim();
                String desde = (rawDesde.isEmpty() || rawDesde.contains("_")) ? null : rawDesde;
                String hasta = (rawHasta.isEmpty() || rawHasta.contains("_")) ? null : rawHasta;
                
                cargarHistorialFiltrado(histModel, desde, hasta, txtBuscarHist.getText().trim());
            }
        });

        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        JButton btnLimpiar = AppTheme.secondaryButton("Limpiar");

        btnFiltrar.addActionListener(e -> {
            String rawDesde = txtDesde.getText().trim();
            String rawHasta = txtHasta.getText().trim();
            String desde = (rawDesde.isEmpty() || rawDesde.contains("_")) ? null : rawDesde;
            String hasta = (rawHasta.isEmpty() || rawHasta.contains("_")) ? null : rawHasta;
            cargarHistorialFiltrado(histModel, desde, hasta, txtBuscarHist.getText().trim());
        });

        btnLimpiar.addActionListener(e -> {
            java.time.LocalDate dHoy = java.time.LocalDate.now();
            txtDesde.setText(dHoy.minusDays(30).format(filterFormatter));
            txtHasta.setText(dHoy.format(filterFormatter));
            txtBuscarHist.setText("");
            cargarHistorialFiltrado(histModel,
                    dHoy.minusDays(30).format(filterFormatter),
                    dHoy.format(filterFormatter),
                    "");
        });

        bar.add(AppTheme.label("Buscar:")); bar.add(txtBuscarHist);
        bar.add(AppTheme.label("Desde:"));  bar.add(txtDesde);
        bar.add(AppTheme.label("Hasta:"));  bar.add(txtHasta);
        bar.add(btnFiltrar); 
        bar.add(btnLimpiar);

        topPanel.add(bar, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        JTable histTable = new JTable(histModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    if (col == 1 || col == 3 || col == 5 || col == 6) {
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    } else {
                        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    }
                }
                return c;
            }
        };
        AppTheme.styleTable(histTable);

        for (int col : new int[]{ 1, 3, 5, 6 }) {
            histTable.getColumnModel().getColumn(col).setHeaderRenderer(
                (t, val, sel, focus, row, c) -> {
                    Component comp = t.getTableHeader().getDefaultRenderer()
                            .getTableCellRendererComponent(t, val, sel, focus, row, c);
                    if (comp instanceof JLabel) {
                        ((JLabel) comp).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    }
                    return comp;
                }
            );
        }

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

        cargarHistorialFiltrado(histModel,
                haceUnMes.format(filterFormatter),
                hoy.format(filterFormatter),
                "");
        dialog.setVisible(true);
    }

    private void cargarHistorialFiltrado(DefaultTableModel histModel,
                                         String desde, String hasta, String termino) {
        histModel.setRowCount(0);

        SwingWorker<List<Alerta>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Alerta> doInBackground() throws Exception {
                return alertaService.obtenerHistorial(desde, hasta, termino);
            }

            @Override
            protected void done() {
                try {
                    List<Alerta> lista = get();
                    for (Alerta a : lista) {
                        String fechaFormateada = (a.getCreadaEn() != null)
                                ? a.getCreadaEn().format(visualFormatter) : "";
                        
                        String tipoUsuario = a.getTipo();
                        if ("SIN_STOCK".equals(tipoUsuario)) tipoUsuario = "Sin Stock";
                        else if ("STOCK_CRITICO".equals(tipoUsuario)) tipoUsuario = "Crítico";
                        else if ("STOCK_BAJO".equals(tipoUsuario)) tipoUsuario = "Bajo";

                        histModel.addRow(new Object[]{
                                a.getId(), 
                                tipoUsuario,
                                a.getTornilloNombre(), a.getTornilloCodigo(),
                                a.getMensaje(),
                                a.isEnviadaEmail() ? "Sí" : "No",
                                fechaFormateada
                        });
                    }
                } catch (Exception ex) {
                }
            }
        };
        w.execute();
    }

    private void poblarTabla(List<Alerta> lista) {
        tableModel.setRowCount(0);

        if (lista.isEmpty()) {
            lblConteo.setText("¡Sistema al día! No se encontraron alertas que requieran atención.");
            lblConteo.setForeground(AppTheme.SUCCESS_TEXT);
            return;
        }

        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        lblConteo.setText(lista.size() + " alerta(s) activa(s)");

        for (Alerta a : lista) {
            String fechaFormateada = (a.getCreadaEn() != null)
                    ? a.getCreadaEn().format(visualFormatter) : "";
            
            String tipoUsuario = a.getTipo();
            if ("SIN_STOCK".equals(tipoUsuario)) tipoUsuario = "Sin Stock";
            else if ("STOCK_CRITICO".equals(tipoUsuario)) tipoUsuario = "Crítico";
            else if ("STOCK_BAJO".equals(tipoUsuario)) tipoUsuario = "Bajo";

            tableModel.addRow(new Object[]{
                    a.getId(), 
                    tipoUsuario,
                    a.getTornilloNombre(), a.getTornilloCodigo(),
                    a.getMensaje(),
                    fechaFormateada
            });
        }
    }

    public void refresh() {
        buscar();
    }
}