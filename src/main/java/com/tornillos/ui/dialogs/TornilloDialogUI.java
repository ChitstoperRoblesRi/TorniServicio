package com.tornillos.ui.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Categoria;
import com.tornillos.model.Tornillo;
import com.tornillos.service.TornilloService;

class TornilloDialogUI {

    final JTextField txtCodigo, txtNombre, txtUbicacion;
    final JTextField txtDiametro, txtLongitud, txtPaso;
    final JTextField txtPrecioCosto, txtPrecioVenta;
    final JTextField txtStockInicial, txtStockMin, txtStockMax;
    final JTextArea  txtDescripcion;

    final JComboBox<Categoria> cmbCategoria;
    final JComboBox<String>    cmbSistemaMedida;
    final JComboBox<String>    cmbMaterial;
    final JComboBox<String>    cmbCabeza;
    final JComboBox<String>    cmbUnidad;

    final JLabel lblDiametro, lblLongitud, lblPaso;

    private static final Border BORDER_ERROR  = BorderFactory.createLineBorder(new Color(220, 53, 69), 2);
    private static final Border BORDER_NORMAL = BorderFactory.createLineBorder(new Color(100, 100, 100));

    private static final String[] MATERIALES = {
        "", "Acero al carbono", "Acero inoxidable 304", "Acero inoxidable 316",
        "Acero galvanizado", "Acero zincado", "Latón", "Cobre", "Aluminio",
        "Titanio", "Zinc", "Nylon/Plástico"
    };
    private static final String[] CABEZAS = {
        "", "Hexagonal", "Hexagonal hueco (Allen)", "Phillips", "Plana (ranura)",
        "Pozidriv", "Torx", "Carruaje", "Cilíndrica", "Gota", "Ojo", "Remache"
    };
    private static final String[] UNIDADES = {
        "PZA", "KG", "BOLSA", "CAJA", "METRO", "PAQ"
    };

    private JPanel especificacionesPanel;

    private final Component owner;

    // ── Constructor ──────────────────────────────────────────────────────────

    TornilloDialogUI(Component owner, Tornillo tornillo, TornilloService tornilloService) {
        this.owner = owner;

        txtCodigo      = AppTheme.styledField("ej. TOR-HEX-M8-ZN");
        txtNombre      = AppTheme.styledField("ej. Tornillo Hexagonal M8 x 30mm Zincado");
        txtUbicacion   = AppTheme.styledField("ej. Estante A-3, Cajón 2");
        txtDiametro    = AppTheme.styledField("ej. 8.0");
        txtLongitud    = AppTheme.styledField("ej. 30.0");
        txtPaso        = AppTheme.styledField("ej. 1.25");
        txtPrecioCosto = AppTheme.styledField("0.00");
        txtPrecioVenta = AppTheme.styledField("0.00");
        txtStockInicial = AppTheme.styledField("0");
        txtStockMin    = AppTheme.styledField("10");
        txtStockMax    = AppTheme.styledField("1000");
        txtDescripcion = AppTheme.styledTextArea();

        txtDescripcion.setFocusTraversalKeys(
            java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null
        );
        txtDescripcion.setFocusTraversalKeys(
            java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null
        );

        txtDescripcion.setRows(3);

        lblDiametro = AppTheme.label("Diámetro (mm):");
        lblLongitud = AppTheme.label("Longitud (mm):");
        lblPaso     = AppTheme.label("Paso de rosca (mm):");

        cmbCategoria    = buildCmbCategoria(tornilloService);
        cmbSistemaMedida = buildCmbSistemaMedida();
        cmbMaterial     = buildComboConOtro(MATERIALES);
        cmbCabeza       = buildComboConOtro(CABEZAS);
        cmbUnidad       = buildComboConOtro(UNIDADES);

        if (tornillo != null) {
            txtCodigo.setEditable(false);
            txtCodigo.setBackground(AppTheme.BG_DISABLED);
            txtCodigo.setForeground(AppTheme.TEXT_DISABLED);

            txtStockInicial.setEditable(false);
            txtStockInicial.setBackground(AppTheme.BG_DISABLED); 
            txtStockInicial.setForeground(AppTheme.TEXT_DISABLED);
        }
    }

