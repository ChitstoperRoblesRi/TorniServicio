package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.service.AlertaService;
import com.tornillos.service.ConfiguracionService;
import com.tornillos.service.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ConfigPanel extends JPanel {

    private final ConfiguracionService confService = new ConfiguracionService();
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

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        cards.add(buildEmpresaCard());
        cards.add(buildEmailCard());
        main.add(cards, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setOpaque(false);
        JButton btnGuardar = AppTheme.goldButton("Guardar configuración");
        btnGuardar.addActionListener(e -> guardar());
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
        addRow(form, gbc, 0, "Nombre:", txtEmpresa);
        addRow(form, gbc, 1, "RFC:", txtRfc);
        addRow(form, gbc, 2, "Email corporativo:", txtEmail);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildEmailCard() {
        JPanel card = buildCard("Alertas por correo electrónico (SMTP)");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = gbc();

        chkEmailActivo = new JCheckBox("Activar envío de alertas por email");
        chkEmailActivo.setFont(AppTheme.FONT_BODY);
        chkEmailActivo.setForeground(AppTheme.TEXT_PRIMARY);
        chkEmailActivo.setOpaque(false);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(chkEmailActivo, gbc);

        txtSmtpHost = AppTheme.styledField("smtp.gmail.com");
        txtSmtpPort = AppTheme.styledField("587");
        txtSmtpUser = AppTheme.styledField("tu@gmail.com");
        txtSmtpPass = AppTheme.styledPasswordField();
        txtSmtpDest = AppTheme.styledField("destino@empresa.com");

        addRow(form, gbc, 1, "SMTP Host:", txtSmtpHost);
        addRow(form, gbc, 2, "Puerto:", txtSmtpPort);
        addRow(form, gbc, 3, "Usuario:", txtSmtpUser);
        addRow(form, gbc, 4, "Contraseña:", txtSmtpPass);
        addRow(form, gbc, 5, "Destino:", txtSmtpDest);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        btnProbar = new JButton("Probar conexión SMTP");
        btnProbar.setFont(AppTheme.FONT_SMALL);
        btnProbar.setForeground(AppTheme.TEXT_PRIMARY);
        btnProbar.setBackground(AppTheme.BG_CARD_HOVER);
        btnProbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        btnProbar.setFocusPainted(false);
        btnProbar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
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

    private void cargarConfig() {
        try {
            Map<String, String> conf = confService.obtenerTodas();
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
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando config: " + e.getMessage());
        }
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
                    btnProbar.setEnabled(true);
                    btnProbar.setText("Probar conexión SMTP");
                }
            }
        };
        w.execute();
    }

    private void guardar() {
        try {
            confService.guardar("empresa_nombre", txtEmpresa.getText().trim());
            confService.guardar("empresa_rfc", txtRfc.getText().trim());
            confService.guardar("empresa_email", txtEmail.getText().trim());
            confService.guardar("smtp_host", txtSmtpHost.getText().trim());
            confService.guardar("smtp_port", txtSmtpPort.getText().trim());
            confService.guardar("smtp_user", txtSmtpUser.getText().trim());
            confService.guardar("smtp_password", new String(txtSmtpPass.getPassword()));
            confService.guardar("alertas_email_destino", txtSmtpDest.getText().trim());
            confService.guardar("alertas_email_activo", String.valueOf(chkEmailActivo.isSelected()));
            JOptionPane.showMessageDialog(this,
                    "Configuración guardada correctamente.",
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al guardar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}