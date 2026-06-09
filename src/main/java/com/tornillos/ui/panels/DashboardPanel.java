package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.AlertaDAO;
import com.tornillos.dao.EntradaDAO;
import com.tornillos.dao.SalidaDAO;
import com.tornillos.dao.TornilloDAO;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;
    private JLabel lblTotal, lblStockBajo, lblEntradas, lblSalidas, lblAlertas;
    private JLabel lblLastUpdate;

    private final TornilloDAO tornilloDAO = new TornilloDAO();
    private final EntradaDAO entradaDAO = new EntradaDAO();
    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final AlertaDAO alertaDAO = new AlertaDAO();

    private javax.swing.Timer autoRefreshTimer;

    public DashboardPanel(MainFrame frame) {
        this.mainFrame = frame;
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        buildUI();
        autoRefreshTimer = new javax.swing.Timer(30000, e -> refresh());
        autoRefreshTimer.start();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 24));
        wrapper.setBackground(AppTheme.BG_SURFACE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        wrapper.add(buildHeader(), BorderLayout.NORTH);
        wrapper.add(buildBody(), BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── Header ────────────────────────────────────────────────
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

    // ── Body ──────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);

        // Fila KPI
        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiRow.setOpaque(false);

        lblTotal = kpiValue("0");
        lblStockBajo = kpiValue("0");
        lblEntradas = kpiValue("0");
        lblSalidas = kpiValue("0");
        lblAlertas = kpiValue("0");

        kpiRow.add(kpiCard("Total productos", lblTotal, AppTheme.ACCENT_LIGHT, "Ver inventario", "INVENTARIO"));
        kpiRow.add(kpiCard("Stock bajo", lblStockBajo, AppTheme.WARNING, "Ver alertas", "ALERTAS"));
        kpiRow.add(kpiCard("Entradas hoy", lblEntradas, AppTheme.SUCCESS_TEXT, "Registrar", "ENTRADAS"));
        kpiRow.add(kpiCard("Salidas hoy", lblSalidas, AppTheme.DANGER_TEXT, "Registrar", "SALIDAS"));
        kpiRow.add(kpiCard("Alertas activas", lblAlertas, AppTheme.WARNING_TEXT, "Ver alertas", "ALERTAS"));

        // Fila inferior
        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.add(buildAccionesRapidas());
        lower.add(buildInfoSesion());

        body.add(kpiRow, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        return body;
    }

    // ── Tarjeta KPI ───────────────────────────────────────────
    private JPanel kpiCard(String label, JLabel valueLabel, Color accentColor,
            String btnText, String navKey) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Borde izquierdo de color
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 3, getHeight());
                // Borde exterior sutil
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        // Label superior
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(AppTheme.FONT_LABEL);
        lbl.setForeground(AppTheme.TEXT_MUTED);

        // Valor
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        valueLabel.setForeground(accentColor);

        // Botón acción
        JButton btn = AppTheme.secondaryButton(btnText);
        btn.setFont(AppTheme.FONT_SMALL);
        btn.addActionListener(e -> mainFrame.showPanel(navKey));

        card.add(lbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    private JLabel kpiValue(String v) {
        JLabel l = new JLabel(v);
        l.setFont(new Font("Segoe UI", Font.BOLD, 34));
        l.setForeground(AppTheme.TEXT_PRIMARY);
        return l;
    }

    // ── Acciones rápidas ─────────────────────────────────────
    private JPanel buildAccionesRapidas() {
        JPanel card = buildCard("Acciones rápidas");

        JPanel btns = new JPanel(new GridLayout(0, 1, 0, 8));
        btns.setOpaque(false);

        String[][] actions = {
                { "Registrar entrada de tornillos", "ENTRADAS" },
                { "Registrar salida de tornillos", "SALIDAS" },
                { "Agregar nuevo tornillo", "INVENTARIO" },
                { "Ver reportes y exportar", "REPORTES" },
        };

        for (String[] a : actions) {
            JButton btn = AppTheme.secondaryButton(a[0]);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            String key = a[1];
            btn.addActionListener(e -> mainFrame.showPanel(key));
            btns.add(btn);
        }

        card.add(btns, BorderLayout.CENTER);
        return card;
    }

    // ── Info sesión ───────────────────────────────────────────
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

        // Indicador de rol
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

    // ── Helper tarjeta con título ─────────────────────────────
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

    // ── Refresh ───────────────────────────────────────────────
    public void refresh() {
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() throws Exception {
                return new int[] {
                        tornilloDAO.contarTotal(),
                        tornilloDAO.contarStockBajo(),
                        entradaDAO.contarHoy(),
                        salidaDAO.contarHoy(),
                        alertaDAO.contarActivas()
                };
            }

            @Override
            protected void done() {
                try {
                    int[] d = get();
                    lblTotal.setText(String.valueOf(d[0]));
                    lblStockBajo.setText(String.valueOf(d[1]));
                    lblEntradas.setText(String.valueOf(d[2]));
                    lblSalidas.setText(String.valueOf(d[3]));
                    lblAlertas.setText(String.valueOf(d[4]));
                    lblLastUpdate.setText("Actualizado: " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }
}
