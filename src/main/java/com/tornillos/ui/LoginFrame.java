package com.tornillos.ui;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.UsuarioDAO;
import com.tornillos.model.Usuario;
import com.tornillos.service.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;

    public LoginFrame() {
        AppTheme.applyGlobalLookAndFeel();
        setTitle("TornillosMax ERP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 500);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.BG_BASE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(AppTheme.BG_BASE);
        root.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        root.add(buildLeft(), BorderLayout.WEST);
        root.add(buildRight(), BorderLayout.CENTER);

        add(root);
        setVisible(true);
    }

    // ── Panel izquierdo — marca ───────────────────────────────
    private JPanel buildLeft() {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.SIDEBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Acento dorado
                g2.setColor(AppTheme.GOLD);
                g2.fillRect(0, 0, 3, getHeight());
                // Línea derecha
                g2.setColor(AppTheme.BORDER);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(290, 0));
        p.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 32, 0, 32);

        // Logo
        JLabel logo = new JLabel("T") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.ACCENT);
                g2.fillOval(0, 0, 50, 50);
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(0, 0, 49, 49);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(AppTheme.GOLD_LIGHT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setVerticalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(50, 50));
        gbc.insets = new Insets(0, 32, 14, 32);
        p.add(logo, gbc);

        JLabel brand = new JLabel("TorniServicio");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brand.setForeground(AppTheme.GOLD_LIGHT);
        gbc.insets = new Insets(0, 32, 2, 32);
        p.add(brand, gbc);

        JLabel sub = new JLabel("Sistema ERP de Inventario");
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.TEXT_SECONDARY);
        gbc.insets = new Insets(0, 32, 26, 32);
        p.add(sub, gbc);

        return p;
    }

    // Panel derecho — formulario puro
    private JPanel buildRight() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(AppTheme.BG_SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(40, 52, 40, 52));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        JLabel title = new JLabel("Iniciar sesión");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.GOLD_LIGHT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        p.add(title, gbc);

        JLabel sub = new JLabel("Ingresa tus credenciales para continuar...");
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.TEXT_SECONDARY);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 32, 0);
        p.add(sub, gbc);

        // Usuario
        addFieldLabel(p, gbc, 3, "USUARIO");
        txtUsername = AppTheme.styledField("Nombre de usuario");
        txtUsername.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 20, 0);
        p.add(txtUsername, gbc);

        // Contraseña
        addFieldLabel(p, gbc, 5, "CONTRASEÑA");
        txtPassword = AppTheme.styledPasswordField();
        txtPassword.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 28, 0);
        p.add(txtPassword, gbc);

        // Botón login
        btnLogin = AppTheme.goldButton("Ingresar al sistema");
        btnLogin.setPreferredSize(new Dimension(0, 44));
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 14, 0);
        p.add(btnLogin, gbc);

        // Status
        lblStatus = new JLabel(" ");
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.DANGER_TEXT);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        p.add(lblStatus, gbc);

        // Listeners
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());

        return p;
    }

    private void addFieldLabel(JPanel p, GridBagConstraints gbc, int row, String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.FONT_LABEL);
        l.setForeground(AppTheme.TEXT_MUTED);
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.anchor = GridBagConstraints.WEST;
        p.add(l, gbc);
    }

    // ── Login ─────────────────────────────────────────────────
    private void doLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Ingresa usuario y contraseña");
            return;
        }
        btnLogin.setEnabled(false);
        lblStatus.setForeground(AppTheme.TEXT_SECONDARY);
        lblStatus.setText("Verificando...");

        SwingWorker<Usuario, Void> w = new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws Exception {
                return new UsuarioDAO().autenticar(user, pass);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    Usuario u = get();
                    if (u != null) {
                        SessionManager.getInstance().iniciarSesion(u);
                        dispose();
                        SwingUtilities.invokeLater(MainFrame::new);
                    } else {
                        lblStatus.setForeground(AppTheme.DANGER_TEXT);
                        lblStatus.setText("Usuario o contraseña incorrectos");
                        txtPassword.setText("");
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(AppTheme.DANGER_TEXT);
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    lblStatus.setText("Error de conexion: " + msg);
                }
            }
        };
        w.execute();
    }
}
