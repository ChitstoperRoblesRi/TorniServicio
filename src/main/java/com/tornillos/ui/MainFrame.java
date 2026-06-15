package com.tornillos.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.tornillos.config.AppTheme;
import com.tornillos.dao.AlertaDAO;
import com.tornillos.model.Usuario;
import com.tornillos.service.AlertaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.panels.AlertasPanel;
import com.tornillos.ui.panels.ConfigPanel;
import com.tornillos.ui.panels.DashboardPanel;
import com.tornillos.ui.panels.EntradasPanel;
import com.tornillos.ui.panels.InventarioPanel;
import com.tornillos.ui.panels.ReportesPanel;
import com.tornillos.ui.panels.SalidasPanel;
import com.tornillos.ui.panels.UsuariosPanel;

public class MainFrame extends JFrame {

    private JPanel    contentPanel;
    private CardLayout cardLayout;
    private JButton   btnAlertasNav;
    private JLabel    lblAlertaBadge;
    private JPanel    navPanel;

    private final AlertaDAO     alertaDAO     = new AlertaDAO();
    private final AlertaService alertaService = new AlertaService();
    private ScheduledExecutorService scheduler;

    private DashboardPanel  dashboardPanel;
    private InventarioPanel inventarioPanel;
    private EntradasPanel   entradasPanel;
    private SalidasPanel    salidasPanel;
    private AlertasPanel    alertasPanel;
    private UsuariosPanel   usuariosPanel;
    private ReportesPanel   reportesPanel;
    private ConfigPanel     configPanel;


