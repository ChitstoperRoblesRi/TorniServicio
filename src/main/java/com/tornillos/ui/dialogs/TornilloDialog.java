package com.tornillos.ui.dialogs;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.*;
import com.tornillos.model.*;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;

public class TornilloDialog extends JDialog {
    private boolean guardado = false;
    private final Tornillo tornillo;

    private JTextField txtCodigo, txtNombre, txtMaterial, txtDiametro, txtLongitud,
            txtPaso, txtCabeza, txtUnidad, txtUbicacion, txtStockInicial, txtStockMin, txtStockMax;
    private JTextField txtPrecioCosto, txtPrecioVenta;
    private JTextArea txtDescripcion;

    private final TornilloDAO tornilloDAO = new TornilloDAO();

    public TornilloDialog(JFrame parent, Tornillo tornillo) {
        super(parent, tornillo == null ? "Nuevo Tornillo" : "Editar Tornillo", true);
        this.tornillo = tornillo;
        setSize(620, 600);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(AppTheme.BG_CARD);
        buildUI();
        if (tornillo != null)
            cargarDatos();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(AppTheme.BG_CARD);
        main.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JLabel title = new JLabel(tornillo == null ? "Nuevo Tornillo" : "✏ Editar Tornillo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        main.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(AppTheme.BG_CARD);
        tabs.setForeground(AppTheme.TEXT_PRIMARY);
        tabs.setFont(AppTheme.FONT_BODY);

        tabs.addTab("General", buildGeneralTab());
        tabs.addTab("Especificaciones", buildEspecTab());
        tabs.addTab("Precios & Stock", buildStockTab());
        main.add(tabs, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton btnCancel = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.primaryButton(tornillo == null ? "Crear Tornillo" : "Guardar Cambios");
        btnCancel.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        btns.add(btnCancel);
        btns.add(btnGuardar);
        main.add(btns, BorderLayout.SOUTH);

        add(main);
    }

    private JPanel buildGeneralTab() {
        JPanel p = formPanel();
        GridBagConstraints gbc = gbc();

        txtCodigo = AppTheme.styledField("ej. TM-M8-50");
        txtNombre = AppTheme.styledField("ej. Tornillo Métrico M8 x 50mm");
        txtDescripcion = AppTheme.styledTextArea();
        txtDescripcion.setRows(3);
        txtUbicacion = AppTheme.styledField("ej. Pasillo A - Estante 3");

        addRow(p, gbc, 0, "Código *:", txtCodigo);
        addRow(p, gbc, 1, "Nombre *:", txtNombre);
        gbc.gridx = 0;
        gbc.gridy = 2;
        p.add(AppTheme.label("Descripción:"), gbc);
        gbc.gridx = 1;
        p.add(AppTheme.darkScrollPane(txtDescripcion), gbc);
        addRow(p, gbc, 3, "Ubicación:", txtUbicacion);
        return p;
    }

    private JPanel buildEspecTab() {
        JPanel p = formPanel();
        GridBagConstraints gbc = gbc();
        txtMaterial = AppTheme.styledField("ej. Acero Inoxidable, Zinc, Galvanizado");
        txtDiametro = AppTheme.styledField("ej. 8.0");
        txtLongitud = AppTheme.styledField("ej. 50.0");
        txtPaso = AppTheme.styledField("ej. 1.25");
        txtCabeza = AppTheme.styledField("ej. Hexagonal, Phillips, Plana, Allen");
        txtUnidad = AppTheme.styledField("ej. PZA, KG, CAJA");
        txtUnidad.setText("PZA");
        addRow(p, gbc, 0, "Material:", txtMaterial);
        addRow(p, gbc, 1, "Diámetro (mm):", txtDiametro);
        addRow(p, gbc, 2, "Longitud (mm):", txtLongitud);
        addRow(p, gbc, 3, "Paso Rosca (mm):", txtPaso);
        addRow(p, gbc, 4, "Tipo de Cabeza:", txtCabeza);
        addRow(p, gbc, 5, "Unidad de Medida:", txtUnidad);
        return p;
    }

    private JPanel buildStockTab() {
        JPanel p = formPanel();
        GridBagConstraints gbc = gbc();
        txtPrecioCosto = AppTheme.styledField("0.00");
        txtPrecioVenta = AppTheme.styledField("0.00");
        txtStockInicial = AppTheme.styledField("0");
        txtStockMin = AppTheme.styledField("10");
        txtStockMax = AppTheme.styledField("1000");
        if (tornillo != null)
            txtStockInicial.setEditable(false);
        addRow(p, gbc, 0, "Precio Costo ($):", txtPrecioCosto);
        addRow(p, gbc, 1, "Precio Venta ($):", txtPrecioVenta);
        addRow(p, gbc, 2, tornillo == null ? "Stock Inicial:" : "Stock Actual (solo lectura):", txtStockInicial);
        addRow(p, gbc, 3, "Stock Mínimo (alerta):", txtStockMin);
        addRow(p, gbc, 4, "Stock Máximo:", txtStockMax);
        return p;
    }

    private void cargarDatos() {
        txtCodigo.setText(tornillo.getCodigo());
        txtNombre.setText(tornillo.getNombre());
        txtDescripcion.setText(tornillo.getDescripcion());
        txtMaterial.setText(tornillo.getMaterial());
        if (tornillo.getDiametroMm() != null)
            txtDiametro.setText(tornillo.getDiametroMm().toString());
        if (tornillo.getLongitudMm() != null)
            txtLongitud.setText(tornillo.getLongitudMm().toString());
        if (tornillo.getPasoRosca() != null)
            txtPaso.setText(tornillo.getPasoRosca().toString());
        txtCabeza.setText(tornillo.getCabezaTipo());
        txtUnidad.setText(tornillo.getUnidadMedida());
        txtUbicacion.setText(tornillo.getUbicacion());
        if (tornillo.getPrecioCosto() != null)
            txtPrecioCosto.setText(tornillo.getPrecioCosto().toString());
        if (tornillo.getPrecioVenta() != null)
            txtPrecioVenta.setText(tornillo.getPrecioVenta().toString());
        txtStockInicial.setText(String.valueOf(tornillo.getStockActual()));
        txtStockMin.setText(String.valueOf(tornillo.getStockMinimo()));
        txtStockMax.setText(String.valueOf(tornillo.getStockMaximo()));
    }

    private void guardar() {
        try {
            if (txtCodigo.getText().isBlank() || txtNombre.getText().isBlank())
                throw new IllegalArgumentException("Código y Nombre son obligatorios");

            Tornillo t = tornillo != null ? tornillo : new Tornillo();
            t.setCodigo(txtCodigo.getText().trim());
            t.setNombre(txtNombre.getText().trim());
            t.setDescripcion(txtDescripcion.getText().trim());
            t.setMaterial(txtMaterial.getText().trim());
            t.setCabezaTipo(txtCabeza.getText().trim());
            t.setUnidadMedida(txtUnidad.getText().trim().isEmpty() ? "PZA" : txtUnidad.getText().trim());
            t.setUbicacion(txtUbicacion.getText().trim());
            if (!txtDiametro.getText().isBlank())
                t.setDiametroMm(new BigDecimal(txtDiametro.getText().trim()));
            if (!txtLongitud.getText().isBlank())
                t.setLongitudMm(new BigDecimal(txtLongitud.getText().trim()));
            if (!txtPaso.getText().isBlank())
                t.setPasoRosca(new BigDecimal(txtPaso.getText().trim()));
            t.setPrecioCosto(
                    new BigDecimal(txtPrecioCosto.getText().trim().isEmpty() ? "0" : txtPrecioCosto.getText().trim()));
            t.setPrecioVenta(
                    new BigDecimal(txtPrecioVenta.getText().trim().isEmpty() ? "0" : txtPrecioVenta.getText().trim()));
            t.setStockMinimo(
                    txtStockMin.getText().trim().isEmpty() ? 10 : Integer.parseInt(txtStockMin.getText().trim()));
            t.setStockMaximo(
                    txtStockMax.getText().trim().isEmpty() ? 1000 : Integer.parseInt(txtStockMax.getText().trim()));

            if (tornillo == null) {
                t.setStockActual(Integer
                        .parseInt(txtStockInicial.getText().trim().isEmpty() ? "0" : txtStockInicial.getText().trim()));
                tornilloDAO.crear(t);
            } else {
                tornilloDAO.actualizar(t);
            }
            guardado = true;
            dispose();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(AppTheme.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 6, 6, 6);
        return g;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        p.add(field, gbc);
    }

    public boolean isGuardado() {
        return guardado;
    }
}
