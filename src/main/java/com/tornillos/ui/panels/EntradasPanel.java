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
import com.tornillos.model.Entrada;
import com.tornillos.model.Tornillo;
import com.tornillos.service.EntradaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.util.FolioGenerator;

public class EntradasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private javax.swing.JFormattedTextField txtDesde, txtHasta;
    private final java.time.format.DateTimeFormatter filterFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final java.time.format.DateTimeFormatter visualFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    // Cambiado: El panel ahora depende estrictamente de su Capa de Servicio dedicada
    private final EntradaService entradaService = new EntradaService();

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
        // Modifica la línea ~66 donde se configura el botón "+ Nueva Entrada" para pasarle un null:
        JButton btnNueva = AppTheme.successButton("+ Nueva Entrada");
        btnNueva.addActionListener(e -> abrirFormularioEntrada(null)); // 🌟 CAMBIADO: pasa null si es entrada limpia
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
        txtDesde.setPreferredSize(new Dimension(150, 34));
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
        txtHasta.setPreferredSize(new Dimension(150, 34));
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

                // REEMPLAZAR ESTA PARTE DENTRO DEL prepareRenderer (Líneas ~184-194):
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    
                    if (col == 4) {
                        // Columna 4 es "Cantidad": Centrada y en negrita
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else if (col == 5 || col == 6) {
                        // 🌟 CORREGIDO: Columnas 5 y 6 (P.Unitario y Total) ahora centradas
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                    } else {
                        // El resto de las columnas (Folio, Tornillo, Código, Usuario, Fecha) a la izquierda
                        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    }
                }
                return c;
            }
        };
        AppTheme.styleTable(table);

        // 🌟 NUEVO: Centrar los títulos de los encabezados de Cantidad, P.Unitario y Total
        for (int col : new int[]{4, 5, 6}) {
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
        JMenuItem itemEliminar = AppTheme.darkMenuItem("Eliminar entrada (revierte stock)", null);
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
        if (!SessionManager.getInstance().isGerente())
            return;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una entrada.");
            return;
        }
        
        // CORRECCIÓN: Conversión del índice antes de extraer datos transaccionales del almacén
        int modelRow = table.convertRowIndexToModel(viewRow);
        String folio = tableModel.getValueAt(modelRow, 1).toString();
        int idEntrada = (int) tableModel.getValueAt(modelRow, 0);

        int opt = JOptionPane.showConfirmDialog(this,
                "¿Eliminar entrada " + folio + "?\nEsto revertira el stock del tornillo.",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                entradaService.eliminarEntrada(idEntrada);
                mainFrame.actualizarBadgeAlertas();
                refresh();
                JOptionPane.showMessageDialog(this, "Entrada eliminada y stock revertido.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 🌟 CORREGIDO: Firma ordenada para solucionar el bug de preselección de precio y control de sobrestock
    public void abrirFormularioEntrada(String codigoPreseleccionado) { 
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Registrar Entrada", true);
        dlg.setSize(520, 510); // 💡 Incrementado ligeramente el alto para acomodar el indicador de stock
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
            tornillos = entradaService.obtenerCatalogoTornillos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando datos: " + e.getMessage());
            return;
        }

        // 1. PASO CLAVE: Inicializar TODOS los campos del formulario primero
        JComboBox<Tornillo> cmbTornillo = com.tornillos.util.SearchableComboBoxFactory.create(tornillos);
        cmbTornillo.setSelectedIndex(-1);
        cmbTornillo.setBackground(AppTheme.BG_CARD_HOVER);
        cmbTornillo.setForeground(AppTheme.TEXT_PRIMARY);

        JTextField txtCantidad = AppTheme.styledField("0");
        JTextField txtPrecio = AppTheme.styledField("0.00");
        JTextField txtFactura = AppTheme.styledField("Numero de factura (opcional)");
        JTextArea txtObs = AppTheme.styledTextArea();
        txtObs.setRows(2);

        // 🌟 NUEVO: Indicador visual de retroalimentación de stock
        JLabel lblInfoStock = new JLabel(" ");
        lblInfoStock.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblInfoStock.setForeground(AppTheme.TEXT_MUTED);

        // 2. PASO CLAVE: Agregar el ActionListener ANTES de hacer la selección automática
        cmbTornillo.addActionListener(e -> {
            Object seleccionadoObj = cmbTornillo.getSelectedItem();
            if (seleccionadoObj instanceof Tornillo) {
                Tornillo seleccionado = (Tornillo) seleccionadoObj;
                if (seleccionado.getPrecioCosto() != null) {
                    txtPrecio.setText(seleccionado.getPrecioCosto().toPlainString());
                } else {
                    txtPrecio.setText("0.00");
                }
                // Actualizar la etiqueta informativa al cambiar de tornillo
                actualizarInformacionStock(lblInfoStock, seleccionado, txtCantidad.getText());
            } else {
                txtPrecio.setText("0.00");
                lblInfoStock.setText(" ");
            }
        });

        // 🌟 NUEVO: DocumentListener para validar y dar retroalimentación EN TIEMPO REAL mientras escriben
        txtCantidad.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void evaluar() {
                Object sel = cmbTornillo.getSelectedItem();
                if (sel instanceof Tornillo) {
                    actualizarInformacionStock(lblInfoStock, (Tornillo) sel, txtCantidad.getText());
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { evaluar(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { evaluar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { evaluar(); }
        });

        // 3. Aplicar estilos UI al ComboBox
        cmbTornillo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
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
        cmbTornillo.setBorder(new javax.swing.border.Border() {
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
        unificarEstiloEditorCombo(cmbTornillo, true);

        // 4. PASO CLAVE: Ejecutar la selección automática AL FINAL. 
        if (codigoPreseleccionado != null) {
            for (int i = 0; i < cmbTornillo.getItemCount(); i++) {
                Tornillo t = cmbTornillo.getItemAt(i);
                if (t != null && codigoPreseleccionado.equals(t.getCodigo())) {
                    cmbTornillo.setSelectedItem(t);
                    break;
                }
            }
        }

        String folio = FolioGenerator.generarEntrada();
        JTextField txtFolio = AppTheme.styledField(folio);
        txtFolio.setText(folio);
        txtFolio.setEditable(false);
        txtFolio.setForeground(AppTheme.TEXT_MUTED);

        // Acomodo estratégico de los componentes en el GridBagLayout
        addRow(form, gbc, 0, "Folio:", txtFolio);
        addRow(form, gbc, 1, "Tornillo:", cmbTornillo);

        // 🌟 INYECCIÓN: Agregar la fila del indicador de stock justo debajo del combo de selección
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        form.add(lblInfoStock, gbc);

        addRow(form, gbc, 3, "Cantidad:", txtCantidad);
        addRow(form, gbc, 4, "Precio Unitario:", txtPrecio);
        addRow(form, gbc, 5, "No. Factura:", txtFactura);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        form.add(AppTheme.label("Observaciones:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(AppTheme.darkScrollPane(txtObs), gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnCancel = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.successButton("Registrar Entrada");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            try {
                Tornillo t = (Tornillo) cmbTornillo.getSelectedItem();
                if (t == null) throw new IllegalArgumentException("Debe seleccionar un tornillo de la lista.");
                
                int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
                if (cantidad <= 0)
                    throw new IllegalArgumentException("Cantidad debe ser mayor a 0");
                if (precio.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("El precio unitario debe ser un número positivo mayor a 0.");

                // 🌟 NUEVA VALIDACIÓN DE BLOQUEO: Verificar el límite máximo de almacén
                if (t.getStockMaximo() > 0) {
                    int stockProyectado = t.getStockActual() + cantidad;
                    if (stockProyectado > t.getStockMaximo()) {
                        throw new IllegalArgumentException(String.format(
                            "Operación denegada: La cantidad ingresada supera el stock máximo.\n\n" +
                            "▪ Stock Actual: %d pzas\n" +
                            "▪ Capacidad Máxima: %d pzas\n" +
                            "▪ Espacio Libre Disponible: %d pzas\n\n" +
                            "Por favor, reduzca el volumen de la entrada.",
                            t.getStockActual(), t.getStockMaximo(), (t.getStockMaximo() - t.getStockActual())
                        ));
                    }
                }

                BigDecimal totalCalculado = precio.multiply(BigDecimal.valueOf(cantidad));
                String resumenTicket = String.format(
                    "¿Confirmar el registro de esta Entrada?\n\n" +
                    "▪ Producto:  %s\n" +
                    "▪ Cantidad:  %d unidades\n" +
                    "▪ P. Unitario: $%,.2f\n" +
                    "▪ Total Operación: $%,.2f\n\n",
                    t.getNombre(), cantidad, precio, totalCalculado
                );
                int confirmacion = JOptionPane.showConfirmDialog(
                    dlg, resumenTicket, "Confirmar Entrada de Inventario",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
                );
                if (confirmacion != JOptionPane.YES_OPTION) return;

                Entrada entrada = new Entrada();
                entrada.setFolio(folio);
                entrada.setTornilloId(t.getId());
                entrada.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
                entrada.setCantidad(cantidad);
                entrada.setPrecioUnitario(precio);
                entrada.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)));
                entrada.setNumeroFactura(txtFactura.getText().trim());
                entrada.setObservaciones(txtObs.getText().trim());

                entradaService.registrarEntrada(entrada);
                
                dlg.dispose();
                refresh();
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
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0; 
        form.add(btns, gbc);
        btns.add(btnCancel);
        btns.add(btnGuardar);
        dlg.add(new JScrollPane(form));
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
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

    private void poblarTabla(List<Entrada> lista) {
        tableModel.setRowCount(0);
        for (Entrada e : lista) {
            // Aplicación del formateador bidireccional humano
            String fechaFormateada = "";
            if (e.getFecha() != null) {
                fechaFormateada = e.getFecha().format(visualFormatter);
            }

            tableModel.addRow(new Object[] {
                    e.getId(), e.getFolio(),
                    e.getTornilloNombre(), e.getTornilloCodigo(),
                    e.getCantidad(), e.getPrecioUnitario(), e.getTotal(),
                    e.getUsuarioNombre(),
                    fechaFormateada // Inyección del string formateado limpiamente
            });
        }
        lblConteo.setText(lista.size() + " entrada(s)");
    }

    // 🌟 CORREGIDO: Colores adaptados al Design System corporativo para alto contraste
    private void actualizarInformacionStock(JLabel lblInfoStock, Tornillo t, String txtCantStr) {
        if (t == null) {
            lblInfoStock.setText(" ");
            return;
        }

        int actual = t.getStockActual();
        int max = t.getStockMaximo();
        int ingresado = 0;

        try {
            if (!txtCantStr.trim().isEmpty()) {
                ingresado = Integer.parseInt(txtCantStr.trim());
            }
        } catch (NumberFormatException e) {
            // Ignorar errores de casteo temporal mientras el usuario escribe
        }

        if (max == 0) {
            lblInfoStock.setText(String.format("Stock Actual: %d pzas | Capacidad Máxima: Sin límite", actual));
            lblInfoStock.setForeground(AppTheme.TEXT_SECONDARY); // 🌟 CORREGIDO
        } else {
            int disponible = max - actual;
            int proyectado = actual + ingresado;

            if (proyectado > max) {
                lblInfoStock.setText(String.format("⚠️ ¡Exceso! Proyectado: %d / Máx: %d (Espacio libre: %d)", proyectado, max, disponible));
                lblInfoStock.setForeground(AppTheme.DANGER_TEXT); // 🌟 CORREGIDO: Rojo Fiori corporativo brillante
            } else {
                lblInfoStock.setText(String.format("Stock Actual: %d / Máx: %d (Disponible para entrada: %d)", actual, max, disponible));
                lblInfoStock.setForeground(AppTheme.TEXT_SECONDARY); // 🌟 CORREGIDO: Gris legible
            }
        }
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Entrada>, Void>() {
            @Override
            protected List<Entrada> doInBackground() throws Exception {
                String termino = txtBuscar.getText().trim();
                String rawDesde = txtDesde.getText().trim();
                String rawHasta = txtHasta.getText().trim();
                
                String desde = (rawDesde.isEmpty() || rawDesde.contains("_")) ? null : rawDesde;
                String hasta = (rawHasta.isEmpty() || rawHasta.contains("_")) ? null : rawHasta;
                
                return entradaService.buscarEntradas(termino, desde, hasta);
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        poblarTabla(get());
                    }
                } catch (Exception ex) {
                    // Control preventivo de flujo asíncrono
                }
            }
        };
        currentWorker.execute();
    }
}