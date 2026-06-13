package com.tornillos.ui.dialogs;

import java.math.BigDecimal;
import java.sql.SQLException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import com.tornillos.model.Categoria;
import com.tornillos.model.Tornillo;

class TornilloFormValidator {

    private final TornilloDialogUI ui;
    private final Tornillo tornilloOriginal;

    TornilloFormValidator(TornilloDialogUI ui, Tornillo tornilloOriginal) {
        this.ui               = ui;
        this.tornilloOriginal = tornilloOriginal;
    }

    /**
     * Valida todos los campos y, si son correctos, construye y devuelve el Tornillo.
     * Devuelve {@code null} si hay algún error de validación.
     */
    Tornillo validarYConstruir() {
        boolean trackingError = false;
        StringBuilder errores = new StringBuilder("Por favor, corrige los siguientes campos:\n\n");

        // ── 1. VALIDACIÓN DE OBLIGATORIOS ────────────────────────────────────────
        String codigo = ui.txtCodigo.getText().trim();
        String nombre = ui.txtNombre.getText().trim();

        if (codigo.isBlank()) {
            ui.marcarBordeError(ui.txtCodigo);
            errores.append("• El código es obligatorio.\n");
            trackingError = true;
        }
        if (nombre.isBlank()) {
            ui.marcarBordeError(ui.txtNombre);
            errores.append("• El nombre del tornillo es obligatorio.\n");
            trackingError = true;
        }

        // ── 2. VALIDACIÓN DE PRECIOS ─────────────────────────────────────────────
        BigDecimal precioCosto = parsePrecio(ui.txtPrecioCosto);
        if (precioCosto == null) {
            ui.marcarBordeError(ui.txtPrecioCosto);
            errores.append("• El precio de costo es inválido o menor a 0.\n");
            trackingError = true;
        }

        BigDecimal precioVenta = parsePrecio(ui.txtPrecioVenta);
        if (precioVenta == null) {
            ui.marcarBordeError(ui.txtPrecioVenta);
            errores.append("• El precio de venta es inválido o menor a 0.\n");
            trackingError = true;
        }

        if (precioCosto != null && precioVenta != null && precioVenta.compareTo(precioCosto) < 0) {
            ui.marcarBordeError(ui.txtPrecioVenta);
            errores.append("• El precio de venta no puede ser menor al precio de costo.\n");
            trackingError = true;
        }

        // ── 3. VALIDACIÓN DE STOCKS ──────────────────────────────────────────────
        Integer stockMin = parseStock(ui.txtStockMin);
        if (stockMin == null) {
            ui.marcarBordeError(ui.txtStockMin);
            errores.append("• El stock mínimo es inválido.\n");
            trackingError = true;
        }

        Integer stockMax = parseStock(ui.txtStockMax);
        if (stockMax == null) {
            ui.marcarBordeError(ui.txtStockMax);
            errores.append("• El stock máximo es inválido.\n");
            trackingError = true;
        }

        // Validación cruzada de stocks
        if (stockMin != null && stockMax != null && stockMax > 0 && stockMax < stockMin) {
            ui.marcarBordeError(ui.txtStockMax);
            errores.append("• El stock máximo no puede ser menor al stock mínimo.\n");
            trackingError = true;
        }

        int stockInicial = 0;
        if (tornilloOriginal == null) {
            Integer si = parseStock(ui.txtStockInicial);
            if (si == null) {
                ui.marcarBordeError(ui.txtStockInicial);
                errores.append("• El stock inicial es inválido.\n");
                trackingError = true;
            } else {
                stockInicial = si;
            }
        }

        // ── 4. VALIDACIÓN DE MEDIDAS OPCIONALES ──────────────────────────────────
        BigDecimal diametro = null;
        if (!ui.txtDiametro.getText().isBlank()) {
            try {
                diametro = parseMedida(ui.txtDiametro.getText());
                if (diametro.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ui.marcarBordeError(ui.txtDiametro);
                errores.append("• Diámetro inválido (ej. 8.0 o 1/4).\n");
                trackingError = true;
            }
        }

        BigDecimal longitud = null;
        if (!ui.txtLongitud.getText().isBlank()) {
            try {
                longitud = parseMedida(ui.txtLongitud.getText());
                if (longitud.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ui.marcarBordeError(ui.txtLongitud);
                errores.append("• Longitud inválida (ej. 30.0 o 1).\n");
                trackingError = true;
            }
        }

        // Nota: El paso de rosca usa directamente un new BigDecimal en tu código original
        BigDecimal pasoRosca = null;
        if (!ui.txtPaso.getText().isBlank()) {
            try {
                pasoRosca = new BigDecimal(ui.txtPaso.getText().trim());
                if (pasoRosca.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ui.marcarBordeError(ui.txtPaso);
                errores.append("• Paso de rosca o TPI inválido.\n");
                trackingError = true;
            }
        }

        // ── 5. EVALUACIÓN FINAL DE ERRORES ACUMULADOS ────────────────────────────
        if (trackingError) {
            // Lanza un único diálogo limpio con la lista detallada de errores
            JOptionPane.showMessageDialog(ui.getOwner(), errores.toString(), 
                    "Campos inválidos", JOptionPane.WARNING_MESSAGE);
            return null; // Detiene el flujo de guardado en TornilloDialog
        }

        // ── 6. CONSTRUCCIÓN DEL OBJETO (Si todo está correcto) ────────────────────
        Tornillo t = (tornilloOriginal != null) ? tornilloOriginal : new Tornillo();
        t.setCodigo(codigo);
        t.setNombre(nombre);
        t.setDescripcion(ui.txtDescripcion.getText().trim());
        t.setUbicacion(ui.txtUbicacion.getText().trim());

        Object sistemaSel = ui.cmbSistemaMedida.getSelectedItem();
        String sistemaTexto = (sistemaSel != null) ? sistemaSel.toString().toUpperCase() : "";
        t.setSistemaMedida(sistemaTexto.contains("IMP") ? "IMPERIAL" : "METRICO");

        int catIndex = ui.cmbCategoria.getSelectedIndex();
        Categoria catSel = (catIndex >= 0) ? ui.cmbCategoria.getItemAt(catIndex) : null;
        if (catSel != null && catSel.getId() > 0) t.setCategoriaId(catSel.getId());

        String material = (String) ui.cmbMaterial.getSelectedItem();
        t.setMaterial(material != null && !material.isBlank() ? material : null);

        String cabeza = (String) ui.cmbCabeza.getSelectedItem();
        t.setCabezaTipo(cabeza != null && !cabeza.isBlank() ? cabeza : null);

        String unidad = (String) ui.cmbUnidad.getSelectedItem();
        t.setUnidadMedida(unidad != null && !unidad.isBlank() ? unidad : "PZA");

        t.setDiametroMm(diametro);
        t.setLongitudMm(longitud);
        t.setPasoRosca(pasoRosca);
        t.setPrecioCosto(precioCosto);
        t.setPrecioVenta(precioVenta);
        t.setStockMinimo(stockMin);
        t.setStockMaximo(stockMax);
        if (tornilloOriginal == null) t.setStockActual(stockInicial);

        return t;
    }

    // ── Helpers de parseo ────────────────────────────────────────────────────

    /** Parsea un campo de precio; devuelve {@code null} y marca error si es inválido. */
    private BigDecimal parsePrecio(JComponent campo) {
        try {
            String raw = ((javax.swing.JTextField) campo).getText().trim();
            BigDecimal valor = new BigDecimal(raw.isEmpty() ? "0" : raw);
            if (valor.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parsea un campo de stock entero; devuelve {@code null} y marca error si es inválido. */
    private Integer parseStock(JComponent campo) {
        try {
            String raw = ((javax.swing.JTextField) campo).getText().trim();
            int valor = Integer.parseInt(raw.isEmpty() ? "0" : raw);
            if (valor < 0) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Convierte texto a BigDecimal soportando fracciones ("1/4") y números mixtos ("1 1/2").
     */
    private BigDecimal parseMedida(String text) throws NumberFormatException {
        text = text.trim().replaceAll("\\s+", " ");
        if (text.contains("/")) {
            if (text.contains(" ")) {
                String[] partesMixtas = text.split(" ");
                if (partesMixtas.length == 2) {
                    double entero      = Double.parseDouble(partesMixtas[0]);
                    String[] fraccion  = partesMixtas[1].split("/");
                    double numerador   = Double.parseDouble(fraccion[0]);
                    double denominador = Double.parseDouble(fraccion[1]);
                    if (denominador == 0) throw new NumberFormatException();
                    return BigDecimal.valueOf(entero + (numerador / denominador));
                }
            }
            String[] partes = text.split("/");
            if (partes.length == 2) {
                double numerador   = Double.parseDouble(partes[0]);
                double denominador = Double.parseDouble(partes[1]);
                if (denominador == 0) throw new NumberFormatException();
                return BigDecimal.valueOf(numerador / denominador);
            }
        }
        return new BigDecimal(text);
    }

    private void marcarError(JComponent campo, String mensaje) {
        ui.marcarError(campo, mensaje);
    }
}