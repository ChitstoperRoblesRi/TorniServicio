package com.tornillos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Salida;
import com.tornillos.model.Tornillo;
import com.tornillos.service.SalidaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.util.FolioGenerator;

public class SalidasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private javax.swing.JFormattedTextField txtDesde, txtHasta;
    private final java.time.format.DateTimeFormatter filterFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final java.time.format.DateTimeFormatter visualFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    // Cambiado: Ahora la interfaz depende estrictamente de su capa de servicio dedicada
    private final SalidaService salidaService = new SalidaService();

    private final String[] MOTIVOS = {"Venta","Uso Interno","Muestra","Devolucion","Merma"};

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
        JLabel title = new JLabel("Módulo de Salidas");
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

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        txtBuscar = AppTheme.styledField("Buscar por folio, tornillo, cliente...");
        txtBuscar.setPreferredSize(new Dimension(260, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { buscar(); }
        });
        
        // --- NUEVA CONFIGURACIÓN DE FECHAS DE REPORTE (ÚLTIMOS 30 DÍAS) ---
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate haceUnMes = hoy.minusDays(30);

        try {
            javax.swing.text.MaskFormatter mascaraFecha = new javax.swing.text.MaskFormatter("####-##-##");
            mascaraFecha.setPlaceholderCharacter('_');
            txtDesde = new javax.swing.JFormattedTextField(mascaraFecha);
        } catch (java.text.ParseException ex) {
            txtDesde = new javax.swing.JFormattedTextField();
        }
        txtDesde.setText(haceUnMes.format(filterFormatter));
        txtDesde.setPreferredSize(new Dimension(148, 34)); // Respeta los 148px originales de salidas
        txtDesde.setBackground(AppTheme.BG_CARD_HOVER);
        txtDesde.setForeground(AppTheme.TEXT_PRIMARY);
        txtDesde.setCaretColor(AppTheme.GOLD_LIGHT);
        txtDesde.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));
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
        txtDesde.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });

        try {
            javax.swing.text.MaskFormatter mascaraFecha2 = new javax.swing.text.MaskFormatter("####-##-##");
            mascaraFecha2.setPlaceholderCharacter('_');
            txtHasta = new javax.swing.JFormattedTextField(mascaraFecha2);
        } catch (java.text.ParseException ex) {
            txtHasta = new javax.swing.JFormattedTextField();
        }
        txtHasta.setText(hoy.format(filterFormatter));
        txtHasta.setPreferredSize(new Dimension(148, 34)); // Respeta los 148px originales de salidas
        txtHasta.setBackground(AppTheme.BG_CARD_HOVER);
        txtHasta.setForeground(AppTheme.TEXT_PRIMARY);
        txtHasta.setCaretColor(AppTheme.GOLD_LIGHT);
        txtHasta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));
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
        txtHasta.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });
        // ------------------------------------------------------------------

        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        JButton btnLimpiar = AppTheme.secondaryButton("X Limpiar");
        btnFiltrar.addActionListener(e -> refresh());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            java.time.LocalDate dHoy = java.time.LocalDate.now();
            txtDesde.setText(dHoy.minusDays(30).format(filterFormatter));
            txtHasta.setText(dHoy.format(filterFormatter));
            refresh();
        });
        bar.add(txtBuscar);
        bar.add(AppTheme.label("Desde:")); bar.add(txtDesde);
        bar.add(AppTheme.label("Hasta:")); bar.add(txtHasta);
        bar.add(btnFiltrar); bar.add(btnLimpiar);
        p.add(bar, BorderLayout.NORTH);

        String[] cols = {"ID","Folio","Tornillo","Código","Motivo","Cliente","Cantidad","P.Unitario","Total","Usuario","Fecha"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
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

                // REEMPLAZAR ESTA PARTE EN EL prepareRenderer DE SalidasPanel.java
                // REEMPLAZAR ESTA PARTE DENTRO DEL prepareRenderer (Líneas ~195-205):
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    
                    if (col == 6) {
                        // Columna 6 es "Cantidad": Centrada y en negrita
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else if (col == 7 || col == 8) {
                        // 🌟 CORREGIDO: Columnas 7 y 8 (P.Unitario y Total) ahora centradas
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                    } else {
                        // El resto de las columnas a la izquierda
                        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    }
                }
                return c;
            }
        };
        AppTheme.styleTable(table);

        // 🌟 NUEVO: Centrar los títulos de los encabezados de Cantidad, P.Unitario y Total
        for (int col : new int[]{6, 7, 8}) {
            table.getColumnModel().getColumn(col).setHeaderRenderer((t, val, sel, focus, r, c) -> {
                Component comp = t.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                }
                return comp;
            });
        }

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        final JPopupMenu menuContextual = buildContextMenu();

        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { evaluarClicContextual(e); }
            @Override public void mouseReleased(MouseEvent e) { evaluarClicContextual(e); }

            private void evaluarClicContextual(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
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
        
        p.add(scrollPane, BorderLayout.CENTER);
        return p;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEliminar = AppTheme.darkMenuItem("Eliminar salida (revierte stock)", null);
        itemEliminar.addActionListener(e -> eliminarSeleccionada());
        if (SessionManager.getInstance().isGerente()) {
            popup.add(itemEliminar);
        }
        return popup;
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
            "Eliminar salida " + folio + "?\nEsto revertirá el stock del tornillo.",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                // Cambiado: Invocación canalizada al servicio para procesar transacciones y reajustar las alertas
                salidaService.eliminarSalida((int) tableModel.getValueAt(row, 0));
                mainFrame.actualizarBadgeAlertas();
                refresh();
                JOptionPane.showMessageDialog(this, "Salida eliminada y stock revertido.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void abrirFormularioSalida() {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Registrar Salida", true);
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
        try { 
            // Cambiado: Ahora se le solicita de manera limpia el catálogo disponible a la capa intermedia
            tornillos = salidaService.obtenerTornillosConStock(); 
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Error cargando existencias: " + e.getMessage()); 
            return; 
        }

        JComboBox<Tornillo> cmbTornillo = com.tornillos.util.SearchableComboBoxFactory.create(tornillos);
        cmbTornillo.setBackground(AppTheme.BG_CARD_HOVER);
        cmbTornillo.setForeground(AppTheme.TEXT_PRIMARY);
        JComboBox<String> cmbMotivo = AppTheme.styledCombo(MOTIVOS);
        
        cmbMotivo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
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

        cmbMotivo.setBorder(new javax.swing.border.Border() {
            @Override public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
            @Override public boolean isBorderOpaque() { return false; }
        });

        cmbMotivo.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override protected JTextField createEditorComponent() {
                JTextField txt = new JTextField();
                txt.setBackground(AppTheme.BG_CARD_HOVER);
                txt.setForeground(AppTheme.TEXT_PRIMARY);
                txt.setEditable(false);
                txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                txt.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mousePressed(java.awt.event.MouseEvent e) {
                        if (cmbMotivo.isEnabled()) {
                            if (cmbMotivo.isPopupVisible()) cmbMotivo.hidePopup();
                            else cmbMotivo.showPopup();
                        }
                    }
                });
                return txt;
            }
        });
        cmbMotivo.setEditable(true);
        
        Object inicialMotivo = cmbMotivo.getSelectedItem();
        cmbMotivo.getEditor().setItem(inicialMotivo != null ? inicialMotivo.toString() : "");
        cmbMotivo.addActionListener(ev -> {
            Object item = cmbMotivo.getSelectedItem();
            cmbMotivo.getEditor().setItem(item != null ? item.toString() : "");
        });

        JTextField txtCantidad = AppTheme.styledField("0");
        JTextField txtPrecio   = AppTheme.styledField("0.00");
        JTextField txtCliente  = AppTheme.styledField("Nombre del cliente (opcional)");
        JTextArea txtObs       = AppTheme.styledTextArea(); txtObs.setRows(2);

        JLabel lblStock = new JLabel("Stock disponible: --");
        lblStock.setFont(AppTheme.FONT_SMALL);
        lblStock.setForeground(AppTheme.SUCCESS_TEXT);
        cmbTornillo.addActionListener(e -> {
            // 🌟 CLAVE: Obtenemos la selección como Object genérico primero
            Object seleccionadoObj = cmbTornillo.getSelectedItem();
            
            // Solo procesamos si el usuario seleccionó un Tornillo real del catálogo
            if (seleccionadoObj instanceof Tornillo) {
                Tornillo t = (Tornillo) seleccionadoObj;
                int stock = t.getStockActual();
                lblStock.setText("Stock disponible: " + stock + " " + t.getUnidadMedida());
                lblStock.setForeground(stock <= t.getStockMinimo() ? AppTheme.WARNING : AppTheme.SUCCESS_TEXT);
                if (t.getPrecioVenta() != null) txtPrecio.setText(t.getPrecioVenta().toString());
            } else {
                // Si es texto libre temporal del buscador, limpiamos la etiqueta de stock preventivamente
                lblStock.setText("Stock disponible: --");
                lblStock.setForeground(AppTheme.SUCCESS_TEXT);
            }
        });
        cmbTornillo.setSelectedIndex(-1);

        cmbTornillo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
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
        cmbTornillo.setBorder(new javax.swing.border.Border() {
            @Override public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
            @Override public boolean isBorderOpaque() { return false; }
        });
        unificarEstiloEditorCombo(cmbTornillo, true);

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
                Tornillo t = (Tornillo) cmbTornillo.getSelectedItem();
                if (t == null) throw new IllegalArgumentException("Debe seleccionar un tornillo de la lista.");
                int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
                if (cantidad <= 0) throw new IllegalArgumentException("Cantidad debe ser mayor a 0");
                if (precio.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("El precio unitario debe ser un número positivo mayor a 0.");

                String motivo = cmbMotivo.getSelectedItem() != null ? cmbMotivo.getSelectedItem().toString() : "";
                boolean motivoValido = java.util.Arrays.asList(MOTIVOS).contains(motivo);
                if (!motivoValido) throw new IllegalArgumentException("Seleccione un motivo válido de la lista.");

                BigDecimal totalCalculado = precio.multiply(BigDecimal.valueOf(cantidad));
                String resumenTicket = String.format(
                    "¿Confirmar el registro de esta Salida / Venta?\n\n" +
                    "▪ Producto:  %s\n" +
                    "▪ Motivo:    %s\n" +
                    "▪ Cantidad:  %d unidades\n" +
                    "▪ P. Unitario: $%,.2f\n" +
                    "▪ Total Operación: $%,.2f\n\n" +
                    "Esta acción descontará las existencias de forma inmediata.",
                    t.getNombre(), motivo, cantidad, precio, totalCalculado
                );
                int confirmacion = JOptionPane.showConfirmDialog(
                    dlg, resumenTicket, "Confirmar Salida de Almacén",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
                );
                if (confirmacion != JOptionPane.YES_OPTION) return;

                Salida s = new Salida();
                s.setFolio(folio);
                s.setTornilloId(t.getId());
                s.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
                s.setCantidad(cantidad);
                s.setPrecioUnitario(precio);
                s.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)));
                s.setMotivo(motivo);
                s.setCliente(txtCliente.getText().trim());
                s.setObservaciones(txtObs.getText().trim());

                // Cambiado: Registro y disparo de alertas procesados de forma segura e íntegra en el Service
                salidaService.registrarSalida(s);
                
                dlg.dispose();
                refresh();
                mainFrame.actualizarBadgeAlertas();

                JOptionPane.showMessageDialog(this,
                    "Salida registrada. Folio: " + folio, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Datos numéricos inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
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
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.0;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        p.add(field, gbc);
    }

    /**
     * Modifica los componentes visuales del editor interno del JComboBox para adaptarlo
     * al tema oscuro sin alterar ni destruir los listeners funcionales del componente.
     */
    private void unificarEstiloEditorCombo(JComboBox<?> combo, boolean esBuscable) {
        // 🌟 CLAVE: Obtenemos el componente editor actual en lugar de destruirlo con setEditor()
        Component editorComp = combo.getEditor().getEditorComponent();
        
        if (editorComp instanceof JTextField) {
            JTextField txt = (JTextField) editorComp;
            
            // Aplicamos los colores del tema oscuro premium
            txt.setBackground(AppTheme.BG_CARD_HOVER);
            txt.setForeground(AppTheme.TEXT_PRIMARY);
            txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6)); // Padding interno
            
            // Si es buscable (como los tornillos), permitimos la edición. 
            // Si no (como los motivos), se bloquea para que actúe como dropdown cerrado.
            txt.setEditable(esBuscable);
            
            // El MouseListener de despliegue automático solo se añade si NO es buscable.
            // Si es buscable, el usuario necesita hacer clic para posicionar el cursor y escribir.
            if (!esBuscable) {
                txt.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        if (combo.isEnabled()) {
                            if (combo.isPopupVisible()) combo.hidePopup();
                            else combo.showPopup();
                        }
                    }
                });
            }
        }
        
        // Forzamos a que el combo reconozca que debe pintar su editor interno
        combo.setEditable(true);
    }

    private void poblarTabla(List<Salida> lista) {
        tableModel.setRowCount(0);
        for (Salida s : lista) {
            // Aplicación del formateador bidireccional humano
            String fechaFormateada = "";
            if (s.getFecha() != null) {
                fechaFormateada = s.getFecha().format(visualFormatter);
            }

            tableModel.addRow(new Object[]{
                s.getId(), s.getFolio(),
                s.getTornilloNombre(), s.getTornilloCodigo(),
                s.getMotivo(), s.getCliente(),
                s.getCantidad(), s.getPrecioUnitario(), s.getTotal(),
                s.getUsuarioNombre(),
                fechaFormateada // Inyección del string formateado limpiamente
            });
        }
        lblConteo.setText(lista.size() + " salida(s)");
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Salida>, Void>() {
            @Override 
            protected List<Salida> doInBackground() throws Exception {
                String termino = txtBuscar.getText().trim();
                String rawDesde = txtDesde.getText().trim();
                String rawHasta = txtHasta.getText().trim();
                
                String desde = (rawDesde.isEmpty() || rawDesde.contains("_")) ? null : rawDesde;
                String hasta = (rawHasta.isEmpty() || rawHasta.contains("_")) ? null : rawHasta;
                
                return salidaService.buscarSalidas(termino, desde, hasta);
            }
            @Override protected void done() {
                try { 
                    if (!isCancelled()) {
                        poblarTabla(get()); 
                    }
                } catch (Exception ex) { /* ignored */ }
            }
        };
        currentWorker.execute();
    }
}