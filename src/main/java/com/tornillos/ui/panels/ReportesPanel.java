package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.model.MovimientoInventario;
import com.tornillos.service.ReportesService;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportesPanel extends JPanel {

    private JTextField txtDesde, txtHasta;
    private JComboBox<String> cmbTipo;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblResumen;
    private SwingWorker<?, ?> currentWorker;

    private final ReportesService reportesService = new ReportesService();

    private static final String[] TIPOS_MOVIMIENTO = {
            "Todos los movimientos", "Creaci\u00f3n", "Entrada", "Salida"
    };

    public ReportesPanel() {
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        buildUI();
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
        cmbTipo.addActionListener(e -> refresh());

        controls.add(AppTheme.label("Tipo:"));
        controls.add(cmbTipo);
        controls.add(AppTheme.label("Desde:"));
        txtDesde = AppTheme.styledField("YYYY-MM-DD");
        txtDesde.setPreferredSize(new Dimension(120, 34));
        txtDesde.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    refresh();
            }
        });
        controls.add(txtDesde);
        controls.add(AppTheme.label("Hasta:"));
        txtHasta = AppTheme.styledField("YYYY-MM-DD");
        txtHasta.setPreferredSize(new Dimension(120, 34));
        txtHasta.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    refresh();
            }
        });
        controls.add(txtHasta);
        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        btnFiltrar.addActionListener(e -> refresh());
        controls.add(btnFiltrar);

        JButton btnLimpiar = AppTheme.secondaryButton("Limpiar filtros");
        btnLimpiar.addActionListener(e -> {
            txtDesde.setText("");
            txtHasta.setText("");
            cmbTipo.setSelectedIndex(0);
        });
        controls.add(btnLimpiar);

        bar.add(controls, BorderLayout.CENTER);
        content2.add(bar, BorderLayout.NORTH);

        lblResumen = new JLabel(" ");
        lblResumen.setFont(AppTheme.FONT_SMALL);
        lblResumen.setForeground(AppTheme.TEXT_SECONDARY);
        lblResumen.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        content2.add(lblResumen, BorderLayout.CENTER);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        content2.add(AppTheme.darkScrollPane(table), BorderLayout.CENTER);

        return content2;
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);

        int idx = cmbTipo.getSelectedIndex();
        String tipo = idx == 0 ? null : TIPOS_MOVIMIENTO[idx];
        String desde = txtDesde.getText().trim().isEmpty() ? null : txtDesde.getText().trim();
        String hasta = txtHasta.getText().trim().isEmpty() ? null : txtHasta.getText().trim();

        currentWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                generarMovimientos(tipo, desde, hasta);
                return null;
            }
        };
        currentWorker.execute();
    }

    private void generarMovimientos(String tipo, String desde, String hasta) throws Exception {
        List<MovimientoInventario> lista = reportesService.listarMovimientos(tipo, desde, hasta);
        SwingUtilities.invokeLater(() -> {
            tableModel.setColumnIdentifiers(new String[] {
                    "Fecha/Hora", "Tornillo", "Codigo", "Tipo", "Cantidad", "Stock resultante", "Usuario" });
            tableModel.setRowCount(0);
            for (MovimientoInventario movimiento : lista) {
                tableModel.addRow(new Object[] {
                        movimiento.getFecha() != null ? movimiento.getFecha().toString().substring(0, 16) : "",
                        movimiento.getTornilloNombre(), movimiento.getTornilloCodigo(), movimiento.getTipoMovimiento(),
                        movimiento.getCantidad(), movimiento.getStockResultante(), movimiento.getUsuarioNombre()
                });
            }
            lblResumen.setText(lista.size() + " movimiento(s) en el inventario");
        });
    }

    private void exportarCSV() {
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
                if (i > 0)
                    sb.append(",");
                sb.append("\"").append(tableModel.getColumnName(i)).append("\"");
            }
            pw.println(sb);
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0)
                        sb.append(",");
                    Object v = tableModel.getValueAt(r, c);
                    sb.append("\"").append(v != null ? v.toString().replace("\"", "'") : "").append("\"");
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
}
