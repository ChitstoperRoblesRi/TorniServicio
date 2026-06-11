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
            Map<String, String> config = confDAO.obtenerTodas();
            boolean emailActivo = "true".equalsIgnoreCase(
                    config.getOrDefault("alertas_email_activo", "false"));

            for (Tornillo tornillo : stockBajo) {
                String tipo = determinarTipoAlerta(tornillo);
                String mensaje = determinarMensajeAlerta(tornillo, tipo);

                Alerta alerta = new Alerta(tornillo.getId(), tornillo.getNombre(), tipo, mensaje);
                alerta.setTornilloCodigo(tornillo.getCodigo());

                alertaDAO.crear(alerta);

                if (emailActivo) {
                    Alerta existente = alertaDAO.buscarNoEnviada(tornillo.getId(), tipo);
                    if (existente != null) {
                        enviarEmail(existente, config);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error verificando alertas: " + e.getMessage());
        }
    }

    private String determinarTipoAlerta(Tornillo tornillo) {
        if (tornillo.getStockActual() == 0) return "SIN_STOCK";
        if (tornillo.getStockActual() <= tornillo.getStockMinimo() / 2) return "STOCK_CRITICO";
        return "STOCK_BAJO";
    }

    private String determinarMensajeAlerta(Tornillo tornillo, String tipo) {
        switch (tipo) {
            case "SIN_STOCK":
                return "SIN STOCK: " + tornillo.getNombre() + " (" + tornillo.getCodigo()
                        + ") — Stock: 0 unidades";
            case "STOCK_CRITICO":
                return "STOCK CRITICO: " + tornillo.getNombre() + " (" + tornillo.getCodigo()
                        + ") — Stock: " + tornillo.getStockActual()
                        + " | Minimo: " + tornillo.getStockMinimo();
            default:
                return "STOCK BAJO: " + tornillo.getNombre() + " (" + tornillo.getCodigo()
                        + ") — Stock: " + tornillo.getStockActual()
                        + " | Minimo: " + tornillo.getStockMinimo();
        }
    }

    /**
     * Reenvía por email todas las alertas no leídas que aún no han sido enviadas.
     * Llamado por el scheduler diario del panel de alertas.
     */
    public void reenviarNoLeidasPorEmail() {
        try {
            Map<String, String> config = confDAO.obtenerTodas();
            boolean emailActivo = "true".equalsIgnoreCase(
                    config.getOrDefault("alertas_email_activo", "false"));
            if (!emailActivo)
                return;

            List<Alerta> pendientes = alertaDAO.listarNoEnviadas();
            for (Alerta alerta : pendientes) {
                enviarEmail(alerta, config);
            }
        } catch (SQLException e) {
            LOG.warning("Error reenviando alertas: " + e.getMessage());
        }
    }

    // ── Métodos de consulta (delegación a DAO) ────────────────
    public int contarActivas() throws SQLException {
        return alertaDAO.contarActivas();
    }

    public List<Alerta> listarActivas() throws SQLException {
        return alertaDAO.listarActivas();
    }

    public List<Alerta> listarHistorial() throws SQLException {
        return alertaDAO.listarHistorial();
    }

    public List<Alerta> buscar(String termino) throws SQLException {
        return alertaDAO.buscar(termino);
    }

    public boolean eliminar(int id) throws SQLException {
        return alertaDAO.eliminar(id);
    }

    public boolean eliminarTodas() throws SQLException {
        return alertaDAO.eliminarTodas();
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
