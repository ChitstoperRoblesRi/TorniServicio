package com.tornillos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Usuario;
import com.tornillos.service.SessionManager;
import com.tornillos.service.UsuarioService;

public class UsuariosPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private JLabel lblConteo;
    
    private final UsuarioService usuarioService = new UsuarioService();

    private final java.time.format.DateTimeFormatter visualFormatter = 
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public UsuariosPanel() {
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
        if (!SessionManager.getInstance().isGerente()) {
            JLabel lbl = new JLabel("Acceso restringido a Gerentes.", SwingConstants.CENTER);
            lbl.setFont(AppTheme.FONT_HEADING);
            lbl.setForeground(AppTheme.DANGER);
            add(lbl, BorderLayout.CENTER);
            return;
        }
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(AppTheme.BG_SURFACE);
        main.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(buildContent(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        JLabel title = new JLabel("Gestión de Usuarios");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        lblConteo = new JLabel("");
        lblConteo.setFont(AppTheme.FONT_SMALL);
        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        left.add(title);
        left.add(lblConteo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton btnNuevo = AppTheme.primaryButton("+ Nuevo Usuario");
        JButton btnRefresh = AppTheme.secondaryButton("Actualizar");
        btnNuevo.addActionListener(e -> abrirDialogoUsuario(null));
        btnRefresh.addActionListener(e -> refresh());
        right.add(btnNuevo);
        right.add(btnRefresh);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        txtBuscar = AppTheme.styledField("Buscar por nombre, usuario o email...");
        txtBuscar.setPreferredSize(new Dimension(340, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                buscar(); // 🌟 CORREGIDO (Problema 3): Búsqueda reactiva automática por cada letra
            }
        });
        
        // 🌟 CORREGIDO (Problema 3): Se elimina el botón "Buscar" físico redundante para limpiar la interfaz
        JButton btnLimpiar = AppTheme.secondaryButton("X Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            refresh();
        });
        bar.add(txtBuscar);
        bar.add(btnLimpiar);
        p.add(bar, BorderLayout.NORTH);

        String[] cols = { "ID", "Username", "Nombre", "Apellido", "Email", "Rol", "Estado", "Última sesión" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? AppTheme.BG_CARD : AppTheme.BG_SURFACE);
                    if (col == 6) {
                        Object val = getValueAt(row, col);
                        if ("Inactivo".equals(val != null ? val.toString() : "")) {
                            c.setForeground(AppTheme.DANGER_TEXT);
                        } else {
                            c.setForeground(AppTheme.SUCCESS_TEXT);
                        }
                    } else {
                        c.setForeground(AppTheme.TEXT_PRIMARY);
                    }
                } else {
                    c.setBackground(AppTheme.ACCENT);
                    c.setForeground(AppTheme.GOLD_LIGHT);
                }

                // 🌟 CORREGIDO (Problema 4): Centrado rígido de las columnas Rol (5), Estado (6) y Última sesión (7)
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    if (col == 5 || col == 6 || col == 7) {
                        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    } else {
                        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    }
                }
                return c;
            }
        };
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        // 🌟 NUEVO (Problema 4): Centrar los títulos de los encabezados correspondientes de la tabla
        for (int col : new int[]{ 5, 6, 7 }) {
            table.getColumnModel().getColumn(col).setHeaderRenderer((t, val, sel, focus, r, c) -> {
                Component comp = t.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                }
                return comp;
            });
        }

        // final JPopupMenu menuContextual = buildContextMenu();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0)
                        abrirDialogoPorFila(row);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) { evaluarClicContextual(e); }
            @Override
            public void mouseReleased(MouseEvent e) { evaluarClicContextual(e); }

            // Busca este método dentro del MouseListener de tu JTable
            private void evaluarClicContextual(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        
                        // 🌟 NUEVO: Extrae el estado visual e inyéctalo en el generador del menú
                        String estadoStr = tableModel.getValueAt(row, 6).toString();
                        boolean esActivo = "Activo".equals(estadoStr);
                        
                        JPopupMenu menuContextual = buildContextMenu(esActivo);
                        menuContextual.show(table, e.getX(), e.getY());
                    } else {
                        table.clearSelection();
                    }
                }
            }
        });

        p.add(AppTheme.darkScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    // 🌟 CORREGIDO: Ahora el menú se adapta dinámicamente al estado real del usuario seleccionado
    private JPopupMenu buildContextMenu(boolean esActivo) {
        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEditar = AppTheme.darkMenuItem("Editar usuario", null);
        JMenuItem itemPassword = AppTheme.darkMenuItem("Cambiar contraseña", null);
        
        itemEditar.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) abrirDialogoPorFila(r);
        });
        itemPassword.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) cambiarPassword((int) tableModel.getValueAt(r, 0));
        });
        
        popup.add(itemEditar);
        popup.add(AppTheme.darkSeparator());
        popup.add(itemPassword);
        popup.add(AppTheme.darkSeparator());
        
        // Muestra únicamente la acción opuesta al estado actual del operador
        if (esActivo) {
            JMenuItem itemInhabilitar = AppTheme.darkMenuItem("Inhabilitar usuario", null);
            itemInhabilitar.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r >= 0) cambiarEstado((int) tableModel.getValueAt(r, 0), false);
            });
            popup.add(itemInhabilitar);
        } else {
            JMenuItem itemHabilitar = AppTheme.darkMenuItem("Habilitar usuario", null);
            itemHabilitar.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r >= 0) cambiarEstado((int) tableModel.getValueAt(r, 0), true);
            });
            popup.add(itemHabilitar);
        }
        return popup;
    }

    private void buscar() {
        String t = txtBuscar.getText().trim();
        SwingWorker<List<Usuario>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                return usuarioService.buscarUsuarios(t);
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        poblarTabla(get());
                    }
                } catch (Exception ex) { /* ignored */ }
            }
        };
        w.execute();
    }

    private void cambiarEstado(int id, boolean habilitar) {
        String accion = habilitar ? "habilitar" : "inhabilitar";
        int opt = JOptionPane.showConfirmDialog(this,
                "¿Deseas " + accion + " este usuario?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                usuarioService.cambiarEstadoUsuario(id, habilitar);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirDialogoUsuario(Usuario usuario) {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                usuario == null ? "Nuevo Usuario" : "Editar Usuario", true);
        dlg.setSize(440, 420); // Incrementamos ligeramente el alto para comodidad de las alertas
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AppTheme.BG_CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // 🌟 CORREGIDO (Problema 2): Cajas de texto limpias sin texto por defecto para eliminar fricción al operador
        JTextField txtNombre = AppTheme.styledField("");
        JTextField txtApellido = AppTheme.styledField("");
        JTextField txtEmail = AppTheme.styledField("");
        JTextField txtUsername = AppTheme.styledField("");
        
        if (usuario != null) {
            txtNombre.setText(usuario.getNombre());
            txtApellido.setText(usuario.getApellido());
            txtEmail.setText(usuario.getEmail());
            txtUsername.setText(usuario.getUsername());
            txtUsername.setEditable(false);
            txtUsername.setForeground(AppTheme.TEXT_MUTED);
        }
        
        JPasswordField txtPass = AppTheme.styledPasswordField();
        
        JComboBox<String> cmbRol = AppTheme.styledCombo(new String[] { "EMPLEADO", "GERENTE" });
        if (usuario != null && "GERENTE".equals(usuario.getRol()))
            cmbRol.setSelectedIndex(1);

        cmbRol.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                btn.setForeground(AppTheme.BG_BASE);
                btn.setBackground(AppTheme.BG_CARD_HOVER);
                btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                btn.setContentAreaFilled(true);
                btn.setFocusable(false);
                return btn;
            }
        });

        cmbRol.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));

        cmbRol.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override
            protected JTextField createEditorComponent() {
                JTextField txt = new JTextField();
                txt.setBackground(AppTheme.BG_CARD_HOVER);
                txt.setForeground(AppTheme.TEXT_PRIMARY);
                txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                txt.setEditable(false);
                txt.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        if (cmbRol.isEnabled()) {
                            if (cmbRol.isPopupVisible()) cmbRol.hidePopup();
                            else cmbRol.showPopup();
                        }
                    }
                });
                return txt;
            }
        });
        cmbRol.setEditable(true);
        
        Object inicialMotivo = cmbRol.getSelectedItem();
        cmbRol.getEditor().setItem(inicialMotivo != null ? inicialMotivo.toString() : "");
        cmbRol.addActionListener(ev -> {
            Object item = cmbRol.getSelectedItem();
            cmbRol.getEditor().setItem(item != null ? item.toString() : "");
        });

        // 🌟 NUEVO: Añadido indicador '*' en las etiquetas para guiar visualmente al usuario
        addRow(form, gbc, 0, "Nombre *:", txtNombre);
        addRow(form, gbc, 1, "Apellido *:", txtApellido);
        addRow(form, gbc, 2, "Email *:", txtEmail);
        addRow(form, gbc, 3, "Username *:", txtUsername);
        if (usuario == null)
            addRow(form, gbc, 4, "Contraseña *:", txtPass);
        addRow(form, gbc, usuario == null ? 5 : 4, "Rol *:", cmbRol);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnCancel = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.primaryButton("Confirmar");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            try {
                // 🌟 CORREGIDO (Problema 1): Capa defensiva estricta de sanitización y validación
                String nombre = txtNombre.getText().trim();
                String apellido = txtApellido.getText().trim();
                String email = txtEmail.getText().trim();
                String username = txtUsername.getText().trim();

                if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || username.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Todos los campos marcados con (*) son estrictamente obligatorios.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Expresión regular estándar para verificar estructura de correo electrónico
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(dlg, "El formato del correo electrónico ingresado no es válido.", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
                    txtEmail.requestFocus();
                    return;
                }

                // 🌟 CORREGIDO: Validación simétrica de longitud mínima para altas de nuevos usuarios
                String passRaw = new String(txtPass.getPassword());
                if (usuario == null) {
                    if (passRaw.isEmpty()) {
                        JOptionPane.showMessageDialog(dlg, "La contraseña es requerida para el registro de nuevos usuarios.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
                        txtPass.requestFocus();
                        return;
                    }
                    if (passRaw.length() < 6) {
                        JOptionPane.showMessageDialog(dlg, "La contraseña es demasiado débil.\nDebe contener un mínimo de 6 caracteres.", "Seguridad Insuficiente", JOptionPane.WARNING_MESSAGE);
                        txtPass.requestFocus();
                        return;
                    }
                }

                if (usuario == null) {
                    Usuario u = new Usuario();
                    u.setNombre(nombre);
                    u.setApellido(apellido);
                    u.setEmail(email);
                    u.setUsername(username);
                    u.setRolId(cmbRol.getSelectedIndex() == 1 ? 1 : 2);
                    String pass = new String(txtPass.getPassword());
                    
                    usuarioService.registrarUsuario(u, pass);
                } else {
                    Usuario u = new Usuario();
                    u.setId(usuario.getId());
                    u.setNombre(nombre);
                    u.setApellido(apellido);
                    u.setEmail(email);
                    u.setUsername(username);
                    u.setRolId(cmbRol.getSelectedIndex() == 1 ? 1 : 2);
                    u.setActivo(usuario.isActivo());
                    
                    usuarioService.actualizarUsuario(u);
                }
                dlg.dispose();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btns.add(btnCancel);
        btns.add(btnGuardar);
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        form.add(btns, gbc);
        dlg.add(form);
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.35;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        p.add(field, gbc);
    }

    private void abrirDialogoPorFila(int row) {
        Usuario u = new Usuario();
        u.setId((int) tableModel.getValueAt(row, 0));
        u.setUsername(tableModel.getValueAt(row, 1).toString());
        u.setNombre(tableModel.getValueAt(row, 2).toString());
        u.setApellido(tableModel.getValueAt(row, 3).toString());
        u.setEmail(tableModel.getValueAt(row, 4).toString());
        u.setRol(tableModel.getValueAt(row, 5).toString());
        u.setRolId("GERENTE".equals(u.getRol()) ? 1 : 2);
        
        String estadoStr = tableModel.getValueAt(row, 6).toString();
        u.setActivo("Activo".equals(estadoStr));
        
        abrirDialogoUsuario(u);
    }

    private void cambiarPassword(int userId) {
        JPasswordField pf1 = AppTheme.styledPasswordField();
        JPasswordField pf2 = AppTheme.styledPasswordField();
        JPanel p = new JPanel(new GridLayout(4, 1, 0, 8));
        p.setBackground(AppTheme.BG_CARD);
        p.add(AppTheme.label("Nueva contraseña:"));
        p.add(pf1);
        p.add(AppTheme.label("Confirmar contraseña:"));
        p.add(pf2);
        int res = JOptionPane.showConfirmDialog(this, p, "Cambiar Contraseña", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String p1 = new String(pf1.getPassword());
            String p2 = new String(pf2.getPassword());
            if (!p1.equals(p2)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (p1.length() < 6) {
                JOptionPane.showMessageDialog(this, "Mínimo 6 caracteres.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                usuarioService.redefinirPassword(userId, p1);
                JOptionPane.showMessageDialog(this, "Contraseña actualizada correctamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void poblarTabla(List<Usuario> lista) {
        tableModel.setRowCount(0);
        for (Usuario u : lista) {
            String fechaFormateada = (u.getUltimaSesion() != null) 
                    ? u.getUltimaSesion().format(visualFormatter) : "Nunca";

            tableModel.addRow(new Object[] {
                    u.getId(), u.getUsername(), u.getNombre(), u.getApellido(),
                    u.getEmail(), u.getRol(),
                    u.isActivo() ? "Activo" : "Inactivo",
                    fechaFormateada
            });
        }
        lblConteo.setText(lista.size() + " usuario(s)");
    }

    public void refresh() {
        SwingWorker<List<Usuario>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                return usuarioService.obtenerTodosLosUsuarios();
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        poblarTabla(get());
                    }
                } catch (Exception ex) { /* ignored */ }
            }
        };
        w.execute();
    }
}