    // ── Construcción del panel del formulario ────────────────────────────────

    /** Devuelve el panel completo con todas las secciones del formulario. */
    JPanel buildFormPanel(Tornillo tornillo) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(AppTheme.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

        p.add(sectionLabel("Identificación"));
        p.add(buildIdentificacionPanel(tornillo != null));
        p.add(Box.createVerticalStrut(12));

        p.add(sectionLabel("Especificaciones técnicas"));
        especificacionesPanel = buildEspecificacionesPanel();
        p.add(especificacionesPanel);
        p.add(Box.createVerticalStrut(12));

        p.add(sectionLabel("Precios"));
        p.add(buildPreciosPanel());
        p.add(Box.createVerticalStrut(12));

        p.add(sectionLabel("Stock y ubicación"));
        p.add(buildStockPanel(tornillo));

        return p;
    }

    // ── Secciones del formulario ─────────────────────────────────────────────

    private JPanel buildIdentificacionPanel(boolean esEdicion) {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        JPanel codigoContenedor = new JPanel(new java.awt.BorderLayout(6, 0));
        codigoContenedor.setOpaque(false);
        codigoContenedor.add(txtCodigo, java.awt.BorderLayout.CENTER);

        JButton btnGenerar = AppTheme.secondaryButton("Generar");
        btnGenerar.setToolTipText("Generar código sugerido según especificaciones actuales");
        btnGenerar.addActionListener(e -> generarPropuestaCodigo());
        
        if (esEdicion) {
            btnGenerar.setEnabled(false);
        }
        
        codigoContenedor.add(btnGenerar, java.awt.BorderLayout.EAST);

        addRow(p, gbc, 0, "Código *:",   codigoContenedor);
        addRow(p, gbc, 1, "Nombre *:",   txtNombre);
        addRow(p, gbc, 2, "Categoría:",  cmbCategoria);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        p.add(AppTheme.label("Descripción:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        p.add(AppTheme.darkScrollPane(txtDescripcion), gbc);

        return p;
    }

    private JPanel buildEspecificacionesPanel() {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        addRow(p, gbc, 0, "Sistema de medida*:", cmbSistemaMedida);
        addRow(p, gbc, 1, "Material:",           cmbMaterial);
        addRow(p, gbc, 2, "Tipo de cabeza:",      cmbCabeza);
        addRow(p, gbc, 3, "Unidad de medida*:",    cmbUnidad);
        addRow(p, gbc, 4, lblDiametro,            txtDiametro);
        addRow(p, gbc, 5, lblLongitud,            txtLongitud);
        addRow(p, gbc, 6, lblPaso,                txtPaso);

        return p;
    }

    private JPanel buildPreciosPanel() {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        addRow(p, gbc, 0, "Precio de costo ($) *:", txtPrecioCosto);
        addRow(p, gbc, 1, "Precio de venta ($) *:", txtPrecioVenta);

        JLabel hint = new JLabel("⚠  El precio de venta debe ser mayor o igual al costo.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(160, 120, 0));
        gbc.gridx = 1; gbc.gridy = 2;
        p.add(hint, gbc);

        return p;
    }

    private JPanel buildStockPanel(Tornillo tornillo) {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        String lblStock = "Cantidad en Stock:";

        addRow(p, gbc, 0, lblStock, txtStockInicial);
        addRow(p, gbc, 1, "Stock mínimo (alerta) *:", txtStockMin);
        addRow(p, gbc, 2, "Stock máximo:",             txtStockMax);
        addRow(p, gbc, 3, "Ubicación en almacén:",     txtUbicacion);

        JLabel hintMin = new JLabel("⚠  Un stock mínimo de 0 deshabilita las alertas de reabastecimiento.");
        hintMin.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintMin.setForeground(new Color(160, 120, 0));
        gbc.gridx = 1; gbc.gridy = 4;
        p.add(hintMin, gbc);

        return p;
    }

    // ── Combos especiales ────────────────────────────────────────────────────

    private JComboBox<Categoria> buildCmbCategoria(TornilloService tornilloService) {
        JComboBox<Categoria> combo = new JComboBox<>();
        combo.addItem(sinSeleccionCategoria());
        try {
            List<Categoria> categorias = tornilloService.obtenerTodasLasCategorias();
            for (Categoria c : categorias) combo.addItem(c);
        } catch (SQLException ex) {
            System.err.println("No se pudieron cargar categorías: " + ex.getMessage());
        }
        aplicarEstiloPremiumCombo(combo);
        return combo;
    }

    private JComboBox<String> buildCmbSistemaMedida() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"MÉTRICO", "IMPERIAL"}) {
            @Override
            public void setSelectedItem(Object anObject) {
                super.setSelectedItem(anObject);
                if (anObject != null && lblDiametro != null) {
                    String texto = anObject.toString().toUpperCase();
                    if (texto.contains("IMP")) {
                        lblDiametro.setText("Diámetro (pulg):");
                        lblLongitud.setText("Longitud (pulg):");
                        lblPaso.setText("Hilos / Rosca (TPI):");
                        
                        txtDiametro.putClientProperty("placeholder", "ej. 1/4 o 0.25");
                        txtLongitud.putClientProperty("placeholder", "ej. 2 o 1 1/2");
                        txtPaso.putClientProperty("placeholder", "ej. 20");
                    } else {
                        lblDiametro.setText("Diámetro (mm):");
                        lblLongitud.setText("Longitud (mm):");
                        lblPaso.setText("Paso de rosca (mm):");
                        
                        txtDiametro.putClientProperty("placeholder", "ej. 8.0");
                        txtLongitud.putClientProperty("placeholder", "ej. 30.0");
                        txtPaso.putClientProperty("placeholder", "ej. 1.25");
                    }
                    txtPaso.setEnabled(true);
                    
                    txtDiametro.repaint();
                    txtLongitud.repaint();
                    txtPaso.repaint();
                    
                    if (especificacionesPanel != null) {
                        especificacionesPanel.revalidate();
                        especificacionesPanel.repaint();
                    }
                }
            }
        };
        aplicarEstiloPremiumCombo(combo);
        return combo;
    }

