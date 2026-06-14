package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.ui.MainFrame;
import com.tornillos.service.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;
    private JLabel lblTotal, lblSinStock, lblEntradas, lblSalidas, lblTopProducto;
    private JLabel lblLastUpdate;
    
    // Sub-etiquetas descriptivas dinámicas de analítica
    private JLabel lblDescCard1, lblDescCard2, lblDescCard3, lblDescCard4, lblDescCard5;

    private javax.swing.Timer autoRefreshTimer;

    public DashboardPanel(MainFrame frame) {
        this.mainFrame = frame;
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        buildUI();
        
        // Timer de refresco automático cada 30 segundos
        autoRefreshTimer = new javax.swing.Timer(30000, e -> refresh());
        autoRefreshTimer.start();
        refresh(); // Carga inicial
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 24));
        wrapper.setBackground(AppTheme.BG_SURFACE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        wrapper.add(buildHeader(), BorderLayout.NORTH);
        wrapper.add(buildBody(), BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 3));
        left.setOpaque(false);

        JLabel title = new JLabel("Dashboard");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.GOLD_LIGHT);

        lblLastUpdate = new JLabel("Actualizando...");
        lblLastUpdate.setFont(AppTheme.FONT_SMALL);
        lblLastUpdate.setForeground(AppTheme.TEXT_MUTED);

        left.add(title);
        left.add(lblLastUpdate);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        String user = SessionManager.getInstance().getUsuarioActual().getNombreCompleto();
        JLabel welcome = new JLabel("Bienvenido, " + user);
        welcome.setFont(AppTheme.FONT_SMALL);
        welcome.setForeground(AppTheme.TEXT_SECONDARY);

        right.add(welcome);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);

        // Fila KPI
        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiRow.setOpaque(false);

        lblTotal = new JLabel("0");
        lblSinStock = new JLabel("0");
        lblEntradas = new JLabel("0");
        lblSalidas = new JLabel("0");
        lblTopProducto = new JLabel("Ninguno");

        lblDescCard1 = createDescLabel("Catálogo activo");
        lblDescCard2 = createDescLabel("En stock crítico: 0");
        lblDescCard3 = createDescLabel("Última: Ninguna");
        lblDescCard4 = createDescLabel("Última: Ninguna");
        lblDescCard5 = createDescLabel("Total: $0.00");

        kpiRow.add(kpiMetricCard("Total productos", lblTotal, AppTheme.ACCENT_LIGHT, lblDescCard1, false));
        kpiRow.add(kpiMetricCard("Sin Stock", lblSinStock, AppTheme.DANGER_TEXT, lblDescCard2, false));
        kpiRow.add(kpiMetricCard("Entradas hoy", lblEntradas, AppTheme.SUCCESS_TEXT, lblDescCard3, false));
        kpiRow.add(kpiMetricCard("Salidas hoy", lblSalidas, AppTheme.WARNING, lblDescCard4, false));
        kpiRow.add(kpiMetricCard("Top Producto", lblTopProducto, AppTheme.GOLD, lblDescCard5, true));

        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.add(buildAccionesRapidas());
        lower.add(buildInfoSesion());

        body.add(kpiRow, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        return body;
    }

    private JPanel kpiMetricCard(String title, JLabel valueLabel, Color accentColor, JLabel descLabel, boolean esTextoLargo) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Línea indicadora izquierda
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 3, getHeight());
                // Marco sutil redondeado
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(180, 155));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // 1. Título superior
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(AppTheme.FONT_LABEL);
        lblTitle.setForeground(AppTheme.TEXT_SECONDARY);
        gbc.gridy = 0;
        gbc.weighty = 0.1;
        card.add(lblTitle, gbc);

        // 2. Valor Central
        gbc.gridy = 1;
        gbc.weighty = 0.5;
        gbc.insets = new Insets(2, 0, 2, 0);
        
        if (esTextoLargo) {
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        } else {
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        }
        valueLabel.setForeground(accentColor);
        card.add(valueLabel, gbc);

        // 3. Etiqueta Informativa Inferior
        gbc.gridy = 2;
        gbc.weighty = 0.4;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(descLabel, gbc);

        return card;
    }

    private JLabel createDescLabel(String defaultText) {
        JLabel l = new JLabel(defaultText);
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(AppTheme.TEXT_PRIMARY);
        return l;
    }

    private JPanel buildAccionesRapidas() {
        JPanel card = buildCard("Acciones rápidas");
        JPanel btns = new JPanel(new GridLayout(0, 1, 0, 8));
        btns.setOpaque(false);

        String[][] actions = {
                { "Registrar entrada de tornillos", "ENTRADAS" },
                { "Registrar salida de tornillos", "SALIDAS" },
                { "Agregar nuevo tornillo", "INVENTARIO" },
                { "Exportar reportes CSV", "REPORTES" },
        };

        for (String[] a : actions) {
            JButton btn = AppTheme.secondaryButton(a[0]);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            String key = a[1];
            btn.addActionListener(e -> {
                switch (key) {
                    case "ENTRADAS": mainFrame.gatillarNuevaEntrada(); break;
                    case "SALIDAS": mainFrame.gatillarNuevaSalida(); break;
                    case "INVENTARIO": mainFrame.gatillarNuevoTornillo(); break;
                    case "REPORTES": mainFrame.gatillarReportes(); break;
                    default: mainFrame.showPanel(key); break;
                }
            });
            btns.add(btn);
        }
        card.add(btns, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInfoSesion() {
        JPanel card = buildCard("Información de sesión");
        JPanel info = new JPanel(new GridLayout(0, 1, 0, 10));
        info.setOpaque(false);

        var u = SessionManager.getInstance().getUsuarioActual();
        addInfoRow(info, "Usuario", u.getNombreCompleto());
        addInfoRow(info, "Rol", u.getRol());
        addInfoRow(info, "Última sesión",
                u.getUltimaSesion() != null
                        ? u.getUltimaSesion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "Primera sesión");

        JPanel rolBadgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rolBadgeRow.setOpaque(false);
        Color badgeBg = u.isGerente() ? AppTheme.ACCENT : new Color(0x1A2D1A);
        Color badgeFg = u.isGerente() ? AppTheme.GOLD_LIGHT : AppTheme.SUCCESS_TEXT;
        rolBadgeRow.add(AppTheme.badge(u.isGerente() ? "Acceso total" : "Acceso operativo", badgeBg, badgeFg));
        info.add(rolBadgeRow);

        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private void addInfoRow(JPanel p, String key, String val) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel k = new JLabel(key);
        k.setFont(AppTheme.FONT_SMALL);
        k.setForeground(AppTheme.TEXT_MUTED);
        JLabel v = new JLabel(val);
        v.setFont(AppTheme.FONT_BOLD);
        v.setForeground(AppTheme.TEXT_PRIMARY);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.BORDER_SUBTLE);
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.add(row, BorderLayout.CENTER);
        wrap.add(sep, BorderLayout.SOUTH);
        p.add(wrap);
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

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(AppTheme.FONT_LABEL);
        lbl.setForeground(AppTheme.TEXT_MUTED);
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.BORDER);
        hdr.add(lbl, BorderLayout.NORTH);
        hdr.add(sep, BorderLayout.SOUTH);
        card.add(hdr, BorderLayout.NORTH);
        return card;
    }

    // ── REFRESH SINCRONIZADO CONTRA BORRADOS LÓGICOS ──
    public void refresh() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                int totalProductos = 0;
                int sinStock = 0;
                int criticoStock = 0;
                int entradasHoy = 0;
                int salidasHoy = 0;
                String ultimaEntrada = "Ninguna";
                String ultimaSalida = "Ninguna";
                String topProducto = "Ninguno";
                double topTotalGenerado = 0.0;

                Connection conn = com.tornillos.config.DatabaseConfig.getConnection();

                // 1. Estados Semánticos
                String sqlInventario = "SELECT " +
                        "  COUNT(*) FILTER (WHERE activo = true) AS total, " +
                        "  COUNT(*) FILTER (WHERE activo = true AND stock_actual = 0) AS sin_stock, " +
                        "  COUNT(*) FILTER (WHERE activo = true AND stock_actual <= stock_minimo / 2 AND stock_actual > 0) AS criticos " +
                        "FROM tornillos";
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlInventario)) {
                    if (rs.next()) {
                        totalProductos = rs.getInt("total");
                        sinStock = rs.getInt("sin_stock");
                        criticoStock = rs.getInt("criticos");
                    }
                }

                // 2. Transacciones Diarias: Entradas (CORREGIDA con activo = true)
                String sqlEntradasHoy = "SELECT COUNT(*), COALESCE((SELECT t.nombre FROM entradas e2 JOIN tornillos t ON e2.tornillo_id = t.id WHERE DATE(e2.fecha) = CURRENT_DATE AND e2.activo = true ORDER BY e2.fecha DESC LIMIT 1), 'Ninguna') FROM entradas WHERE DATE(fecha) = CURRENT_DATE AND activo = true";
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlEntradasHoy)) {
                    if (rs.next()) {
                        entradasHoy = rs.getInt(1);
                        ultimaEntrada = rs.getString(2);
                    }
                }

                // 3. Transacciones Diarias: Salidas (CORREGIDA con activo = true)
                String sqlSalidasHoy = "SELECT COUNT(*), COALESCE((SELECT t.nombre FROM salidas s2 JOIN tornillos t ON s2.tornillo_id = t.id WHERE DATE(s2.fecha) = CURRENT_DATE AND s2.activo = true ORDER BY s2.fecha DESC LIMIT 1), 'Ninguna') FROM salidas WHERE DATE(fecha) = CURRENT_DATE AND activo = true";
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlSalidasHoy)) {
                    if (rs.next()) {
                        salidasHoy = rs.getInt(1);
                        ultimaSalida = rs.getString(2);
                    }
                }

                // 4. Métrica de Producto Estrella (CORREGIDA con s.activo = true)
                String sqlTop = "SELECT t.nombre, SUM(s.total) AS total_dia " +
                                "FROM salidas s JOIN tornillos t ON s.tornillo_id = t.id " +
                                "WHERE DATE(s.fecha) = CURRENT_DATE AND s.activo = true " +
                                "GROUP BY t.id, t.nombre ORDER BY total_dia DESC LIMIT 1";
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlTop)) {
                    if (rs.next()) {
                        topProducto = rs.getString("nombre");
                        topTotalGenerado = rs.getDouble("total_dia");
                    }
                }

                return new Object[] { totalProductos, sinStock, criticoStock, entradasHoy, ultimaEntrada, salidasHoy, ultimaSalida, topProducto, topTotalGenerado };
            }

            @Override
            protected void done() {
                try {
                    Object[] d = get();
                    
                    // Números KPI puros, limpios y gigantescos de vuelta en las primeras 4 tarjetas
                    lblTotal.setText(String.valueOf(d[0]));
                    lblSinStock.setText(String.valueOf(d[1]));
                    lblEntradas.setText(String.valueOf(d[3]));
                    lblSalidas.setText(String.valueOf(d[5]));
                    
                    // Card 5: El nombre del tornillo estrella baja de línea de manera controlada y limpia
                    String prodEstrella = (String) d[7];
                    lblTopProducto.setText("<html><p style='width: 150px; margin: 0; padding: 0; font-family: Segoe UI; font-weight: bold;'>" + prodEstrella + "</p></html>");

                    // Etiquetas descriptivas inferiores estructuradas en HTML sin tocar los números principales
                    lblDescCard1.setText("<html>Catálogo activo</html>");
                    lblDescCard2.setText("<html>En stock crítico: <b style='color:#F0CC70;'>" + d[2] + "</b></html>");
                    
                    String ent = (String) d[4];
                    lblDescCard3.setText("<html><p style='width: 125px; margin: 0; padding: 0;'>Última:<br><span style='color:#A0AABF; font-size:10px;'>" + ent + "</span></p></html>");
                    
                    String sal = (String) d[6];
                    lblDescCard4.setText("<html><p style='width: 125px; margin: 0; padding: 0;'>Última:<br><span style='color:#A0AABF; font-size:10px;'>" + sal + "</span></p></html>");
                    
                    lblDescCard5.setText(String.format("<html>Total generado:<br><span style='color:#4DC99A; font-size:11px; font-weight:bold;'>$%.2f</span></html>", (Double) d[8]));

                    lblLastUpdate.setText("Actualizado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                } catch (Exception ex) {
                    Logger.getLogger(DashboardPanel.class.getName()).log(Level.SEVERE, "Error al refrescar dashboard", ex);
                }
            }
        }.execute();
    }
}