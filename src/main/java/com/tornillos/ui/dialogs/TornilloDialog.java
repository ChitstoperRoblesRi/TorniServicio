package com.tornillos.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
// import java.awt.event.KeyAdapter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.Border;
// import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Categoria;
import com.tornillos.model.Tornillo;
import com.tornillos.service.TornilloService;

public class TornilloDialog extends JDialog {

    private boolean guardado = false;
    private final Tornillo tornillo;

    // Cambiado: Ahora interactuamos únicamente con la Capa de Servicio
    private final TornilloService tornilloService = new TornilloService();

    private JTextField txtCodigo, txtNombre, txtUbicacion;
    private JTextField txtDiametro, txtLongitud, txtPaso;
    private JTextField txtPrecioCosto, txtPrecioVenta;
    private JTextField txtStockInicial, txtStockMin, txtStockMax;
    private JTextArea  txtDescripcion;

    private JComboBox<Categoria>  cmbCategoria;
    private JComboBox<String>     cmbSistemaMedida;
    private JComboBox<String>     cmbMaterial;
    private JComboBox<String>     cmbCabeza;
    private JComboBox<String>     cmbUnidad;

    private JLabel lblDiametro, lblLongitud, lblPaso;

    private static final Border BORDER_ERROR  = BorderFactory.createLineBorder(new Color(220, 53, 69), 2);
    private static final Border BORDER_NORMAL = BorderFactory.createLineBorder(new Color(100, 100, 100));

    private static final String[] MATERIALES = {
        "", "Acero al carbono", "Acero inoxidable 304", "Acero inoxidable 316",
        "Acero galvanizado", "Acero zincado", "Latón", "Cobre", "Aluminio", "Titanio", "Zinc", "Nylon/Plástico", "Otro"
    };
    private static final String[] CABEZAS = {
        "", "Hexagonal", "Hexagonal hueco (Allen)", "Phillips", "Plana (ranura)",
        "Pozidriv", "Torx", "Carruaje", "Cilíndrica", "Gota", "Ojo", "Remache", "Otro"
    };
    private static final String[] UNIDADES = {
        "PZA", "KG", "BOLSA", "CAJA", "METRO", "PAQ", "Otro"
    };

