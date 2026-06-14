package com.tornillos.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Categoria;
import com.tornillos.model.Tornillo;
import com.tornillos.service.TornilloService;

public class TornilloDialog extends JDialog {

    private boolean guardado = false;
    private final Tornillo tornillo;

    private final TornilloService       tornilloService = new TornilloService();
    private final TornilloDialogUI      ui;
    private final TornilloFormValidator validator;

    public TornilloDialog(JFrame parent, Tornillo tornillo) {
        super(parent, tornillo == null ? "Nuevo Tornillo" : "Editar Tornillo", true);
        this.tornillo  = tornillo;

        ui        = new TornilloDialogUI(this, tornillo, tornilloService);
        validator = new TornilloFormValidator(ui, tornillo);

        getContentPane().setBackground(AppTheme.BG_CARD);
        buildUI();
        if (tornillo != null) cargarDatos();

        pack();
        setMinimumSize(new Dimension(580, 520));
        setLocationRelativeTo(parent);
    }

    // ── Construcción de la ventana ───────────────────────────────────────────

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(AppTheme.BG_CARD);
        main.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JLabel title = new JLabel(tornillo == null ? "Nuevo Tornillo" : "✏  Editar Tornillo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        main.add(title, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(
                ui.buildFormPanel(tornillo),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(AppTheme.BG_CARD);
        main.add(scroll, BorderLayout.CENTER);

        main.add(buildButtonPanel(), BorderLayout.SOUTH);

        add(main);
    }

