package com.tornillos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Tornillo;
import com.tornillos.service.InventarioService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.ui.dialogs.TornilloDialog;

public class InventarioPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private JComboBox<String> cmbEstado;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    private final MainFrame mainFrame;
    
    // Cambiado: Ahora consume el Servicio en lugar de instanciar directamente el DAO
    private final InventarioService inventarioService = new InventarioService();

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

        cmbEstado = AppTheme.styledCombo(new String[] { "Todos los estados", "Normal", "Stock Bajo", "Crítico", "Sin Stock", "Inactivos" });
        cmbEstado.setPreferredSize(new Dimension(160, 34));

        cmbEstado.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
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

        cmbEstado.setBorder(new javax.swing.border.Border() {
            @Override
            public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BORDER); 
                g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
            @Override public boolean isBorderOpaque() { return false; }
        });

        cmbEstado.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
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
                        if (cmbEstado.isEnabled()) {
                            if (cmbEstado.isPopupVisible()) cmbEstado.hidePopup();
                            else cmbEstado.showPopup();
                        }
                    }
                });
                return txt;
            }
        });
        cmbEstado.setEditable(true);
        
        Object inicial = cmbEstado.getSelectedItem();
        cmbEstado.getEditor().setItem(inicial != null ? inicial.toString() : "");

        cmbEstado.addActionListener(e -> {
            Object item = cmbEstado.getSelectedItem();
            cmbEstado.getEditor().setItem(item != null ? item.toString() : "");
            buscarConFiltro();
        });

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
        String[] cols = { "ID", "Código", "Nombre", "Material", "Diámetro", "Longitud", "Stock", "Mín.",
                "P.Venta", "Estado", "Ubicación", "Activo" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean esActivo = (boolean) getValueAt(row, 11);

                if (!isRowSelected(row)) {
                    if (!esActivo) {
                        c.setBackground(row % 2 == 0 ? AppTheme.BG_CARD : AppTheme.BG_SURFACE);
                        c.setForeground(AppTheme.TEXT_MUTED);
                    } else {
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
        table.getColumnModel().getColumn(11).setMinWidth(0);
        table.getColumnModel().getColumn(11).setMaxWidth(0);

        int[] widths = { 0, 90, 200, 100, 65, 70, 55, 45, 90, 80, 110, 0 };
        for (int i = 1; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        final JPopupMenu menuContextual = buildContextMenu();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            // CORRECCIÓN: Doble clic para editar respetando el orden visual
                if (e.getClickCount() == 2 && SessionManager.getInstance().isGerente()) {
                int viewRow = table.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        abrirDialogoPorId((int) tableModel.getValueAt(modelRow, 0));
                    }
                }
            }

            @Override public void mousePressed(MouseEvent e) { evaluarClicContextual(e); }
            @Override public void mouseReleased(MouseEvent e) { evaluarClicContextual(e); }

            private void evaluarClicContextual(MouseEvent e) {
            if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                int viewRow = table.rowAtPoint(e.getPoint());
                    if (viewRow >= 0 && viewRow < table.getRowCount()) {
                        table.setRowSelectionInterval(viewRow, viewRow);
                        menuContextual.show(table, e.getX(), e.getY());
                    } else {
                        table.clearSelection();
                    }
                }
            }
        });

        // Solución al glitch visual de renderizado al hacer scroll
        JScrollPane scrollPane = AppTheme.darkScrollPane(table);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        
        return scrollPane;
    }

    private JPopupMenu buildContextMenu() {
    JPopupMenu popup = AppTheme.darkPopup();

    JMenuItem verItem = AppTheme.darkMenuItem("Ver stock actual", null);
    verItem.addActionListener(e -> verStock());

        if (SessionManager.getInstance().isGerente()) {
            JMenuItem editItem = AppTheme.darkMenuItem("Editar tornillo", null);
            editItem.addActionListener(e -> {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    abrirDialogoPorId((int) tableModel.getValueAt(modelRow, 0));
                }
            });

            JMenuItem bajaItem = AppTheme.darkMenuItem("Dar de baja tornillo", null);
            bajaItem.setForeground(AppTheme.WARNING_TEXT);
            bajaItem.addActionListener(ev -> darDeBaja());

            JMenuItem altaItem = AppTheme.darkMenuItem("Reactivar tornillo", null);
            altaItem.setForeground(AppTheme.SUCCESS_TEXT);
            altaItem.addActionListener(ev -> reactivarTornillo());

            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    popup.removeAll();
                    int viewRow = table.getSelectedRow();
                    if (viewRow < 0) return;

                    popup.add(verItem);
                    popup.add(AppTheme.darkSeparator());
                    popup.add(editItem);
                    popup.add(AppTheme.darkSeparator());

                    // CORRECCIÓN: Validación de estado usando el mapeo al modelo correcto
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    boolean esActivo = (boolean) tableModel.getValueAt(modelRow, 11);
                    if (esActivo) {
                        popup.add(bajaItem);
                    } else {
                        popup.add(altaItem);
                    }

                    popup.add(AppTheme.darkSeparator());
                    popup.pack();
                }

                @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
                @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
            });
        } else {
            popup.add(verItem);
        }

        return popup;
    }

    private void verStock() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        String nombre = tableModel.getValueAt(modelRow, 2).toString();
        int stock = (int) tableModel.getValueAt(modelRow, 6);
        int minimo = (int) tableModel.getValueAt(modelRow, 7);
        String estado = tableModel.getValueAt(modelRow, 9).toString();

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
        lStock.setForeground("INACTIVO".equals(estado) ? AppTheme.TEXT_MUTED : stock == 0 ? AppTheme.DANGER : stock <= minimo ? AppTheme.WARNING : AppTheme.SUCCESS);
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
                "INACTIVO".equals(estado) ? AppTheme.TEXT_MUTED
                        : "SIN STOCK".equals(estado) ? AppTheme.DANGER
                        : "CRÍTICO".equals(estado) ? AppTheme.WARNING
                        : "BAJO".equals(estado) ? AppTheme.WARNING : AppTheme.SUCCESS);
        panel.add(lEstado);

        JOptionPane.showMessageDialog(this, panel, "Consultar Stock", JOptionPane.PLAIN_MESSAGE);
    }

    private void darDeBaja() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String nombre = tableModel.getValueAt(modelRow, 2).toString();
        
        int opt = JOptionPane.showConfirmDialog(this,
                "¿Dar de baja a '" + nombre + "'?\n" +
                        "El tornillo quedará inactivo pero su historial se conserva.",
                "Dar de baja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            inventarioService.darDeBajaTornillo(id);
            buscarConFiltro(); 
            mainFrame.actualizarBadgeAlertas();
            JOptionPane.showMessageDialog(this, "'" + nombre + "' ha sido desactivado.", "Listo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reactivarTornillo() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String nombre = tableModel.getValueAt(modelRow, 2).toString();
        try {
            inventarioService.reactivarTornillo(id);
            buscarConFiltro();
            mainFrame.actualizarBadgeAlertas();
            JOptionPane.showMessageDialog(this, "'" + nombre + "' ha sido reactivado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        String nombre = tableModel.getValueAt(row, 2).toString();
        int opt = JOptionPane.showConfirmDialog(this,
                "¿ELIMINAR PERMANENTEMENTE '" + nombre + "'?\nEsta acción no se puede deshacer.",
                "Eliminar", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            // Cambiado: Ahora delega al servicio unificado
            inventarioService.eliminarTornilloPermanente(id);
            buscarConFiltro();
            mainFrame.actualizarBadgeAlertas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }*/

    public void abrirDialogo(Tornillo tornillo) {
        TornilloDialog dlg = new TornilloDialog((JFrame) SwingUtilities.getWindowAncestor(this), tornillo);
        dlg.setVisible(true);
        // CORREGIDO: Se añadió el llamado correcto al método () para resolver el error de compilación
        if (dlg.isGuardado()) {
            buscarConFiltro();
            inventarioService.forzarVerificacionAlertas();
            mainFrame.actualizarBadgeAlertas();
        }
    }

    public void abrirModalNuevoTornillo() {
        abrirDialogo(null);
    }

    private void abrirDialogoPorId(int id) {
        try {
            // Cambiado: Ahora se obtiene mediante el servicio unificado
            Tornillo t = inventarioService.obtenerTornilloPorId(id);
            abrirDialogo(t);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarConFiltro() {
        String termino = txtBuscar.getText().trim();
        String[] estadoMap = { null, "NORMAL", "BAJO", "CRÍTICO", "SIN_STOCK", "INACTIVO" };
        
        int indexSeleccionado = cmbEstado.getSelectedIndex();
        if (indexSeleccionado < 0) {
            indexSeleccionado = 0;
        }
        
        String estado = estadoMap[Math.min(indexSeleccionado, estadoMap.length - 1)];

        final String ft = termino;
        final String fe = estado;

        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Tornillo>, Void>() {
            @Override
            protected List<Tornillo> doInBackground() throws Exception {
                // Cambiado: Ahora solicita los datos filtrados a través del servicio
                return inventarioService.buscarTornillos(ft, fe);
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        poblarTabla(get());
                    }
                } catch (Exception e) {
                }
            }
        };
        currentWorker.execute();
    }

    private void poblarTabla(List<Tornillo> lista) {
        tableModel.setRowCount(0);
        for (Tornillo t : lista) {
            String estado;
            if (!t.isActivo()) {
                estado = "INACTIVO";
            } else {
                String est0 = t.getEstadoStock();
                if ("SIN_STOCK".equals(est0))
                    estado = "SIN STOCK";
                else if ("CRÍTICO".equals(est0))
                    estado = "CRÍTICO";
                else if ("BAJO".equals(est0))
                    estado = "BAJO";
                else
                    estado = "NORMAL";
            }

            tableModel.addRow(new Object[] {
                    t.getId(), t.getCodigo(), t.getNombre(),
                    t.getMaterial(),
                    formatearMedida(t.getDiametroMm(), t.getSistemaMedida()),
                    formatearMedida(t.getLongitudMm(), t.getSistemaMedida()),
                    t.getStockActual(), t.getStockMinimo(),
                    t.getPrecioVenta(), estado, t.getUbicacion(),
                    t.isActivo()
            });
        }
        lblConteo.setText(lista.size() + " producto(s) encontrado(s)");
    }

    public void refresh() {
        buscarConFiltro();
    }

    // ── Motor de Conversión y Formateo de Medidas Comerciales ────────────────

    private String formatearMedida(java.math.BigDecimal valor, String sistemaMedida) {
        if (valor == null) return "";
        
        if ("IMPERIAL".equalsIgnoreCase(sistemaMedida)) {
            // 🌟 1. El valor viene de la base de datos en milímetros, lo convertimos a pulgadas
            java.math.BigDecimal mmAInches = new java.math.BigDecimal("25.4");
            java.math.BigDecimal valorPulgadas = valor.divide(mmAInches, java.math.MathContext.DECIMAL64);
            
            // 🌟 2. Lo traducimos a fracción dinámica y le concatenamos las comillas de pulgadas (")
            return convertirDecimalAFraccion(valorPulgadas) + "\"";
        }
        
        // Formato métrico estándar
        return valor.stripTrailingZeros().toPlainString() + " mm";
    }

    /**
     * Toma un valor decimal en pulgadas y descubre de forma dinámica su fracción comercial 
     * más cercana (simplificando automáticamente a denominadores de 2, 4, 8, 16, 32 o 64).
     */
    private String convertirDecimalAFraccion(java.math.BigDecimal valor) {
        if (valor == null) return "";

        // Separar la parte entera del residuo decimal
        int entero = valor.intValue();
        java.math.BigDecimal residuo = valor.subtract(new java.math.BigDecimal(entero));

        // Si es un entero puro (ej: 2 pulgadas -> "2")
        if (residuo.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return String.valueOf(entero);
        }

        // Denominadores comerciales del ramo ferretero
        int[] denominadoresComerciales = {2, 4, 8, 16, 32, 64};
        double valorDecimal = residuo.doubleValue();

        int mejorNumerador = 0;
        int mejorDenominador = 1;
        double menorError = 1.0;

        // Buscar la combinación con menor margen de error con tolerancia estricta
        for (int d : denominadoresComerciales) {
            long n = Math.round(valorDecimal * d);
            double error = Math.abs(valorDecimal - ((double) n / d));
            
            if (error < menorError && error < 0.005) { 
                menorError = error;
                mejorNumerador = (int) n;
                mejorDenominador = d;
            }
        }

        // Si el número no corresponde a una fracción estándar, devolvemos el decimal plano limpio
        if (mejorNumerador == 0) {
            return valor.stripTrailingZeros().toPlainString();
        }

        // Simplificar la fracción mediante el Máximo Común Divisor (MCD)
        int mcd = calcularMCD(mejorNumerador, mejorDenominador);
        mejorNumerador /= mcd;
        mejorDenominador /= mcd;

        // Construir la cadena legible (soporta números mixtos como "1 1/2")
        StringBuilder resultado = new StringBuilder();
        if (entero > 0) {
            resultado.append(entero).append(" ");
        }
        resultado.append(mejorNumerador).append("/").append(mejorDenominador);

        return resultado.toString();
    }

    /** Algoritmo de Euclides para simplificar fracciones */
    private int calcularMCD(int a, int b) {
        return b == 0 ? a : calcularMCD(b, a % b);
    }
}