    public TornilloDialog(JFrame parent, Tornillo tornillo) {
        super(parent, tornillo == null ? "Nuevo Tornillo" : "Editar Tornillo", true);
        this.tornillo = tornillo;
        getContentPane().setBackground(AppTheme.BG_CARD);
        buildUI();
        if (tornillo != null) cargarDatos();
        pack();
        setMinimumSize(new Dimension(580, 520));
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(AppTheme.BG_CARD);
        main.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JLabel title = new JLabel(tornillo == null ? "Nuevo Tornillo" : "✏  Editar Tornillo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        main.add(title, BorderLayout.NORTH);

        JPanel formPanel = buildFormPanel();
        JScrollPane scroll = new JScrollPane(formPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(AppTheme.BG_CARD);

        main.add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton btnCancel  = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.primaryButton(tornillo == null ? "Crear Tornillo" : "Guardar Cambios");

        btnCancel.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());

        btns.add(btnCancel); btns.add(btnGuardar);
        main.add(btns, BorderLayout.SOUTH);

        add(main);
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(AppTheme.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

        p.add(sectionLabel("Identificación"));
        p.add(buildIdentificacionPanel());
        p.add(Box.createVerticalStrut(12));

        p.add(sectionLabel("Especificaciones técnicas"));
        p.add(buildEspecificacionesPanel());
        p.add(Box.createVerticalStrut(12));

        p.add(sectionLabel("Precios"));
        p.add(buildPreciosPanel());
        p.add(Box.createVerticalStrut(12));

        p.add(sectionLabel("Stock y ubicación"));
        p.add(buildStockPanel());

        return p;
    }

    private JPanel buildIdentificacionPanel() {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        txtCodigo = AppTheme.styledField("ej. TOR-HEX-M8-ZN");
        addRow(p, gbc, 0, "Código *:", txtCodigo);

        txtNombre = AppTheme.styledField("ej. Tornillo Hexagonal M8 x 30mm Zincado");
        addRow(p, gbc, 1, "Nombre *:", txtNombre);

        cmbCategoria = new JComboBox<>();
        cmbCategoria.addItem(sinSeleccion(Categoria.class));
        try {
            // Cambiado: Solicitud de listado redirigida al Servicio
            List<Categoria> categorias = tornilloService.obtenerTodasLasCategorias();
            for (Categoria c : categorias) cmbCategoria.addItem(c);
        } catch (SQLException ex) {
            System.err.println("No se pudieron cargar categorías: " + ex.getMessage());
        }

        aplicarEstiloPremiumCombo(cmbCategoria);
        addRow(p, gbc, 2, "Categoría:", cmbCategoria);

        txtDescripcion = AppTheme.styledTextArea();
        txtDescripcion.setRows(3);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        p.add(AppTheme.label("Descripción:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        p.add(AppTheme.darkScrollPane(txtDescripcion), gbc);

        return p;
    }

    private JPanel buildEspecificacionesPanel() {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        txtDiametro = AppTheme.styledField("ej. 8.0");
        txtLongitud = AppTheme.styledField("ej. 30.0");
        txtPaso     = AppTheme.styledField("ej. 1.25");

        lblDiametro = AppTheme.label("Diámetro (mm):");
        lblLongitud = AppTheme.label("Longitud (mm):");
        lblPaso     = AppTheme.label("Paso de rosca (mm):");

        cmbSistemaMedida = new JComboBox<>(new String[]{"MÉTRICO", "IMPERIAL"}) {
            @Override
            public void setSelectedItem(Object anObject) {
                super.setSelectedItem(anObject);
                if (anObject != null && lblDiametro != null) {
                    String texto = anObject.toString().toUpperCase();
                    if (texto.contains("IMP")) {
                        lblDiametro.setText("Diámetro (pulg):");
                        lblLongitud.setText("Longitud (pulg):");
                        lblPaso.setText("Hilos / Rosca (TPI):");
                    } else {
                        lblDiametro.setText("Diámetro (mm):");
                        lblLongitud.setText("Longitud (mm):");
                        lblPaso.setText("Paso de rosca (mm):");
                    }
                    txtPaso.setEnabled(true);
                    p.revalidate();
                    p.repaint();
                }
            }
        };
        aplicarEstiloPremiumCombo(cmbSistemaMedida);
        addRow(p, gbc, 0, "Sistema de medida:", cmbSistemaMedida);

        cmbMaterial = new JComboBox<>(MATERIALES);
        configurarOpcionOtro(cmbMaterial);
        aplicarEstiloPremiumCombo(cmbMaterial);
        addRow(p, gbc, 1, "Material:", cmbMaterial);

        cmbCabeza = new JComboBox<>(CABEZAS);
        configurarOpcionOtro(cmbCabeza);
        aplicarEstiloPremiumCombo(cmbCabeza);
        addRow(p, gbc, 2, "Tipo de cabeza:", cmbCabeza);

        cmbUnidad = new JComboBox<>(UNIDADES);
        configurarOpcionOtro(cmbUnidad);
        aplicarEstiloPremiumCombo(cmbUnidad);
        addRow(p, gbc, 3, "Unidad de medida:", cmbUnidad);

        addRow(p, gbc, 4, lblDiametro, txtDiametro);
        addRow(p, gbc, 5, lblLongitud, txtLongitud);
        addRow(p, gbc, 6, lblPaso, txtPaso);

        return p;
    }

    private JPanel buildPreciosPanel() {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        txtPrecioCosto  = AppTheme.styledField("0.00");
        txtPrecioVenta  = AppTheme.styledField("0.00");

        addRow(p, gbc, 0, "Precio de costo ($) *:", txtPrecioCosto);
        addRow(p, gbc, 1, "Precio de venta ($) *:", txtPrecioVenta);

        JLabel hint = new JLabel("⚠  El precio de venta debe ser mayor o igual al costo.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(160, 120, 0));
        gbc.gridx = 1; gbc.gridy = 2; p.add(hint, gbc);

        return p;
    }

    private JPanel buildStockPanel() {
        JPanel p = sectionPanel();
        GridBagConstraints gbc = defaultGbc();

        txtStockInicial = AppTheme.styledField("0");
        txtStockMin     = AppTheme.styledField("10");
        txtStockMax     = AppTheme.styledField("1000");
        txtUbicacion    = AppTheme.styledField("ej. Estante A-3, Cajón 2");

        if (tornillo != null) {
            txtStockInicial.setEditable(false);
        }

        addRow(p, gbc, 0, tornillo == null ? "Stock inicial *:" : "Stock actual (solo lectura):", txtStockInicial);
        addRow(p, gbc, 1, "Stock mínimo (alerta) *:", txtStockMin);
        addRow(p, gbc, 2, "Stock máximo:", txtStockMax);
        addRow(p, gbc, 3, "Ubicación en almacén:", txtUbicacion);

        JLabel hintMin = new JLabel("⚠  Un stock mínimo de 0 deshabilita las alertas de reabastecimiento.");
        hintMin.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintMin.setForeground(new Color(160, 120, 0));
        gbc.gridx = 1; gbc.gridy = 4; p.add(hintMin, gbc);

        return p;
    }

    private void cargarDatos() {
        txtCodigo.setText(tornillo.getCodigo());
        txtNombre.setText(tornillo.getNombre());
        txtDescripcion.setText(tornillo.getDescripcion());
        txtUbicacion.setText(tornillo.getUbicacion());

        if (tornillo.getCategoriaId() > 0) {
            for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
                Categoria c = cmbCategoria.getItemAt(i);
                if (c != null && c.getId() == tornillo.getCategoriaId()) {
                    cmbCategoria.setSelectedIndex(i);
                    break;
                }
            }
        }

        String sistema = tornillo.getSistemaMedida() != null ? tornillo.getSistemaMedida() : "METRICO";
        cmbSistemaMedida.setSelectedItem(sistema.equalsIgnoreCase("IMPERIAL") ? "IMPERIAL" : "MÉTRICO");
        for (java.awt.event.ActionListener al : cmbSistemaMedida.getActionListeners()) {
            al.actionPerformed(new java.awt.event.ActionEvent(cmbSistemaMedida, java.awt.event.ActionEvent.ACTION_PERFORMED, null));
        }

        seleccionarCombo(cmbMaterial, tornillo.getMaterial());
        seleccionarCombo(cmbCabeza,   tornillo.getCabezaTipo());
        seleccionarCombo(cmbUnidad,   tornillo.getUnidadMedida() != null ? tornillo.getUnidadMedida() : "PZA");

        if (tornillo.getDiametroMm() != null) txtDiametro.setText(tornillo.getDiametroMm().toPlainString());
        if (tornillo.getLongitudMm() != null) txtLongitud.setText(tornillo.getLongitudMm().toPlainString());
        if (tornillo.getPasoRosca()  != null) txtPaso.setText(tornillo.getPasoRosca().toPlainString());

        if (tornillo.getPrecioCosto() != null) txtPrecioCosto.setText(tornillo.getPrecioCosto().toPlainString());
        if (tornillo.getPrecioVenta() != null) txtPrecioVenta.setText(tornillo.getPrecioVenta().toPlainString());

        txtStockInicial.setText(String.valueOf(tornillo.getStockActual()));
        txtStockMin.setText(String.valueOf(tornillo.getStockMinimo()));
        txtStockMax.setText(String.valueOf(tornillo.getStockMaximo()));
    }

    private void guardar() {
        resetBordes();
        Tornillo t = validarYConstruir();
        if (t == null) return;

        try {
            // Cambiado: Registro y mutación del catálogo delegados al Servicio unificado
            tornilloService.guardarTornillo(t);
            guardado = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            marcarError(txtCodigo, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar en BD:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Tornillo validarYConstruir() {
        String codigo  = txtCodigo.getText().trim();
        String nombre  = txtNombre.getText().trim();

        if (codigo.isBlank()) { marcarError(txtCodigo, "El código es obligatorio."); return null; }
        if (nombre.isBlank()) { marcarError(txtNombre, "El nombre es obligatorio."); return null; }

        try {
            int idActual = (tornillo != null) ? tornillo.getId() : -1;
            // Cambiado: Verificación cruzada enviada a través de la capa lógica
            if (tornilloService.existeCodigoProducto(codigo, idActual)) {
                marcarError(txtCodigo, "El código \"" + codigo + "\" ya está asignado a otro tornillo.");
                return null;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al verificar código.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        BigDecimal precioCosto;
        try {
            precioCosto = new BigDecimal(txtPrecioCosto.getText().trim().isEmpty() ? "0" : txtPrecioCosto.getText().trim());
            if (precioCosto.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            marcarError(txtPrecioCosto, "Costo inválido."); return null;
        }

        BigDecimal precioVenta;
        try {
            precioVenta = new BigDecimal(txtPrecioVenta.getText().trim().isEmpty() ? "0" : txtPrecioVenta.getText().trim());
            if (precioVenta.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            marcarError(txtPrecioVenta, "Venta inválida."); return null;
        }

        if (precioVenta.compareTo(precioCosto) < 0) {
            marcarError(txtPrecioVenta, "El precio de venta no puede ser menor al costo."); return null;
        }

        int stockMin;
        try {
            stockMin = Integer.parseInt(txtStockMin.getText().trim().isEmpty() ? "0" : txtStockMin.getText().trim());
            if (stockMin < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            marcarError(txtStockMin, "Stock mínimo inválido."); return null;
        }

        int stockMax;
        try {
            stockMax = Integer.parseInt(txtStockMax.getText().trim().isEmpty() ? "1000" : txtStockMax.getText().trim());
            if (stockMax < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            marcarError(txtStockMax, "Stock máximo inválido."); return null;
        }
        if (stockMax > 0 && stockMax < stockMin) {
            marcarError(txtStockMax, "Stock máximo no puede ser menor al mínimo."); return null;
        }

        int stockInicial = 0;
        if (tornillo == null) {
            try {
                stockInicial = Integer.parseInt(txtStockInicial.getText().trim().isEmpty() ? "0" : txtStockInicial.getText().trim());
                if (stockInicial < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                marcarError(txtStockInicial, "Stock inicial inválido."); return null;
            }
        }

        BigDecimal diametro = null, longitud = null, pasoRosca = null;
        if (!txtDiametro.getText().isBlank()) {
            try {
                diametro = parseMedida(txtDiametro.getText());
                if (diametro.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                marcarError(txtDiametro, "Diámetro inválido (ej. 8.0 o 1/4)."); return null;
            }
        }
        if (!txtLongitud.getText().isBlank()) {
            try {
                longitud = parseMedida(txtLongitud.getText());
                if (longitud.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                marcarError(txtLongitud, "Longitud inválida (ej. 30.0 o 1)."); return null;
            }
        }
        
        if (!txtPaso.getText().isBlank()) {
            try {
                pasoRosca = new BigDecimal(txtPaso.getText().trim());
                if (pasoRosca.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                marcarError(txtPaso, "Paso de rosca o TPI inválido."); return null;
            }
        }

        Tornillo t = (tornillo != null) ? tornillo : new Tornillo();
        t.setCodigo(codigo); t.setNombre(nombre);
        t.setDescripcion(txtDescripcion.getText().trim());
        t.setUbicacion(txtUbicacion.getText().trim());
        
        Object sistemaSel = cmbSistemaMedida.getSelectedItem();
        String sistemaTexto = (sistemaSel != null) ? sistemaSel.toString().toUpperCase() : "";
        t.setSistemaMedida(sistemaTexto.contains("IMP") ? "IMPERIAL" : "METRICO");

        int catIndex = cmbCategoria.getSelectedIndex();
        Categoria catSel = (catIndex >= 0) ? cmbCategoria.getItemAt(catIndex) : null;
        if (catSel != null && catSel.getId() > 0) t.setCategoriaId(catSel.getId());

        String material = (String) cmbMaterial.getSelectedItem();
        t.setMaterial(material != null && !material.isBlank() ? material : null);

        String cabeza = (String) cmbCabeza.getSelectedItem();
        t.setCabezaTipo(cabeza != null && !cabeza.isBlank() ? cabeza : null);

        String unidad = (String) cmbUnidad.getSelectedItem();
        t.setUnidadMedida(unidad != null && !unidad.isBlank() ? unidad : "PZA");

        t.setDiametroMm(diametro); t.setLongitudMm(longitud); t.setPasoRosca(pasoRosca);
        t.setPrecioCosto(precioCosto); t.setPrecioVenta(precioVenta);
        t.setStockMinimo(stockMin); t.setStockMaximo(stockMax);
        if (tornillo == null) t.setStockActual(stockInicial);

        return t;
    }

    private BigDecimal parseMedida(String text) throws NumberFormatException {
        text = text.trim().replaceAll("\\s+", " ");
        if (text.contains("/")) {
            if (text.contains(" ")) {
                String[] partesMixtas = text.split(" ");
                if (partesMixtas.length == 2) {
                    double entero = Double.parseDouble(partesMixtas[0]);
                    String[] fraccion = partesMixtas[1].split("/");
                    double numerador = Double.parseDouble(fraccion[0]);
                    double denominador = Double.parseDouble(fraccion[1]);
                    if (denominador == 0) throw new NumberFormatException();
                    return BigDecimal.valueOf(entero + (numerador / denominador));
                }
            }
            String[] partes = text.split("/");
            if (partes.length == 2) {
                double numerador = Double.parseDouble(partes[0]);
                double denominador = Double.parseDouble(partes[1]);
                if (denominador == 0) throw new NumberFormatException();
                return BigDecimal.valueOf(numerador / denominador);
            }
        }
        return new BigDecimal(text);
    }

    private void configurarOpcionOtro(JComboBox<String> combo) {
        combo.addActionListener(e -> {
            if ("Otro".equals(combo.getSelectedItem())) {
                String nuevoValor = JOptionPane.showInputDialog(this,
                        "Escriba el valor personalizado:",
                        "Agregar nueva opción", JOptionPane.QUESTION_MESSAGE);
                if (nuevoValor != null && !nuevoValor.trim().isBlank()) {
                    String limpio = nuevoValor.trim();
                    combo.addItem(limpio);
                    combo.setSelectedItem(limpio);
                } else {
                    combo.setSelectedIndex(0);
                }
            }
        });
    }

    private void marcarError(JComponent campo, String mensaje) {
        campo.setBorder(BORDER_ERROR);
        campo.requestFocus();
        campo.scrollRectToVisible(campo.getBounds());
        JOptionPane.showMessageDialog(this, mensaje, "Campo inválido", JOptionPane.WARNING_MESSAGE);
    }

    private void resetBordes() {
        JComponent[] campos = { txtCodigo, txtNombre, txtDiametro, txtLongitud, txtPaso, txtPrecioCosto, txtPrecioVenta, txtStockInicial, txtStockMin, txtStockMax };
        for (JComponent c : campos) if (c != null) c.setBorder(BORDER_NORMAL);
    }

    private JPanel sectionPanel() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(AppTheme.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); return p;
    }

    private JLabel sectionLabel(String texto) {
        JLabel lbl = new JLabel(texto.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11)); lbl.setForeground(new Color(130, 130, 140));
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0)); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private GridBagConstraints defaultGbc() {
        GridBagConstraints g = new GridBagConstraints(); g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(6, 6, 6, 6); return g;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        addRow(p, gbc, row, AppTheme.label(label), field);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, JLabel labelComponent, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3; p.add(labelComponent, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; p.add(field, gbc);
    }

    private void seleccionarCombo(JComboBox<String> combo, String valor) {
        if (valor == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (valor.equalsIgnoreCase(combo.getItemAt(i))) { combo.setSelectedIndex(i); return; }
        }
        combo.addItem(valor); combo.setSelectedItem(valor);
    }

    @SuppressWarnings("unchecked")
    private <T> T sinSeleccion(Class<T> clazz) {
        if (clazz == Categoria.class) {
            Categoria c = new Categoria(); c.setId(0); c.setNombre("— Sin categoría —"); return (T) c;
        }
        return null;
    }

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

        combo.setBorder(new javax.swing.border.Border() {
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

    public boolean isGuardado() {
        return this.guardado;
    }
}