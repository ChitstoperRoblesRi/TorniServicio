package com.tornillos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Tornillo;
import com.tornillos.service.AlertaService;
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

        // INTEGRADO: Añadido "Inactivos" como quinto estado disponible
        cmbEstado = AppTheme.styledCombo(new String[] { "Todos los estados", "Normal", "Stock Bajo", "Crítico", "Sin Stock", "Inactivos" });
        cmbEstado.setPreferredSize(new Dimension(160, 34));

        // ── SOLUCIÓN DE RAÍZ: Reemplazar la UI nativa de Windows por una limpia, agnóstica y oscura ──
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

        // ── ENMARCADO Y REDONDEO PREMIUM CON EL COLOR OFICIAL DEL DESIGN SYSTEM ──
        cmbEstado.setBorder(new javax.swing.border.Border() {
            @Override
            public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                // Activamos el suavizado de bordes (Antialiasing) para evitar pixeles duros
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                
                // ¡AQUÍ ESTÁ TU COLOR EXACTO! Usamos el mismo borde que styledField
                g2.setColor(AppTheme.BORDER); 
                
                // Dibujamos el rectángulo redondeado (radio de 8px para suavizar las esquinas rectas)
                g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                g2.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c) {
                // Margen interno sutil para proteger el texto del redondeo
                return new Insets(2, 2, 2, 2);
            }

            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        });

        // ── PARCHE LOOK & FEEL PARA EL EDITOR TEXTUAL INTERNO ──
        cmbEstado.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override
            protected JTextField createEditorComponent() {
                JTextField txt = new JTextField();
                txt.setBackground(AppTheme.BG_CARD_HOVER); // Tu fondo oscuro premium
                txt.setForeground(AppTheme.TEXT_PRIMARY);    // Tu texto claro
                txt.setEditable(false);                     // Bloquea por completo la escritura libre
                txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                
                // Abre el menú desplegable al hacer clic en cualquier parte de la caja de texto
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
        
        // Sincroniza el texto inicial del contenedor con el editor oscuro
        Object inicial = cmbEstado.getSelectedItem();
        cmbEstado.getEditor().setItem(inicial != null ? inicial.toString() : "");

        // Listener corregido: actualiza el texto visual del editor y dispara el filtro
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
        // AGREGADO: Columna oculta en índice 11 para trackear la propiedad "activo" nativa
        String[] cols = { "ID", "Código", "Nombre", "Material", "Diámetro", "Longitud", "Stock", "Mín.",
                "P.Venta", "Estado", "Ubicación", "Activo" };
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

        // Construcción e inicialización del JPopupMenu con su lógica interna
        final JPopupMenu menuContextual = buildContextMenu();

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
                evaluarClicContextual(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                evaluarClicContextual(e);
            }

            private void evaluarClicContextual(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    
                    // CORRECCIÓN DE SEGURIDAD VISUAL: Solo abre el menú si golpea una fila válida
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        menuContextual.show(table, e.getX(), e.getY());
                    } else {
                        table.clearSelection();
                    }
                }
            }
        });

        return AppTheme.darkScrollPane(table);
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu popup = AppTheme.darkPopup();

        JMenuItem verItem = AppTheme.darkMenuItem("Ver stock actual", null);
        verItem.addActionListener(e -> verStock());

        if (SessionManager.getInstance().isGerente()) {
            JMenuItem editItem = AppTheme.darkMenuItem("Editar tornillo", null);
            editItem.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0)
                    abrirDialogoPorId((int) tableModel.getValueAt(row, 0));
            });

            JMenuItem bajaItem = AppTheme.darkMenuItem("Dar de baja tornillo", null);
            bajaItem.setForeground(AppTheme.WARNING_TEXT);
            bajaItem.addActionListener(ev -> darDeBaja());

            JMenuItem altaItem = AppTheme.darkMenuItem("Reactivar tornillo", null);
            altaItem.setForeground(AppTheme.SUCCESS_TEXT);
            altaItem.addActionListener(ev -> reactivarTornillo());

            JMenuItem eliminarItem = AppTheme.darkMenuItem("Eliminar permanentemente", null);
            eliminarItem.setForeground(AppTheme.DANGER_TEXT);
            eliminarItem.addActionListener(ev -> eliminarSeleccionado());

            // CORRECCIÓN ASÍNCRONA: PopupMenuListener nativo para evitar retrasos y menús vacíos
            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    popup.removeAll();
                    int row = table.getSelectedRow();
                    if (row < 0) return;

                    popup.add(verItem);
                    popup.add(AppTheme.darkSeparator());
                    popup.add(editItem);
                    popup.add(AppTheme.darkSeparator());

                    boolean esActivo = (boolean) tableModel.getValueAt(row, 11);
                    if (esActivo) {
                        popup.add(bajaItem);
                    } else {
                        popup.add(altaItem);
                    }

                    popup.add(AppTheme.darkSeparator());
                    popup.add(eliminarItem);
                    
                    popup.pack(); // Fuerza el cálculo instantáneo de dimensiones
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
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        int id = (int) tableModel.getValueAt(row, 0);
        String nombre = tableModel.getValueAt(row, 2).toString();
        int opt = JOptionPane.showConfirmDialog(this,
                "Dar de baja a '" + nombre + "'?\n" +
                        "El tornillo quedará inactivo pero su historial se conserva.",
                "Dar de baja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION)
            return;
        try {
            tornilloDAO.darDeBaja(id);
            buscarConFiltro(); 
            JOptionPane.showMessageDialog(this,
                    "'" + nombre + "' ha sido desactivado.", "Listo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reactivarTornillo() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        int id = (int) tableModel.getValueAt(row, 0);
        String nombre = tableModel.getValueAt(row, 2).toString();
        try {
            tornilloDAO.reactivar(id);
            buscarConFiltro();
            JOptionPane.showMessageDialog(this,
                    "'" + nombre + "' ha sido reactivado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
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
            buscarConFiltro();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void abrirDialogo(Tornillo tornillo) {
        TornilloDialog dlg = new TornilloDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), tornillo);
        dlg.setVisible(true);
        if (dlg.isGuardado()) {
            buscarConFiltro();
            alertaService.verificarAlertas();
            mainFrame.actualizarBadgeAlertas();
        }
    }

    public void abrirModalNuevoTornillo() {
        abrirDialogo(null);
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
        String[] estadoMap = { null, "NORMAL", "BAJO", "CRÍTICO", "SIN_STOCK", "INACTIVO" };
        
        // BLINDAJE: Si el renderizado editable devuelve -1, lo forzamos a 0 ("Todos los estados")
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
                    // Pasamos el valor y el sistema de medida por el formateador dinámico
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

    /**
     * Convierte un BigDecimal numérico a una representación visual amigable 
    * dependiendo de si el sistema de medida es MÉTRICO o IMPERIAL.
    */
    private String formatearMedida(java.math.BigDecimal valor, String sistemaMedida) {
        if (valor == null) return "";
        
        if ("IMPERIAL".equalsIgnoreCase(sistemaMedida)) {
            double val = valor.doubleValue();
            int entero = (int) val;
            double decimal = val - entero;

            String fraccion = "";
            // Mapeo preciso de los decimales flotantes a fracciones comunes en ferreterías
            if (Math.abs(decimal - 0.0625) < 0.001) fraccion = "1/16";
            else if (Math.abs(decimal - 0.125) < 0.001)  fraccion = "1/8";
            else if (Math.abs(decimal - 0.1875) < 0.001) fraccion = "3/16";
            else if (Math.abs(decimal - 0.25) < 0.001)   fraccion = "1/4";
            else if (Math.abs(decimal - 0.3125) < 0.001) fraccion = "5/16";
            else if (Math.abs(decimal - 0.375) < 0.001)  fraccion = "3/8";
            else if (Math.abs(decimal - 0.5) < 0.001)    fraccion = "1/2";
            else if (Math.abs(decimal - 0.625) < 0.001)  fraccion = "5/8";
            else if (Math.abs(decimal - 0.75) < 0.001)   fraccion = "3/4";
            else if (Math.abs(decimal - 0.875) < 0.001)  fraccion = "7/8";

            // Construir la cadena final (Fracción mixta o simple)
            if (entero > 0) {
                return fraccion.isEmpty() ? valor.toPlainString() + "\"" : entero + " " + fraccion + "\"";
            } else {
                return fraccion.isEmpty() ? valor.toPlainString() + "\"" : fraccion + "\"";
            }
        }
        
        // Si es MÉTRICO, simplemente adjuntamos la unidad "mm" de forma limpia
        return valor.stripTrailingZeros().toPlainString() + " mm";
    }
}