package com.tornillos.service;

import com.tornillos.dao.AlertaDAO;
import com.tornillos.dao.ConfiguracionDAO;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Alerta;
import com.tornillos.model.Tornillo;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class AlertaService {

    private static final Logger LOG = Logger.getLogger(AlertaService.class.getName());

    private final AlertaDAO alertaDAO = new AlertaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final ConfiguracionDAO confDAO = new ConfiguracionDAO();

    // ── Verificación principal ────────────────────────────────
    public void verificarAlertas() {
        try {
            List<Tornillo> stockBajo = tornilloDAO.listarConStockBajo();
            Map<String, String> conf = confDAO.obtenerTodas();
            boolean emailActivo = "true".equalsIgnoreCase(
                    conf.getOrDefault("alertas_email_activo", "false"));

            for (Tornillo t : stockBajo) {
                String tipo;
                String msg;

                if (t.getStockActual() == 0) {
                    tipo = "SIN_STOCK";
                    msg = "SIN STOCK: " + t.getNombre() + " (" + t.getCodigo()
                            + ") — Stock: 0 unidades";
                } else if (t.getStockActual() <= t.getStockMinimo() / 2) {
                    tipo = "STOCK_CRITICO";
                    msg = "STOCK CRITICO: " + t.getNombre() + " (" + t.getCodigo()
                            + ") — Stock: " + t.getStockActual()
                            + " | Minimo: " + t.getStockMinimo();
                } else {
                    tipo = "STOCK_BAJO";
                    msg = "STOCK BAJO: " + t.getNombre() + " (" + t.getCodigo()
                            + ") — Stock: " + t.getStockActual()
                            + " | Minimo: " + t.getStockMinimo();
                }

                Alerta alerta = new Alerta(t.getId(), t.getNombre(), tipo, msg);
                alerta.setTornilloCodigo(t.getCodigo());

                // crear() ya evita duplicados; si ya existe, no inserta
                alertaDAO.crear(alerta);

                // Solo enviar email si la alerta es nueva (no enviada aún)
                if (emailActivo) {
                    Alerta existente = alertaDAO.buscarNoEnviada(t.getId(), tipo);
                    if (existente != null) {
                        enviarEmail(existente, conf);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error verificando alertas: " + e.getMessage());
        }
    }

    /**
     * Reenvía por email todas las alertas no leídas que aún no han sido enviadas.
     * Llamado por el scheduler diario del panel de alertas.
     */
    public void reenviarNoLeidasPorEmail() {
        try {
            Map<String, String> conf = confDAO.obtenerTodas();
            boolean emailActivo = "true".equalsIgnoreCase(
                    conf.getOrDefault("alertas_email_activo", "false"));
            if (!emailActivo)
                return;

            List<Alerta> pendientes = alertaDAO.listarNoEnviadas();
            for (Alerta a : pendientes) {
                enviarEmail(a, conf);
            }
        } catch (SQLException e) {
            LOG.warning("Error reenviando alertas: " + e.getMessage());
        }
    }

    // ── Envío de email ────────────────────────────────────────
    private void enviarEmail(Alerta alerta, Map<String, String> conf) {
        try {
            enviarEmailInternal(alerta.getTipo(), alerta.getTornilloNombre(), alerta.getMensaje(), conf);
            alertaDAO.marcarEnviadaEmail(alerta.getId());
            LOG.info("Email de alerta enviado: " + alerta.getTornilloNombre());
        } catch (Exception e) {
            LOG.warning("Error enviando alerta: " + e.getMessage());
        }
    }

    /**
     * Envía un email de prueba con la configuración SMTP proporcionada.
     * @return null si éxito, o mensaje de error si falla
     */
    public String probarConexion(Map<String, String> conf) {
        try {
            enviarEmailInternal("PRUEBA", "Test de conexión",
                    "Este es un correo de prueba del sistema de inventario.", conf);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private void enviarEmailInternal(String tipo, String nombre, String mensaje, Map<String, String> conf)
            throws MessagingException {
        String host = conf.getOrDefault("smtp_host", "");
        String port = conf.getOrDefault("smtp_port", "587");
        String user = conf.getOrDefault("smtp_user", "");
        String pass = conf.getOrDefault("smtp_password", "");
        String destino = conf.getOrDefault("alertas_email_destino", "");
        String empresa = conf.getOrDefault("empresa_nombre", "Sistema de Inventario");

        if (host.isEmpty() || user.isEmpty() || pass.isEmpty() || destino.isEmpty()) {
            throw new MessagingException(
                    "Configuración SMTP incompleta.\nVerifica: Host, Usuario, Contraseña y Destino.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        javax.mail.Session session = javax.mail.Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(user, empresa, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new MessagingException("Error codificando remitente: " + e.getMessage());
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
        message.setSubject("[ALERTA] " + tipo + " - " + nombre, "UTF-8");
        message.setContent(buildEmailHtml(tipo, nombre, mensaje, empresa), "text/html; charset=UTF-8");

        Transport.send(message);
    }

    // ── HTML del email ────────────────────────────────────────
    private String buildEmailHtml(String tipo, String nombre, String mensaje, String empresa) {
        String color;
        if ("SIN_STOCK".equals(tipo)) {
            color = "#FF4D6A";
        } else if ("STOCK_CRITICO".equals(tipo)) {
            color = "#FF6B35";
        } else {
            color = "#FFB347";
        }

        return "<html><body style='font-family:Segoe UI,Arial,sans-serif;"
                + "background:#0F1117;color:#F0F2FF;padding:32px;margin:0'>"
                + "<div style='max-width:520px;margin:0 auto;background:#1A1D27;"
                + "border-radius:16px;padding:32px;border:1px solid #2D3350'>"
                + "<h2 style='color:" + color + ";margin-top:0;font-size:20px'>"
                + tipo.replace("_", " ") + "</h2>"
                + "<table style='width:100%;border-collapse:collapse'>"
                + "<tr><td style='padding:6px 0;color:#8892B0'>Empresa:</td>"
                + "<td style='padding:6px 0'>" + empresa + "</td></tr>"
                + "<tr><td style='padding:6px 0;color:#8892B0'>Producto:</td>"
                + "<td style='padding:6px 0'>" + nombre + "</td></tr>"
                + "</table>"
                + "<p style='margin:16px 0;padding:12px;background:#0F1117;"
                + "border-radius:8px;border-left:3px solid " + color + "'>"
                + mensaje + "</p>"
                + "<hr style='border:none;border-top:1px solid #2D3350;margin:20px 0'>"
                + "<p style='color:#4A5568;font-size:11px;margin:0'>"
                + "Mensaje generado automaticamente por el Sistema de Inventario TorniServicio.</p>"
                + "</div></body></html>";
    }
}
