package com.tornillos.service;

import com.tornillos.dao.AlertaDAO;
import com.tornillos.dao.ConfiguracionDAO;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.model.Alerta;
import com.tornillos.model.Tornillo;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.SQLException;
// import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class AlertaService {

    private static final Logger LOG = Logger.getLogger(AlertaService.class.getName());

    private final AlertaDAO alertaDAO = new AlertaDAO();
    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final ConfiguracionDAO confDAO = new ConfiguracionDAO();

    // ── Métodos puente para AlertasPanel ──
    
    public List<Alerta> obtenerAlertasActivas() throws SQLException {
        return alertaDAO.listarActivas();
    }

    public List<Alerta> buscarAlertas(String criterio) throws SQLException {
        if (criterio == null || criterio.trim().isEmpty()) {
            return obtenerAlertasActivas();
        }
        return alertaDAO.buscar(criterio.trim());
    }

    public List<Alerta> obtenerHistorial(String desde, String hasta, String criterio) throws Exception {
        return alertaDAO.listarHistorial(desde, hasta, criterio); // Suponiendo que tienes la instancia ahí
    }

    public List<Alerta> obtenerHistorialPorTornillo(int tornilloId) throws SQLException {
        return alertaDAO.listarHistorialPorTornillo(tornilloId);
    }

    public Map<String, String> obtenerTodasLasConfiguraciones() throws SQLException {
        return confDAO.obtainAll();
    }

    public void guardarConfiguracion(String clave, String valor) throws SQLException {
        confDAO.guardar(clave, valor);
    }

    /**
     * Sobrecarga agregada para procesar la llamada basada en mapas desde ConfigPanel.
     * Retorna null si el correo se envió con éxito o el mensaje de error si falló.
     */
    public String probarConexion(Map<String, String> conf) {
        try {
            String host = conf.get("smtp_host");
            String port = conf.get("smtp_port");
            String user = conf.get("smtp_user");
            String pass = conf.get("smtp_password");
            String dest = conf.get("alertas_email_destino");

            Properties prop = new Properties();
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.port", port);
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.timeout", "5000");
            prop.put("mail.smtp.connectiontimeout", "5000");

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(dest));
            message.setSubject("Prueba de Conexión SMTP - " + conf.get("empresa_nombre"));
            message.setText("Si recibes este correo, la configuración de alertas por email funciona correctamente.");
            
            Transport.send(message);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // ── Verificación principal e Email ──
    public void verificarAlertas() {
        try {
            List<Tornillo> stockBajo = tornilloDAO.listarConStockBajo();
            Map<String, String> conf = confDAO.obtainAll();
            boolean emailActivo = "true".equalsIgnoreCase(conf.getOrDefault("alertas_email_activo", "false"));

            for (Tornillo t : stockBajo) {
                String tipo = "STOCK_BAJO";
                if (t.getStockActual() == 0) {
                    tipo = "SIN_STOCK";
                } else if (t.getStockActual() <= t.getStockMinimo() * 0.5) {
                    tipo = "STOCK_CRITICO";
                }

                String msg = "El tornillo " + t.getNombre() + " (" + t.getCodigo() + ") está en " + tipo 
                           + ". Stock actual: " + t.getStockActual() + ", Mínimo: " + t.getStockMinimo();

                Alerta alerta = new Alerta(t.getId(), t.getNombre(), tipo, msg);
                alertaDAO.crear(alerta);

                if (emailActivo) {
                    Alerta existente = alertaDAO.buscarNoEnviada(t.getId(), tipo);
                    if (existente != null) {
                        boolean enviado = enviarCorreoAlerta(existente, conf, t.getNombre(), t.getCodigo());
                        if (enviado) {
                            alertaDAO.marcarComoEnviada(existente.getId());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.severe("Error al verificar alertas en segundo plano: " + e.getMessage());
        }
    }

    private boolean enviarCorreoAlerta(Alerta alerta, Map<String, String> conf, String nombre, String codigo) {
        String host = conf.get("smtp_host");
        String port = conf.get("smtp_port");
        String user = conf.get("smtp_user");
        String pass = conf.get("smtp_password");
        String dest = conf.get("alertas_email_destino");
        String empresa = conf.getOrDefault("empresa_nombre", "Sistema Tornillos");

        if (host == null || port == null || user == null || pass == null || dest == null) {
            LOG.warning("Configuración SMTP incompleta. No se puede enviar el correo.");
            return false;
        }

        Properties prop = new Properties();
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(dest));
            message.setSubject("[" + alerta.getTipo() + "] Alerta de Inventario - " + nombre);

            String cuerpoHtml = generarContenidoHtml(alerta.getTipo(), empresa, nombre, codigo, alerta.getMensaje());
            message.setContent(cuerpoHtml, "text/html; charset=utf-8");

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            LOG.severe("Error al enviar email de alerta: " + e.getMessage());
            return false;
        }
    }

    private String generarContenidoHtml(String tipo, String empresa, String nombre, String codigo, String mensaje) {
        String color;
        if ("SIN_STOCK".equals(tipo)) {
            color = "#EF4444";
        } else if ("STOCK_CRITICO".equals(tipo)) {
            color = "#FF6B35";
        } else {
            color = "#FFB347";
        }

        return "<html><body style='font-family:Segoe UI,Arial,sans-serif; background:#0F1117; color:#F0F2FF; padding:32px; margin:0'>"
                + "<div style='max-width:520px; margin:0 auto; background:#1A1D27; border-radius:16px; padding:32px; border:1px solid #2D3350'>"
                + "<h2 style='color:" + color + "; margin-top:0; font-size:20px'>" + tipo.replace("_", " ") + "</h2>"
                + "<table style='width:100%; border-collapse:collapse'>"
                + "<tr><td style='padding:6px 0; color:#8892B0'>Empresa:</td><td style='padding:6px 0'>" + empresa + "</td></tr>"
                + "<tr><td style='padding:6px 0; color:#8892B0'>Producto:</td><td style='padding:6px 0'>" + nombre + " (" + codigo + ")</td></tr>"
                + "</table>"
                + "<p style='margin:16px 0; padding:12px; background:#0F1117; border-left:4px solid " + color + "; border-radius:4px'>" + mensaje + "</p>"
                + "<hr style='border:0; border-top:1px solid #2D3350; margin:24px 0'>"
                + "<p style='font-size:12px; color:#8892B0; margin:0; text-align:center'>Este es un correo automático, por favor no responder.</p>"
                + "</div></body></html>";
    }
}