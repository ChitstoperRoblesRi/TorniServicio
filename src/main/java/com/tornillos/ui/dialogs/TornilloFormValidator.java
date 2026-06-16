package com.tornillos.ui.dialogs;

import java.math.BigDecimal;

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
        String textStockMin = ui.txtStockMin.getText().trim();
        Integer stockMin = parseStock(ui.txtStockMin);
        
        if (textStockMin.isEmpty() || stockMin == null) {
            ui.marcarBordeError(ui.txtStockMin);
            errores.append("• El stock mínimo es obligatorio y debe ser un número entero mayor o igual a 0.\n");
            trackingError = true;
        }

        Integer stockMax = null;
        String textStockMax = ui.txtStockMax.getText().trim();
        if (!textStockMax.isEmpty()) {
            stockMax = parseStock(ui.txtStockMax);
            if (stockMax == null) {
                ui.marcarBordeError(ui.txtStockMax);
                errores.append("• El stock máximo debe ser un número entero válido.\n");
                trackingError = true;
            }
        }

        if (stockMin != null && stockMax != null && stockMax < stockMin) {
            ui.marcarBordeError(ui.txtStockMax);
            errores.append("• El stock máximo no puede ser menor al stock mínimo.\n");
            trackingError = true;
        }

        // =========================================================================
        // NUEVA VALIDACIÓN (Modo Edición): Evitar reducción del máximo inferior al stock actual
        // =========================================================================
        if (tornilloOriginal != null && stockMax != null && stockMax < tornilloOriginal.getStockActual()) {
            ui.marcarBordeError(ui.txtStockMax);
            errores.append("• El stock máximo no puede ser menor a la cantidad actual en almacén (Stock actual: ")
                   .append(tornilloOriginal.getStockActual()).append(").\n");
            trackingError = true;
        }

        int stockInicial = 0;
        if (tornilloOriginal == null) { // Esto solo aplica al crear un tornillo nuevo
            String raw = ui.txtStockInicial.getText().trim();
            
            if (!raw.isEmpty()) { 
                Integer si = parseStock(ui.txtStockInicial);
                if (si == null) {
                    ui.marcarBordeError(ui.txtStockInicial);
                    errores.append("• El stock inicial es inválido.\n");
                    trackingError = true;
                } else {
                    stockInicial = si;
                }
            } else {
                stockInicial = 0; 
            }

            // Evitar que el stock inicial supere al stock máximo al crear
            if (stockMax != null && stockInicial > stockMax) {
                ui.marcarBordeError(ui.txtStockInicial);
                errores.append("• El stock inicial no puede ser mayor al stock máximo permitido.\n");
                trackingError = true;
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
            JOptionPane.showMessageDialog(ui.getOwner(), errores.toString(), 
                    "Campos inválidos", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        // ── 6. CONSTRUCCIÓN DEL OBJETO (Si todo está correcto) ────────────────────
        Tornillo t = (tornilloOriginal != null) ? tornilloOriginal : new Tornillo();
        t.setCodigo(codigo);
        t.setNombre(nombre);
        t.setDescripcion(ui.txtDescripcion.getText().trim());
        t.setUbicacion(ui.txtUbicacion.getText().trim());

        Object sistemaSel = ui.cmbSistemaMedida.getSelectedItem();
        String sistemaTexto = (sistemaSel != null) ? sistemaSel.toString().toUpperCase() : "";
        boolean esImperial = sistemaTexto.contains("IMP");
        t.setSistemaMedida(esImperial ? "IMPERIAL" : "METRICO");

        int catIndex = ui.cmbCategoria.getSelectedIndex();
        Categoria catSel = (catIndex >= 0) ? ui.cmbCategoria.getItemAt(catIndex) : null;
        if (catSel != null && catSel.getId() > 0) t.setCategoriaId(catSel.getId());

        String material = (String) ui.cmbMaterial.getSelectedItem();
        t.setMaterial(material != null && !material.isBlank() ? material : null);

        String cabeza = (String) ui.cmbCabeza.getSelectedItem();
        t.setCabezaTipo(cabeza != null && !cabeza.isBlank() ? cabeza : null);

        String unidad = (String) ui.cmbUnidad.getSelectedItem();
        t.setUnidadMedida(unidad != null && !unidad.isBlank() ? unidad : "PZA");

        if (esImperial) {
        if (diametro != null) {
            diametro = diametro.multiply(new BigDecimal("25.4"));
        }
        if (longitud != null) {
            longitud = longitud.multiply(new BigDecimal("25.4"));
        }
        if (pasoRosca != null && pasoRosca.compareTo(BigDecimal.ZERO) > 0) {
            pasoRosca = new BigDecimal("25.4").divide(pasoRosca, java.math.MathContext.DECIMAL64);
        }
    }

        t.setDiametroMm(diametro);
        t.setLongitudMm(longitud);
        t.setPasoRosca(pasoRosca);
        t.setPrecioCosto(precioCosto);
        t.setPrecioVenta(precioVenta);
        t.setStockMinimo(stockMin);
        
        // 🌟 CORREGIDO: Si stockMax es null (vacío), le pasamos un 0 de forma segura al primitivo
        t.setStockMaximo(stockMax != null ? stockMax : 0); 
        
        if (tornilloOriginal == null) t.setStockActual(stockInicial);

        return t;
    }

    // ── Helpers de parseo ────────────────────────────────────────────────────

    private BigDecimal parsePrecio(JComponent campo) {
        try {
            String raw = ((javax.swing.JTextField) campo).getText().trim();
            if (raw.isEmpty()) {
                return null;
            }
            BigDecimal valor = new BigDecimal(raw);
            if (valor.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseStock(JComponent campo) {
        try {
            String raw = ((javax.swing.JTextField) campo).getText().trim();
            if (raw.isEmpty()) {
                return null;
            }
            int valor = Integer.parseInt(raw);
            if (valor < 0) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseMedida(String text) throws NumberFormatException {
        text = text.trim().replaceAll("\\s+", " ");
        if (text.contains("/")) {
            if (text.contains(" ")) {
                String[] partesMixtas = text.split(" ");
                if (partesMixtas.length == 2) {
                    BigDecimal entero = new BigDecimal(partesMixtas[0]);
                    String[] fraccion = partesMixtas[1].split("/");
                    BigDecimal numerador = new BigDecimal(fraccion[0]);
                    BigDecimal denominador = new BigDecimal(fraccion[1]);
                    
                    if (denominador.compareTo(BigDecimal.ZERO) == 0) throw new NumberFormatException();
                    
                    BigDecimal parteFraccionaria = numerador.divide(denominador, java.math.MathContext.DECIMAL64);
                    return entero.add(parteFraccionaria);
                }
            }
            String[] partes = text.split("/");
            if (partes.length == 2) {
                BigDecimal numerador = new BigDecimal(partes[0]);
                BigDecimal denominador = new BigDecimal(partes[1]);
                
                if (denominador.compareTo(BigDecimal.ZERO) == 0) throw new NumberFormatException();
                
                return numerador.divide(denominador, java.math.MathContext.DECIMAL64);
            }
        }
        return new BigDecimal(text);
    }

    private void marcarError(JComponent campo, String mensaje) {
        ui.marcarError(campo, mensaje);
    }
}