    public MainFrame() {
        setTitle("TornillosMax ERP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1200, 700));
        getContentPane().setBackground(AppTheme.BG_BASE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_BASE);
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildSidebar(),  BorderLayout.WEST);
        root.add(buildContent(),  BorderLayout.CENTER);
        add(root);
        setVisible(true);

        showPanel("DASHBOARD");
        actualizarBadgeAlertas();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            alertaService.verificarAlertas();
            SwingUtilities.invokeLater(this::actualizarBadgeAlertas);
        }, 15, 120, TimeUnit.SECONDS);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { scheduler.shutdown(); }
        });
    }

    // ── Barra de titulo ───────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BG_BASE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.BORDER_SUBTLE);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                // Acento dorado izquierdo
                g2.setColor(AppTheme.GOLD);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 40));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));

        JLabel brand = new JLabel("TORNILLOSMAX ERP");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 11));
        brand.setForeground(AppTheme.GOLD);

        bar.add(brand, BorderLayout.WEST);
        return bar;
    }


    // ── Sidebar ───────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sb = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.SIDEBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Línea divisora derecha
                g2.setColor(AppTheme.BORDER);
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                // Acento dorado izquierdo
                g2.setColor(AppTheme.GOLD);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        sb.setLayout(new BorderLayout());
        sb.setPreferredSize(new Dimension(214, 0));
        sb.setOpaque(false);

        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        navPanel.add(sideSection("Menu principal"));
        navPanel.add(sideBtn("Dashboard",    "DASHBOARD",   false));
        navPanel.add(sideBtn("Inventario",   "INVENTARIO",  false));
        navPanel.add(sideBtn("Entradas",     "ENTRADAS",    false));
        navPanel.add(sideBtn("Salidas",      "SALIDAS",     false));
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(sideSection("Gestión"));
        btnAlertasNav = sideBtn("Alertas", "ALERTAS", false);
        navPanel.add(btnAlertasNav);
        navPanel.add(sideBtn("Reportes",     "REPORTES",    false));

        if (SessionManager.getInstance().isGerente()) {
            navPanel.add(Box.createVerticalStrut(8));
            navPanel.add(sideSection("Administración"));
            navPanel.add(sideBtn("Usuarios",     "USUARIOS",    false));
            navPanel.add(sideBtn("Configuración","CONFIG",       false));
        }

        sb.add(navPanel, BorderLayout.CENTER);
        sb.add(buildSidebarFooter(), BorderLayout.SOUTH);
        return sb;
    }

    private JLabel sideSection(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(AppTheme.TEXT_MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(14, 20, 4, 12));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return l;
    }

    private JButton sideBtn(String text, String key, boolean active) {
        JButton btn = new JButton() {
            boolean hover = false;
            boolean sel   = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hover=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (sel) {
                    // Fondo activo sutil
                    g2.setColor(AppTheme.ACCENT);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // Barra dorada izquierda
                    g2.setColor(AppTheme.GOLD);
                    g2.fillRect(3, 4, 3, getHeight()-8);
                } else if (hover) {
                    g2.setColor(new Color(27, 58, 92, 40));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            public void setSelected(boolean s) {
                super.setSelected(s);
                sel = s;
                repaint();
            }
            @Override
            public boolean isSelected() {
                return sel;
            }
        };
        btn.setText("    " + text);
        btn.setFont(AppTheme.FONT_BODY);
        btn.setForeground(active ? AppTheme.GOLD_LIGHT : AppTheme.TEXT_SECONDARY);
        btn.setBackground(new Color(0,0,0,0));
        btn.setOpaque(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setContentAreaFilled(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setPreferredSize(new Dimension(214, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            showPanel(key);
        });
        btn.setName(key);
        return btn;
    }

    private void updateNavSelection(JButton selected) {
        Container parent = selected.getParent();
        if (parent == null) return;
        for (Component c : parent.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setSelected(b == selected);
                b.setForeground(b == selected ? AppTheme.GOLD_LIGHT : AppTheme.TEXT_SECONDARY);
            }
        }
    }

    private JPanel buildSidebarFooter() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
            BorderFactory.createEmptyBorder(12, 16, 14, 12)));

        Usuario u = SessionManager.getInstance().getUsuarioActual();
        String initials = "";
        if (u.getNombre() != null && !u.getNombre().isEmpty())   initials += u.getNombre().charAt(0);
        if (u.getApellido() != null && !u.getApellido().isEmpty()) initials += u.getApellido().charAt(0);

        // Avatar
        JLabel avatar = new JLabel(initials.toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.GOLD);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawOval(0, 0, getWidth()-1, getHeight()-1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        avatar.setForeground(AppTheme.GOLD_LIGHT);
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setVerticalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(32, 32));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 1));
        info.setOpaque(false);
        JLabel nameL = new JLabel(u.getNombreCompleto());
        nameL.setFont(AppTheme.FONT_BOLD); nameL.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel rolL  = new JLabel(u.getRol());
        rolL.setFont(AppTheme.FONT_SMALL);
        rolL.setForeground(u.isGerente() ? AppTheme.GOLD : AppTheme.TEXT_SECONDARY);
        info.add(nameL); info.add(rolL);

        JButton logout = new JButton("Salir");
        logout.setFont(AppTheme.FONT_SMALL);
        logout.setForeground(AppTheme.TEXT_MUTED);
        logout.setBackground(new Color(0,0,0,0)); logout.setOpaque(false);
        logout.setBorderPainted(false); logout.setFocusPainted(false); logout.setContentAreaFilled(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { logout.setForeground(AppTheme.DANGER_TEXT); }
            @Override public void mouseExited(MouseEvent e)  { logout.setForeground(AppTheme.TEXT_MUTED); }
        });
        logout.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this, "¿Deseas cerrar sesión?",
                "Cerrar sesión", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                scheduler.shutdown();
                SessionManager.getInstance().cerrarSesion();
                dispose();
                SwingUtilities.invokeLater(LoginFrame::new);
            }
        });

        p.add(avatar, BorderLayout.WEST);
        p.add(info,   BorderLayout.CENTER);
        p.add(logout, BorderLayout.EAST);
        return p;
    }

    // ── Area de contenido ─────────────────────────────────────
    private JPanel buildContent() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(AppTheme.BG_SURFACE);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_SURFACE);

        dashboardPanel  = new DashboardPanel(this);
        inventarioPanel = new InventarioPanel(this);
        entradasPanel   = new EntradasPanel(this);
        salidasPanel    = new SalidasPanel(this);
        alertasPanel    = new AlertasPanel(this);
        reportesPanel   = new ReportesPanel();
        usuariosPanel   = new UsuariosPanel();
        configPanel     = new ConfigPanel();

        contentPanel.add(dashboardPanel,  "DASHBOARD");
        contentPanel.add(inventarioPanel, "INVENTARIO");
        contentPanel.add(entradasPanel,   "ENTRADAS");
        contentPanel.add(salidasPanel,    "SALIDAS");
        contentPanel.add(alertasPanel,    "ALERTAS");
        contentPanel.add(reportesPanel,   "REPORTES");
        contentPanel.add(usuariosPanel,   "USUARIOS");
        contentPanel.add(configPanel,     "CONFIG");

        area.add(contentPanel, BorderLayout.CENTER);
        return area;
    }

    // ── Navegacion ────────────────────────────────────────────
    public void showPanel(String key) {
        cardLayout.show(contentPanel, key);
        if      ("DASHBOARD".equals(key))  dashboardPanel.refresh();
        else if ("INVENTARIO".equals(key)) inventarioPanel.refresh();
        else if ("ENTRADAS".equals(key))   entradasPanel.refresh();
        else if ("SALIDAS".equals(key))    salidasPanel.refresh();
        else if ("ALERTAS".equals(key))  { alertasPanel.refresh(); actualizarBadgeAlertas(); }
        else if ("REPORTES".equals(key))   reportesPanel.refresh();
        else if ("USUARIOS".equals(key))   usuariosPanel.refresh();

        if (navPanel != null) {
            for (Component c : navPanel.getComponents()) {
                if (c instanceof JButton && key.equals(c.getName())) {
                    updateNavSelection((JButton) c);
                    break;
                }
            }
        }
    }

    public void actualizarBadgeAlertas() {
        try {
            int count = alertaDAO.contarActivas();
            if (btnAlertasNav != null) {
                String t = count > 0 ? "    Alertas  [" + count + "]" : "    Alertas";
                btnAlertasNav.setText(t);
                if (btnAlertasNav.isSelected()) {
                    btnAlertasNav.setForeground(AppTheme.GOLD_LIGHT);
                } else {
                    btnAlertasNav.setForeground(count > 0 ? AppTheme.WARNING_TEXT : AppTheme.TEXT_SECONDARY);
                }
            }
        } catch (Exception ignored) {}
    }

    // 🌟 CORREGIDO: Enlaza el doble clic de Alertas, cambia de pestaña y pre-selecciona el tornillo
    public void IrAEntradasYReabastecer(String codigoTornillo) {
        // 1. Cambia visualmente la vista del CardLayout al panel de Entradas
        showPanel("ENTRADAS"); 

        // 2. Abre el formulario en el panel de entradas pasándole el código del tornillo
        entradasPanel.abrirFormularioEntrada(codigoTornillo); 
    }

    // 🌟 CORREGIDO: Adaptado a la nueva firma del método pasando 'null' para una entrada limpia
    public void gatillarNuevaEntrada() {
        showPanel("ENTRADAS");          // Cambia la vista a la pestaña de entradas
        entradasPanel.abrirFormularioEntrada(null); // 🌟 SOLUCIÓN: Pasa null para indicar que no viene de alertas
    }

    public void gatillarNuevaSalida() {
        showPanel("SALIDAS");
        salidasPanel.abrirFormularioSalida();
    }

    public void gatillarNuevoTornillo() {
        showPanel("INVENTARIO");
        inventarioPanel.abrirModalNuevoTornillo(); // Abrir diálogo con un Tornillo vacío para crear uno nuevo
    }

    public void gatillarReportes() {
        showPanel("REPORTES");
        reportesPanel.exportarCSV();
    }
}