    private JPanel buildButtonPanel() {
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton btnCancel  = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.primaryButton(tornillo == null ? "Crear Tornillo" : "Guardar Cambios");

        btnCancel.addActionListener(e -> procesarCierreSeguro());
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                procesarCierreSeguro();
            }
        });
        btnGuardar.addActionListener(e -> guardar());

        btns.add(btnCancel);
        btns.add(btnGuardar);
        return btns;
    }

    // ── Carga de datos (modo edición) ────────────────────────────────────────

    private void cargarDatos() {
        ui.txtCodigo.setText(tornillo.getCodigo());
        ui.txtNombre.setText(tornillo.getNombre());
        ui.txtDescripcion.setText(tornillo.getDescripcion());
        ui.txtUbicacion.setText(tornillo.getUbicacion());

        if (tornillo.getCategoriaId() > 0) {
            for (int i = 0; i < ui.cmbCategoria.getItemCount(); i++) {
                Categoria c = ui.cmbCategoria.getItemAt(i);
                if (c != null && c.getId() == tornillo.getCategoriaId()) {
                    ui.cmbCategoria.setSelectedIndex(i);
                    break;
                }
            }
        }

        String sistema = tornillo.getSistemaMedida() != null ? tornillo.getSistemaMedida() : "METRICO";
        boolean esImperial = sistema.equalsIgnoreCase("IMPERIAL");
        ui.cmbSistemaMedida.setSelectedItem(esImperial ? "IMPERIAL" : "MÉTRICO");
        for (java.awt.event.ActionListener al : ui.cmbSistemaMedida.getActionListeners()) {
            al.actionPerformed(new java.awt.event.ActionEvent(
                    ui.cmbSistemaMedida, java.awt.event.ActionEvent.ACTION_PERFORMED, null));
        }

        ui.seleccionarCombo(ui.cmbMaterial, tornillo.getMaterial());
        ui.seleccionarCombo(ui.cmbCabeza,   tornillo.getCabezaTipo());
        ui.seleccionarCombo(ui.cmbUnidad,   tornillo.getUnidadMedida() != null ? tornillo.getUnidadMedida() : "PZA");

        java.math.BigDecimal mmAInches = new java.math.BigDecimal("25.4");

        if (esImperial) {
            if (tornillo.getDiametroMm() != null) {
                java.math.BigDecimal diaIn = tornillo.getDiametroMm().divide(mmAInches, java.math.MathContext.DECIMAL64);
                ui.txtDiametro.setText(convertirDecimalAFraccion(diaIn));
            }
            if (tornillo.getLongitudMm() != null) {
                java.math.BigDecimal lonIn = tornillo.getLongitudMm().divide(mmAInches, java.math.MathContext.DECIMAL64);
                ui.txtLongitud.setText(convertirDecimalAFraccion(lonIn));
            }
            if (tornillo.getPasoRosca() != null && tornillo.getPasoRosca().compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal tpi = mmAInches.divide(tornillo.getPasoRosca(), java.math.MathContext.DECIMAL64).stripTrailingZeros();
                ui.txtPaso.setText(tpi.toPlainString());
            }
        } else {
            if (tornillo.getDiametroMm() != null) ui.txtDiametro.setText(tornillo.getDiametroMm().toPlainString());
            if (tornillo.getLongitudMm() != null) ui.txtLongitud.setText(tornillo.getLongitudMm().toPlainString());
            if (tornillo.getPasoRosca()  != null) ui.txtPaso.setText(tornillo.getPasoRosca().toPlainString());
        }

        if (tornillo.getPrecioCosto() != null) ui.txtPrecioCosto.setText(tornillo.getPrecioCosto().toPlainString());
        if (tornillo.getPrecioVenta() != null) ui.txtPrecioVenta.setText(tornillo.getPrecioVenta().toPlainString());

        ui.txtStockInicial.setText(String.valueOf(tornillo.getStockActual()));
        ui.txtStockMin.setText(String.valueOf(tornillo.getStockMinimo()));
        ui.txtStockMax.setText(String.valueOf(tornillo.getStockMaximo()));
    }

    private String convertirDecimalAFraccion(java.math.BigDecimal valor) {
        if (valor == null) return "";

        int entero = valor.intValue();
        java.math.BigDecimal residuo = valor.subtract(new java.math.BigDecimal(entero));

        if (residuo.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return String.valueOf(entero);
        }

        int[] denominadoresComerciales = {2, 4, 8, 16, 32, 64};
        double valorDecimal = residuo.doubleValue();

        int mejorNumerador = 0;
        int mejorDenominador = 1;
        double menorError = 1.0;

        for (int d : denominadoresComerciales) {
            long n = Math.round(valorDecimal * d);
            double error = Math.abs(valorDecimal - ((double) n / d));
            
            if (error < menorError && error < 0.005) { 
                menorError = error;
                mejorNumerador = (int) n;
                mejorDenominador = d;
            }
        }

        if (mejorNumerador == 0) {
            return valor.stripTrailingZeros().toPlainString();
        }

        int mcd = calcularMCD(mejorNumerador, mejorDenominador);
        mejorNumerador /= mcd;
        mejorDenominador /= mcd;

        StringBuilder resultado = new StringBuilder();
        if (entero > 0) {
            resultado.append(entero).append(" ");
        }
        resultado.append(mejorNumerador).append("/").append(mejorDenominador);

        return resultado.toString();
    }

    private int calcularMCD(int a, int b) {
        return b == 0 ? a : calcularMCD(b, a % b);
    }

    private void procesarCierreSeguro() {
        if (ui.isFormularioModificado(tornillo)) {
            int opcion = JOptionPane.showConfirmDialog(
                this,
                "Hay cambios sin guardar en el formulario.\n¿Está seguro de que desea salir y perder los datos?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (opcion == JOptionPane.YES_OPTION) {
                dispose();
            }
        } else {
            dispose();
        }
    }

    // ── Guardar ──────────────────────────────────────────────────────────────

    private void guardar() {
        ui.resetBordes();
        Tornillo t = validator.validarYConstruir();
        if (t == null) return;

        try {
            tornilloService.guardarTornillo(t);
            guardado = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            ui.marcarError(ui.txtCodigo, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al guardar en BD:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── API pública ──────────────────────────────────────────────────────────

    public boolean isGuardado() {
        return this.guardado;
    }
}