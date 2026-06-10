package com.tornillos.config;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

/**
 * TornillosMax ERP — Design System Corporativo
 * Paleta inspirada en Bloomberg / SAP Fiori Dark / Oracle Fusion
 */
public class AppTheme {

    // ── Paleta principal ──────────────────────────────────────
    public static final Color BG_BASE = new Color(0x0E1520); // fondo profundo
    public static final Color BG_SURFACE = new Color(0x111827); // contenedor principal
    public static final Color BG_CARD = new Color(0x152238); // tarjetas
    public static final Color BG_CARD_HOVER = new Color(0x1A2D48); // hover tarjeta
    public static final Color SIDEBAR_BG = new Color(0x0A1018); // sidebar oscuro

    // Acento: azul marino corporativo
    public static final Color ACCENT = new Color(0x1B3A5C);
    public static final Color ACCENT_MID = new Color(0x2A5280);
    public static final Color ACCENT_LIGHT = new Color(0x4A7FAC);

    // Dorado institucional (marca / activo)
    public static final Color GOLD = new Color(0xC9A84C);
    public static final Color GOLD_LIGHT = new Color(0xE8DCC8);
    public static final Color GOLD_DIM = new Color(0x8A6E2E);

    // Semánticos sobrios
    public static final Color SUCCESS = new Color(0x1E7A5A);
    public static final Color SUCCESS_BG = new Color(0x0A2A1E);
    public static final Color SUCCESS_TEXT = new Color(0x4DC99A);
    public static final Color DANGER = new Color(0xC0392B);
    public static final Color DANGER_BG = new Color(0x2A0E0E);
    public static final Color DANGER_TEXT = new Color(0xFF8080);
    public static final Color WARNING = new Color(0xD4A843);
    public static final Color WARNING_BG = new Color(0x2A1E08);
    public static final Color WARNING_TEXT = new Color(0xF0CC70);

    // Texto
    public static final Color TEXT_PRIMARY = new Color(0xE8DCC8); // crema
    public static final Color TEXT_SECONDARY = new Color(0x6A7E99); // gris azulado
    public static final Color TEXT_MUTED = new Color(0x3A4A5C); // muy apagado
    public static final Color TEXT_HEADING = new Color(0xD4C9B0); // titulos

    // Bordes
    public static final Color BORDER = new Color(0x1B3A5C); // borde estándar
    public static final Color BORDER_SUBTLE = new Color(0x152238); // borde sutil
    public static final Color BORDER_ACTIVE = new Color(0xC9A84C); // borde activo/dorado

