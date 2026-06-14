package com.tornillos.ui.panels;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.MovimientoInventarioDAO;
import com.tornillos.model.MovimientoInventario;

public class ReportesPanel extends JPanel {

    private JFormattedTextField txtDesde, txtHasta;
    private JComboBox<String> cmbTipo;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblResumen;
    private SwingWorker<List<MovimientoInventario>, Void> currentWorker;

    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final DateTimeFormatter visualFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter filterFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] TIPOS_MOVIMIENTO = {
            "Todos los movimientos", "Creaci\u00f3n", "Entrada", "Salida"
    };

    public ReportesPanel() {
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        buildUI();
        
        refresh();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(AppTheme.BG_SURFACE);
        main.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(buildContent(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel title = new JLabel("Reportes y an\u00e1lisis");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.GOLD_LIGHT);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton btnCSV = AppTheme.secondaryButton("Exportar CSV");
        btnCSV.addActionListener(e -> exportarCSV());
        right.add(btnCSV);
        h.add(title, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel content2 = new JPanel(new BorderLayout(0, 10));
        content2.setOpaque(false);

        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);

        cmbTipo = AppTheme.styledCombo(TIPOS_MOVIMIENTO);
        cmbTipo.setPreferredSize(new Dimension(200, 34));
        aplicarEstiloPremiumCombo(cmbTipo);
        cmbTipo.addActionListener(e -> refresh());

        controls.add(AppTheme.label("Tipo:"));
        controls.add(cmbTipo);
        
        LocalDate hoy = LocalDate.now();
        LocalDate haceUnMes = hoy.minusDays(30);

        controls.add(AppTheme.label("Desde:"));
        try {
            javax.swing.text.MaskFormatter mascaraFecha = new javax.swing.text.MaskFormatter("####-##-##");
            mascaraFecha.setPlaceholderCharacter('_');
            txtDesde = new JFormattedTextField(mascaraFecha);
        } catch (java.text.ParseException ex) {
            txtDesde = new JFormattedTextField();
        }
        txtDesde.setText(haceUnMes.format(filterFormatter));
        txtDesde.setPreferredSize(new Dimension(120, 34));
        txtDesde.setBackground(AppTheme.BG_CARD_HOVER);
        txtDesde.setForeground(AppTheme.TEXT_PRIMARY);
        txtDesde.setCaretColor(AppTheme.GOLD_LIGHT);

        // Padding de 8 píxeles a los lados para que el texto respire
        txtDesde.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));

        // Efecto interactivo iluminado al hacer clic
        txtDesde.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                txtDesde.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.GOLD_LIGHT, 1),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                txtDesde.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
            }
        });
        controls.add(txtDesde);

        controls.add(AppTheme.label("Hasta:"));
        try {
            javax.swing.text.MaskFormatter mascaraFecha2 = new javax.swing.text.MaskFormatter("####-##-##");
            mascaraFecha2.setPlaceholderCharacter('_');
            txtHasta = new JFormattedTextField(mascaraFecha2);
        } catch (java.text.ParseException ex) {
            txtHasta = new JFormattedTextField();
        }
        txtHasta.setText(hoy.format(filterFormatter));
        txtHasta.setPreferredSize(new Dimension(120, 34));
        txtHasta.setBackground(AppTheme.BG_CARD_HOVER);
        txtHasta.setForeground(AppTheme.TEXT_PRIMARY);
        txtHasta.setCaretColor(AppTheme.GOLD_LIGHT);

        // Padding de 8 píxeles a los lados
        txtHasta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));

        // Efecto interactivo iluminado al hacer clic
        txtHasta.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                txtHasta.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.GOLD_LIGHT, 1),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                txtHasta.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
            }
        });
        controls.add(txtHasta);

        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        btnFiltrar.addActionListener(e -> refresh());
        controls.add(btnFiltrar);

        JButton btnLimpiar = AppTheme.secondaryButton("Limpiar filtros");
        btnLimpiar.addActionListener(e -> {
            txtDesde.setText(haceUnMes.format(filterFormatter));
            txtHasta.setText(hoy.format(filterFormatter));
            cmbTipo.setSelectedIndex(0);
            refresh();
        });
        controls.add(btnLimpiar);

        bar.add(controls, BorderLayout.CENTER);
        content2.add(bar, BorderLayout.NORTH);

        lblResumen = new JLabel(" ");
        lblResumen.setFont(AppTheme.FONT_SMALL);
        lblResumen.setForeground(AppTheme.TEXT_SECONDARY);
        lblResumen.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        content2.add(lblResumen, BorderLayout.CENTER);

        // tableModel = new DefaultTableModel() {
        //     @Override
        //     public boolean isCellEditable(int r, int c) { return false; }
        // };
        // table = new JTable(tableModel);
        // AppTheme.styleTable(table);

        // content2.add(AppTheme.darkScrollPane(table), BorderLayout.CENTER);

        String[] columnas = { "Fecha/Hora", "Tornillo", "Codigo", "Tipo", "Cantidad", "Stock resultante", "Usuario" };

        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        // Interceptamos la creación de la JTable para no romper el Look & Feel de AppTheme
        table = new JTable(tableModel) {
            @Override
            public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                java.awt.Component c = super.prepareRenderer(renderer, row, column);
                
                // Si la celda es un JLabel (común en JTable), modificamos solo lo necesario
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    
                    // Si son las columnas de Cantidad (4) o Stock Resultante (5)
                    if (column == 4 || column == 5) {
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(Font.BOLD)); // Mantiene la fuente del tema pero en negrita
                        
                        // Aplicamos colores semánticos respetando el estado de selección de la fila
                        if (!isRowSelected(row)) {
                            Object tipoObj = getValueAt(row, 3);
                            String tipoMov = (tipoObj != null) ? tipoObj.toString() : "";
                            
                            if ("Entrada".equals(tipoMov)) {
                                label.setForeground(new Color(46, 204, 113)); // Verde esmeralda
                            } else if ("Salida".equals(tipoMov)) {
                                label.setForeground(new Color(231, 76, 60));  // Rojo coral
                            } else {
                                label.setForeground(AppTheme.GOLD_LIGHT);     // Dorado del sistema
                            }
                        }
                    } else {
                        // Para las demás columnas, aseguramos que mantengan la alineación original a la izquierda
                        label.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                }
                return c;
            }
        };

        // Ahora sí, el estilo del tema se aplicará de forma uniforme a toda la estructura
        AppTheme.styleTable(table);

        JScrollPane scrollPane = AppTheme.darkScrollPane(table);

        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        content2.add(scrollPane, BorderLayout.CENTER);

        return content2;
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        int idx = cmbTipo.getSelectedIndex();
        String tipo = idx == 0 ? null : TIPOS_MOVIMIENTO[idx];
        String rawDesde = txtDesde.getText().trim();
        String rawHasta = txtHasta.getText().trim();
        String desde = (rawDesde.isEmpty() || rawDesde.contains("_")) ? null : rawDesde;
        String hasta = (rawHasta.isEmpty() || rawHasta.contains("_")) ? null : rawHasta;

        if (desde != null && !desde.matches("\\d{4}-\\d{2}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Formato de fecha 'Desde' inválido (Use YYYY-MM-DD)", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (hasta != null && !hasta.matches("\\d{4}-\\d{2}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Formato de fecha 'Hasta' inválido (Use YYYY-MM-DD)", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        lblResumen.setText("Consultando movimientos en la base de datos... Por favor, espere.");
        lblResumen.setForeground(AppTheme.GOLD_LIGHT);

        currentWorker = new SwingWorker<List<MovimientoInventario>, Void>() {
            @Override
            protected List<MovimientoInventario> doInBackground() throws Exception {
                return movimientoDAO.listar(tipo, desde, hasta);
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) return;
                    
                    List<MovimientoInventario> lista = get();
                    actualizarTabla(lista);
                    lblResumen.setForeground(AppTheme.TEXT_SECONDARY);
                } catch (Exception e) {
                    lblResumen.setText("Error al cargar los movimientos.");
                    lblResumen.setForeground(AppTheme.DANGER_TEXT);
                    JOptionPane.showMessageDialog(ReportesPanel.this, 
                        "Error al cargar los movimientos: " + e.getMessage(), 
                        "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        currentWorker.execute();
    }

    private void actualizarTabla(List<MovimientoInventario> lista) {
        // tableModel.setColumnIdentifiers(new String[] {
        //         "Fecha/Hora", "Tornillo", "Codigo", "Tipo", "Cantidad", "Stock resultante", "Usuario" });
        tableModel.setRowCount(0);
        
        for (MovimientoInventario m : lista) {
            String fechaFormateada = "";
            if (m.getFecha() != null) {
                fechaFormateada = m.getFecha().format(visualFormatter);
            }
            
            tableModel.addRow(new Object[] {
                    fechaFormateada,
                    m.getTornilloNombre(), 
                    m.getTornilloCodigo(), 
                    m.getTipoMovimiento(),
                    m.getCantidad(), 
                    m.getStockResultante(), 
                    m.getUsuarioNombre()
            });
        }
        lblResumen.setText(lista.size() + " movimiento(s) en el inventario");
    }

    public void exportarCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Genera un reporte antes de exportar.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("reporte_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
            
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(tableModel.getColumnName(i)).append("\"");
            }
            pw.println(sb);
            
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) sb.append(",");
                    Object v = tableModel.getValueAt(r, c);
                    // Manejo robusto de comillas dobles internas dentro de los nombres de los tornillos
                    String celda = (v != null) ? v.toString().replace("\"", "\"\"") : "";
                    sb.append("\"").append(celda).append("\"");
                }
                pw.println(sb);
            }
            JOptionPane.showMessageDialog(this,
                    "Exportado correctamente:\n" + fc.getSelectedFile().getPath(),
                    "Exportaci\u00f3n exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al exportar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarEstiloPremiumCombo(javax.swing.JComboBox<String> combo) {
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                btn.setForeground(AppTheme.BG_BASE);
                btn.setBackground(AppTheme.BG_CARD_HOVER);
                btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                btn.setContentAreaFilled(true);
                btn.setFocusable(false);
                return btn;
            }
        });

        combo.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));

        combo.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override
            protected JTextField createEditorComponent() {
                JTextField txt = new JTextField();
                txt.setBackground(AppTheme.BG_CARD_HOVER);
                txt.setForeground(AppTheme.TEXT_PRIMARY);
                txt.setEditable(false);
                txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                
                txt.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        if (combo.isEnabled()) {
                            if (combo.isPopupVisible()) {
                                combo.hidePopup();
                            } else {
                                combo.showPopup();
                            }
                        }
                    }
                });
                return txt;
            }
        });
        combo.setEditable(true);

        Object inicial = combo.getSelectedItem();
        combo.getEditor().setItem(inicial != null ? inicial.toString() : "");

        combo.addActionListener(e -> {
            Object item = combo.getSelectedItem();
            combo.getEditor().setItem(item != null ? item.toString() : "");
        });
    }
}