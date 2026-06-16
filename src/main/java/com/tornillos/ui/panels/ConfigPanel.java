package com.tornillos.ui.panels;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.ConfiguracionDAO;
import com.tornillos.service.AlertaService;
import com.tornillos.service.SessionManager;

public class ConfigPanel extends JPanel {

    private final ConfiguracionDAO confDAO = new ConfiguracionDAO();
    private final AlertaService alertaService = new AlertaService();
    private JButton btnProbar;
    private JTextField txtEmpresa, txtRfc, txtEmail;
    private JTextField txtSmtpHost, txtSmtpPort, txtSmtpUser, txtSmtpDest;
    private JPasswordField txtSmtpPass;
    private JCheckBox chkEmailActivo;

    public ConfigPanel() {
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        if (!SessionManager.getInstance().isGerente()) {
            JLabel l = new JLabel("Acceso restringido a Gerentes.", SwingConstants.CENTER);
            l.setFont(AppTheme.FONT_HEADING);
            l.setForeground(AppTheme.DANGER_TEXT);
            add(l, BorderLayout.CENTER);
            return;
        }
        buildUI();
    }

    // ─── MEJORA 2 y 4: GridBagLayout proporcional + botón "Deshacer cambios" ───
    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(AppTheme.BG_SURFACE);
        main.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel title = new JLabel("Configuración del sistema");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.GOLD_LIGHT);
        hdr.add(title, BorderLayout.WEST);
        main.add(hdr, BorderLayout.NORTH);

        // MEJORA 2: GridBagLayout evita que la tarjeta izquierda estire sus
        // inputs desproporcionadamente en pantallas anchas (antes era GridLayout(1,2))
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);
        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.fill = GridBagConstraints.BOTH;
        cGbc.weighty = 1.0;
        cGbc.insets = new Insets(0, 0, 0, 16);

        cGbc.gridx = 0;
        cGbc.weightx = 0.45; // Menos peso a la tarjeta de empresa (3 campos)
        cards.add(buildEmpresaCard(), cGbc);

        cGbc.gridx = 1;
        cGbc.weightx = 0.55; // Más peso a la tarjeta SMTP (6 campos + botón)
        cGbc.insets = new Insets(0, 0, 0, 0);
        cards.add(buildEmailCard(), cGbc);

        main.add(cards, BorderLayout.CENTER);

        // MEJORA 4: Fila de botones con opción de deshacer cambios no guardados
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);

        JButton btnRestablecer = AppTheme.secondaryButton("Deshacer cambios");
        btnRestablecer.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this,
                    "¿Deseas descartar las modificaciones actuales y restaurar los valores guardados?",
                    "Deshacer cambios", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                cargarConfig(); // Recarga los datos originales desde la base de datos
            }
        });

        JButton btnGuardar = AppTheme.goldButton("Guardar configuración");
        btnGuardar.addActionListener(e -> guardar());

        btns.add(btnRestablecer);
        btns.add(btnGuardar);
        main.add(btns, BorderLayout.SOUTH);

        add(main, BorderLayout.CENTER);
        cargarConfig();
    }

    private JPanel buildEmpresaCard() {
        JPanel card = buildCard("Datos de la empresa");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = gbc();
        
        txtEmpresa = AppTheme.styledField("");
        txtRfc = AppTheme.styledField("");
        txtEmail = AppTheme.styledField("");

        // 🌟 NUEVO: Filtro que fuerza mayúsculas automáticas en el RFC y limita a 13 caracteres máximos
        ((AbstractDocument) txtRfc.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string != null) string = string.toUpperCase();
                super.insertString(fb, offset, string, attr);
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text != null) text = text.toUpperCase();
                String contenidoActual = fb.getDocument().getText(0, fb.getDocument().getLength());
                String resultado = contenidoActual.substring(0, offset) + (text != null ? text : "") + contenidoActual.substring(offset + length);
                if (resultado.length() <= 13) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        // 🌟 CORREGIDO: Indicador visual '*' de campos obligatorios para guiar al usuario
        addRow(form, gbc, 0, "Nombre *:", txtEmpresa);
        addRow(form, gbc, 1, "RFC *:", txtRfc);
        addRow(form, gbc, 2, "Email corporativo *:", txtEmail);
        
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ─── MEJORA 1 y 3: Listener reactivo en checkbox + filtro numérico en puerto ───
    private JPanel buildEmailCard() {
        JPanel card = buildCard("Alertas por correo electrónico (SMTP)");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = gbc();

        chkEmailActivo = new JCheckBox("Activar envío de alertas por email");
        chkEmailActivo.setFont(AppTheme.FONT_BODY);
        chkEmailActivo.setForeground(AppTheme.TEXT_PRIMARY);
        chkEmailActivo.setOpaque(false);

        // MEJORA 1: Habilita o deshabilita los campos SMTP al instante según el checkbox
        chkEmailActivo.addActionListener(e -> toggleCamposSmtp(chkEmailActivo.isSelected()));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(chkEmailActivo, gbc);

        txtSmtpHost = AppTheme.styledField("smtp.gmail.com");
        txtSmtpPort = AppTheme.styledField("587");
        txtSmtpUser = AppTheme.styledField("tu@gmail.com");
        txtSmtpPass = AppTheme.styledPasswordField();
        txtSmtpDest = AppTheme.styledField("destino@empresa.com");

        // MEJORA 3: Filtro que bloquea letras en el campo Puerto en tiempo real
        // y limita la entrada a 5 dígitos (máximo válido para un puerto TCP: 65535)
        ((AbstractDocument) txtSmtpPort.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String contenidoActual = fb.getDocument().getText(0, fb.getDocument().getLength());
                // Reconstruye el texto resultante para validar longitud total
                String resultado = contenidoActual.substring(0, offset)
                        + (text != null ? text : "")
                        + contenidoActual.substring(offset + length);
                // Solo acepta dígitos y no supera 5 caracteres
                if (resultado.matches("\\d{0,5}")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        addRow(form, gbc, 1, "SMTP Host:", txtSmtpHost);
        addRow(form, gbc, 2, "Puerto:", txtSmtpPort);
        addRow(form, gbc, 3, "Usuario:", txtSmtpUser);
        addRow(form, gbc, 4, "Contraseña:", txtSmtpPass);
        addRow(form, gbc, 5, "Destino:", txtSmtpDest);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 5, 5, 5);

        btnProbar = AppTheme.secondaryButton("Probar conexión SMTP");
        btnProbar.addActionListener(e -> probarConexion());
        form.add(btnProbar, gbc);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel hdr = new JPanel(new BorderLayout(0, 6));
        hdr.setOpaque(false);
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(AppTheme.FONT_LABEL);
        lbl.setForeground(AppTheme.TEXT_MUTED);
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.BORDER_SUBTLE);
        hdr.add(lbl, BorderLayout.NORTH);
        hdr.add(sep, BorderLayout.SOUTH);
        card.add(hdr, BorderLayout.NORTH);
        return card;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 5, 5, 5);
        return g;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.65;
        p.add(field, gbc);
    }

    // ─── MEJORA 1: Sincroniza el estado visual al cargar y al cambiar el checkbox ───
    private void cargarConfig() {
        try {
            Map<String, String> conf = confDAO.obtainAll();
            txtEmpresa.setText(conf.getOrDefault("empresa_nombre", ""));
            txtRfc.setText(conf.getOrDefault("empresa_rfc", ""));
            txtEmail.setText(conf.getOrDefault("empresa_email", ""));
            txtSmtpHost.setText(conf.getOrDefault("smtp_host", "smtp.gmail.com"));
            txtSmtpPort.setText(conf.getOrDefault("smtp_port", "587"));
            txtSmtpUser.setText(conf.getOrDefault("smtp_user", ""));
            txtSmtpPass.setText(conf.getOrDefault("smtp_password", ""));
            txtSmtpDest.setText(conf.getOrDefault("alertas_email_destino", ""));
            chkEmailActivo.setSelected("true".equalsIgnoreCase(
                    conf.getOrDefault("alertas_email_activo", "false")));

            // MEJORA 1: Sincroniza el estado habilitado/deshabilitado al cargar
            toggleCamposSmtp(chkEmailActivo.isSelected());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando config: " + e.getMessage());
        }
    }

    // MEJORA 1: Bloquea o libera los controles SMTP según el estado del checkbox
    private void toggleCamposSmtp(boolean activo) {
        txtSmtpHost.setEnabled(activo);
        txtSmtpPort.setEnabled(activo);
        txtSmtpUser.setEnabled(activo);
        txtSmtpPass.setEnabled(activo);
        txtSmtpDest.setEnabled(activo);
        btnProbar.setEnabled(activo);
    }

    private void probarConexion() {
        Map<String, String> conf = new java.util.HashMap<>();
        conf.put("smtp_host", txtSmtpHost.getText().trim());
        conf.put("smtp_port", txtSmtpPort.getText().trim());
        conf.put("smtp_user", txtSmtpUser.getText().trim());
        conf.put("smtp_password", new String(txtSmtpPass.getPassword()));
        conf.put("alertas_email_destino", txtSmtpDest.getText().trim());
        conf.put("empresa_nombre", txtEmpresa.getText().trim().isEmpty()
                ? "Sistema de Inventario" : txtEmpresa.getText().trim());

        btnProbar.setEnabled(false);
        btnProbar.setText("Enviando...");
        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return alertaService.probarConexion(conf);
            }

            @Override
            protected void done() {
                try {
                    String error = get();
                    if (error == null) {
                        JOptionPane.showMessageDialog(ConfigPanel.this,
                                "Correo de prueba enviado correctamente.\n"
                                + "Revisa la bandeja de entrada de: " + txtSmtpDest.getText().trim(),
                                "Conexión exitosa", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ConfigPanel.this,
                                "Error al enviar correo de prueba:\n" + error,
                                "Error de conexión", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ConfigPanel.this,
                            "Error inesperado: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Restaura el botón respetando el estado actual del checkbox
                    btnProbar.setEnabled(chkEmailActivo.isSelected());
                    btnProbar.setText("Probar conexión SMTP");
                }
            }
        };
        w.execute();
    }

    private void guardar() {
        // 1. Sanitización e inyección de variables limpias
        String nombre = txtEmpresa.getText().trim();
        String rfc = txtRfc.getText().trim();
        String emailCorp = txtEmail.getText().trim();

        // 2. Validación estricta de datos de la Empresa
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de la empresa es un campo obligatorio.", "Validación de datos", JOptionPane.WARNING_MESSAGE);
            txtEmpresa.requestFocus();
            return;
        }
        if (rfc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El RFC de la empresa es obligatorio para la emisión de comprobantes.", "Validación de datos", JOptionPane.WARNING_MESSAGE);
            txtRfc.requestFocus();
            return;
        }
        // Expresión regular oficial para validar estructura de RFC Mexicano (Físico o Moral)
        if (!rfc.matches("^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$")) {
            JOptionPane.showMessageDialog(this, "El formato del RFC no es válido.\nRecuerde la estructura oficial (Ej: VECM051219XXX).", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
            txtRfc.requestFocus();
            return;
        }
        if (emailCorp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El email corporativo es requerido para las notificaciones del sistema.", "Validación de datos", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }
        // Expresión regular estándar para correo electrónico
        if (!emailCorp.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "El formato del email corporativo no es válido.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        // 3. Validación condicional del bloque SMTP (Solo si las alertas por correo están activas)
        if (chkEmailActivo.isSelected()) {
            String host = txtSmtpHost.getText().trim();
            String port = txtSmtpPort.getText().trim();
            String user = txtSmtpUser.getText().trim();
            String pass = new String(txtSmtpPass.getPassword());
            String dest = txtSmtpDest.getText().trim();

            if (host.isEmpty() || port.isEmpty() || user.isEmpty() || pass.isEmpty() || dest.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ha activado las alertas por correo electrónico.\nTodos los campos del servidor SMTP son estrictamente obligatorios.", "Validación de Red", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!user.matches("^[A-Za-z0-9+_.-]+@(.+)$") || !dest.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(this, "El usuario SMTP o el email destino no cuentan con una estructura de correo válida.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // 4. Persistencia segura una vez superados todos los filtros defensivos
        try {
            confDAO.guardar("empresa_nombre", nombre);
            confDAO.guardar("empresa_rfc", rfc);
            confDAO.guardar("empresa_email", emailCorp);
            confDAO.guardar("smtp_host", txtSmtpHost.getText().trim());
            confDAO.guardar("smtp_port", txtSmtpPort.getText().trim());
            confDAO.guardar("smtp_user", txtSmtpUser.getText().trim());
            confDAO.guardar("smtp_password", new String(txtSmtpPass.getPassword()));
            confDAO.guardar("alertas_email_destino", txtSmtpDest.getText().trim());
            confDAO.guardar("alertas_email_activo", String.valueOf(chkEmailActivo.isSelected()));
            
            JOptionPane.showMessageDialog(this,
                    "Configuración del sistema guardada correctamente.",
                    "Guardado Exitoso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error crítico al guardar en base de datos: " + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}