    // ── Fuentes ───────────────────────────────────────────────
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 10);

    // ── Look & Feel global ────────────────────────────────────
    public static void applyGlobalLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        UIManager.put("Panel.background", BG_SURFACE);
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("TextField.background", BG_CARD);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", GOLD);
        UIManager.put("TextArea.background", BG_CARD);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.caretForeground", GOLD);
        UIManager.put("ComboBox.background", BG_CARD);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("CheckBox.background", BG_CARD);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", BG_CARD);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor", BORDER_SUBTLE);
        UIManager.put("Table.selectionBackground", ACCENT);
        UIManager.put("Table.selectionForeground", GOLD_LIGHT);
        UIManager.put("TableHeader.background", BG_BASE);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("ScrollPane.background", BG_BASE);
        UIManager.put("TabbedPane.background", BG_CARD);
        UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected", BG_CARD_HOVER);
        UIManager.put("Separator.foreground", BORDER);

        // PopupMenu corporativo
        UIManager.put("PopupMenu.background", BG_CARD);
        UIManager.put("PopupMenu.foreground", TEXT_PRIMARY);
        UIManager.put("PopupMenu.border",
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        UIManager.put("MenuItem.background", BG_CARD);
        UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", ACCENT_MID);
        UIManager.put("MenuItem.selectionForeground", GOLD_LIGHT);
        UIManager.put("MenuItem.font", FONT_BODY);
        UIManager.put("MenuItem.border",
                BorderFactory.createEmptyBorder(7, 14, 7, 14));
        UIManager.put("Menu.background", BG_CARD);
        UIManager.put("Menu.foreground", TEXT_PRIMARY);
        UIManager.put("Menu.selectionBackground", ACCENT_MID);
        UIManager.put("Menu.selectionForeground", GOLD_LIGHT);
    }

    // ── MENU CONTEXTUAL — pintado a mano, ignora L&F del sistema ─────
    public static JPopupMenu darkPopup() {
        JPopupMenu popup = new JPopupMenu() {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x18243A));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        popup.setOpaque(false);
        popup.setBackground(new Color(0x18243A));
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        return popup;
    }

    public static JMenuItem darkMenuItem(String texto, Color colorTexto) {
        Color fg = (colorTexto != null) ? colorTexto : TEXT_PRIMARY;
        JMenuItem item = new JMenuItem(texto) {
            boolean hovered = false;
            {
                setOpaque(false);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hovered || getModel().isArmed()) {
                    g2.setColor(ACCENT_MID);
                    g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 5, 5);
                    setForeground(GOLD_LIGHT);
                } else {
                    g2.setColor(new Color(0x18243A));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    setForeground(fg);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setBackground(new Color(0x18243A));
        item.setForeground(fg);
        item.setFont(FONT_BODY);
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return item;
    }

    public static JSeparator darkSeparator() {
        JSeparator sep = new JSeparator() {
            @Override
            public void paintComponent(Graphics g) {
                g.setColor(BORDER);
                g.fillRect(8, getHeight() / 2, getWidth() - 16, 1);
            }
        };
        sep.setPreferredSize(new Dimension(0, 9));
        sep.setOpaque(false);
        return sep;
    }

    // ── BOTONES ───────────────────────────────────────────────
    /** Botón primario: azul marino con texto crema */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed() ? ACCENT.darker()
                        : getModel().isRollover() ? ACCENT_MID : ACCENT;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(BORDER_ACTIVE);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, ACCENT, GOLD_LIGHT);
        return btn;
    }

    /** Botón de peligro: rojo sobrio */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed() ? DANGER.darker()
                        : getModel().isRollover() ? new Color(0xA0302A) : DANGER;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, DANGER, new Color(0xFFCCCC));
        return btn;
    }

    /** Botón secundario: borde sutil, sin relleno */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isRollover() ? BG_CARD_HOVER : BG_CARD;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, BG_CARD, TEXT_SECONDARY);
        return btn;
    }

    /** Botón éxito: verde institucional */
    public static JButton successButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed() ? SUCCESS.darker()
                        : getModel().isRollover() ? new Color(0x25966E) : SUCCESS;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, SUCCESS, new Color(0xCCF0E0));
        return btn;
    }

    /** Botón dorado — acciones de marca/primarias destacadas */
    public static JButton goldButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed() ? GOLD_DIM
                        : getModel().isRollover() ? new Color(0xD4B055) : GOLD;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, GOLD, new Color(0x1A1200));
        return btn;
    }

    private static void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(FONT_BOLD);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 24, 36));
    }

    // ── CAMPOS DE TEXTO ───────────────────────────────────────
    public static JTextField styledField(String placeholder) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && placeholder != null && !placeholder.isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(106, 126, 153, 160));
                    g2.setFont(getFont());
                    Insets insets = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    int x = insets.left;
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(placeholder, x, y);
                    g2.dispose();
                }
            }
        };
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(GOLD);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, false),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT_LIGHT, 1, false),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1, false),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
        return f;
    }

    public static JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(GOLD);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, false),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT_LIGHT, 1, false),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1, false),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
        return f;
    }

    public static JTextArea styledTextArea() {
        JTextArea ta = new JTextArea();
        ta.setBackground(BG_CARD);
        ta.setForeground(TEXT_PRIMARY);
        ta.setCaretColor(GOLD);
        ta.setFont(FONT_BODY);
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    public static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setBackground(sel ? ACCENT_MID : BG_CARD);
                setForeground(sel ? GOLD_LIGHT : TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return this;
            }
        });
        return combo;
    }

    // ── CONTENEDORES ─────────────────────────────────────────
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return p;
    }

    public static JScrollPane darkScrollPane(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_BASE);
        sp.getViewport().setBackground(BG_BASE);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getVerticalScrollBar().setUI(new CorporateScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new CorporateScrollBarUI());
        return sp;
    }

    // ── LABELS ────────────────────────────────────────────────
    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADING);
        l.setForeground(TEXT_HEADING);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    /** Etiqueta de sección en mayúsculas pequeñas */
    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    // ── BADGE de estado ───────────────────────────────────────
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setForeground(fg);
        l.setFont(FONT_SMALL);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return l;
    }

    // ── TABLA OSCURA ─────────────────────────────────────────
    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(34);
        table.setGridColor(BORDER_SUBTLE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(GOLD_LIGHT);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_BASE);
        header.setForeground(TEXT_SECONDARY);
        header.setFont(FONT_LABEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                l.setBackground(BG_BASE);
                l.setForeground(TEXT_SECONDARY);
                l.setFont(FONT_LABEL);
                l.setText(v != null ? v.toString().toUpperCase() : "");
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                l.setOpaque(true);
                return l;
            }
        });

        // Renderer de filas alternadas
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (sel) {
                    setBackground(ACCENT_MID);
                    setForeground(GOLD_LIGHT);
                } else {
                    setBackground(row % 2 == 0 ? BG_CARD : new Color(0x101B2E));
                    setForeground(TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setFont(FONT_BODY);
                setOpaque(true);
                return this;
            }
        });
    }

    // ── SCROLLBAR corporativa ─────────────────────────────────
    public static class CorporateScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = ACCENT_MID;
            trackColor = BG_BASE;
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return invisBtn();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return invisBtn();
        }

        private JButton invisBtn() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty())
                return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? GOLD_DIM : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(BG_BASE);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}