    private JComboBox<String> buildComboConOtro(String[] opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones);
        // configurarOpcionOtro(combo);
        aplicarEstiloPremiumCombo(combo);
        return combo;
    }

    private void generarPropuestaCodigo() {
        String cabeza = (String) cmbCabeza.getSelectedItem();
        String parteCabeza = "GEN";
        if (cabeza != null && !cabeza.trim().isEmpty()) {
            String limpio = cabeza.trim().toUpperCase();
            parteCabeza = limpio.length() >= 3 ? limpio.substring(0, 3) : limpio;
        }

        String diametro = txtDiametro.getText().trim().toUpperCase();
        String parteDiametro = diametro.isEmpty() ? "X" : diametro.replace("/", "").replace(" ", "");

        String longitud = txtLongitud.getText().trim().toUpperCase();
        String parteLongitud = longitud.isEmpty() ? "X" : longitud.replace("/", "").replace(" ", "");

        String material = (String) cmbMaterial.getSelectedItem();
        String parteMaterial = "GE";
        if (material != null && !material.trim().isEmpty()) {
            String limpio = material.trim().toUpperCase();
            parteMaterial = limpio.length() >= 2 ? limpio.substring(0, 2) : limpio;
        }

        String codigoSugerido = "TOR-" + parteCabeza + "-M" + parteDiametro + "-" + parteMaterial + "-L" + parteLongitud;

        txtCodigo.setText(codigoSugerido);
        
        txtCodigo.requestFocus();
    }

    // ── Helpers de estilo y layout ───────────────────────────────────────────

    void marcarError(JComponent campo, String mensaje) {
        marcarBordeError(campo);
        campo.requestFocus();
        campo.scrollRectToVisible(campo.getBounds());
        JOptionPane.showMessageDialog(owner, mensaje, "Campo inválido", JOptionPane.WARNING_MESSAGE);
    }

    void resetBordes() {
        JComponent[] campos = {
            txtCodigo, txtNombre, txtDiametro, txtLongitud, txtPaso,
            txtPrecioCosto, txtPrecioVenta, txtStockInicial, txtStockMin, txtStockMax
        };
        
        for (JComponent c : campos) {
        if (c != null) {
            Border original = (Border) c.getClientProperty("bordeOriginal");
            if (original != null) {
                c.setBorder(original);
                c.putClientProperty("bordeOriginal", null); 
            }
        }
    }
    }

    void seleccionarCombo(JComboBox<String> combo, String valor) {
        if (valor == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (valor.equalsIgnoreCase(combo.getItemAt(i))) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.addItem(valor);
        combo.setSelectedItem(valor);
    }

    // private void configurarOpcionOtro(JComboBox<String> combo) {
    //     combo.addActionListener(e -> {
    //         if ("Otro".equals(combo.getSelectedItem())) {
    //             String nuevoValor = JOptionPane.showInputDialog(owner,
    //                     "Escriba el valor personalizado:",
    //                     "Agregar nueva opción", JOptionPane.QUESTION_MESSAGE);
    //             if (nuevoValor != null && !nuevoValor.trim().isBlank()) {
    //                 String limpio = nuevoValor.trim();
    //                 combo.addItem(limpio);
    //                 combo.setSelectedItem(limpio);
    //             } else {
    //                 combo.setSelectedIndex(0);
    //             }
    //         }
    //     });
    // }

    private void aplicarEstiloPremiumCombo(JComboBox<?> combo) {
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

        combo.setBorder(new Border() {
            @Override
            public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
            @Override public boolean isBorderOpaque() { return false; }
        });

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
                            if (combo.isPopupVisible()) combo.hidePopup();
                            else combo.showPopup();
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

    void marcarBordeError(JComponent campo) {
        if (campo != null) {
            if (campo.getClientProperty("bordeOriginal") == null) {
                campo.putClientProperty("bordeOriginal", campo.getBorder());
            }
            campo.setBorder(BORDER_ERROR);
        }
    }

    Component getOwner() {
        return this.owner;
    }

    // ── Helpers de layout y etiquetas ────────────────────────────────────────

    private JPanel sectionPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(AppTheme.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return p;
    }

    private JLabel sectionLabel(String texto) {
        JLabel lbl = new JLabel(texto.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(130, 130, 140));
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private GridBagConstraints defaultGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 6, 6, 6);
        return g;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        addRow(p, gbc, row, AppTheme.label(label), field);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, JLabel labelComponent, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        p.add(labelComponent, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        p.add(field, gbc);
    }

    private Categoria sinSeleccionCategoria() {
        Categoria c = new Categoria();
        c.setId(0);
        c.setNombre("— Sin categoría —");
        return c;
    }

    boolean isFormularioModificado(Tornillo original) {
        if (original == null) {
            return !txtCodigo.getText().trim().isEmpty() 
                || !txtNombre.getText().trim().isEmpty()
                || !txtDescripcion.getText().trim().isEmpty()
                || (!txtPrecioCosto.getText().equals("0.00") && !txtPrecioCosto.getText().isEmpty())
                || (!txtPrecioVenta.getText().equals("0.00") && !txtPrecioVenta.getText().isEmpty());
        }
        
        return !txtNombre.getText().trim().equals(original.getNombre())
            || !txtDescripcion.getText().trim().equals(original.getDescripcion() != null ? original.getDescripcion() : "")
            || !txtUbicacion.getText().trim().equals(original.getUbicacion() != null ? original.getUbicacion() : "");
    }
}