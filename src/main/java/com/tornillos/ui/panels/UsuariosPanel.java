package com.tornillos.ui.panels;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Usuario;
import com.tornillos.service.UsuarioService;
import com.tornillos.service.SessionManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class UsuariosPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar;
    private JLabel lblConteo;
    
    // Cambiado: El panel ahora depende estrictamente de su Capa de Servicio dedicada
    private final UsuarioService usuarioService = new UsuarioService();

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
                buscar();
            }
        });
        JButton btnBuscar = AppTheme.primaryButton("Buscar");
        JButton btnLimpiar = AppTheme.secondaryButton("X Limpiar");
        btnBuscar.addActionListener(e -> buscar());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            refresh();
        });
        bar.add(txtBuscar);
        bar.add(btnBuscar);
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
                return c;
            }
        };
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        final JPopupMenu menuContextual = buildContextMenu();

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

            private void evaluarClicContextual(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
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

    private JPopupMenu buildContextMenu() {
        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEditar = AppTheme.darkMenuItem("Editar usuario", null);
        JMenuItem itemPassword = AppTheme.darkMenuItem("Cambiar contraseña", null);
        JMenuItem itemInhabilitar = AppTheme.darkMenuItem("Inhabilitar usuario", null);
        JMenuItem itemHabilitar = AppTheme.darkMenuItem("Habilitar usuario", null);
        
        itemEditar.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) abrirDialogoPorFila(r);
        });
        itemPassword.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) cambiarPassword((int) tableModel.getValueAt(r, 0));
        });
        itemInhabilitar.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) cambiarEstado((int) tableModel.getValueAt(r, 0), false);
        });
        itemHabilitar.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) cambiarEstado((int) tableModel.getValueAt(r, 0), true);
        });
        
        popup.add(itemEditar);
        popup.add(AppTheme.darkSeparator());
        popup.add(itemPassword);
        popup.add(AppTheme.darkSeparator());
        popup.add(itemInhabilitar);
        popup.add(itemHabilitar);
        return popup;
    }

    private void buscar() {
        String t = txtBuscar.getText().trim();
        SwingWorker<List<Usuario>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                // Cambiado: Invocación delegada a través del servicio
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
                // Cambiado: El servicio procesa el cambio de estado operativo
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
        dlg.setSize(440, 400);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AppTheme.BG_CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField txtNombre = AppTheme.styledField("Nombre");
        JTextField txtApellido = AppTheme.styledField("Apellido");
        JTextField txtEmail = AppTheme.styledField("Email");
        JTextField txtUsername = AppTheme.styledField("Username");
        
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

        addRow(form, gbc, 0, "Nombre:", txtNombre);
        addRow(form, gbc, 1, "Apellido:", txtApellido);
        addRow(form, gbc, 2, "Email:", txtEmail);
        addRow(form, gbc, 3, "Username:", txtUsername);
        if (usuario == null)
            addRow(form, gbc, 4, "Contraseña:", txtPass);
        addRow(form, gbc, usuario == null ? 5 : 4, "Rol:", cmbRol);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnCancel = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.primaryButton("Confirmar");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            try {
                if (usuario == null) {
                    Usuario u = new Usuario();
                    u.setNombre(txtNombre.getText().trim());
                    u.setApellido(txtApellido.getText().trim());
                    u.setEmail(txtEmail.getText().trim());
                    u.setUsername(txtUsername.getText().trim());
                    u.setRolId(cmbRol.getSelectedIndex() == 1 ? 1 : 2);
                    String pass = new String(txtPass.getPassword());
                    
                    // Cambiado: Registro delegado al servicio corporativo
                    usuarioService.registrarUsuario(u, pass);
                } else {
                    Usuario u = new Usuario();
                    u.setId(usuario.getId());
                    u.setNombre(txtNombre.getText().trim());
                    u.setApellido(txtApellido.getText().trim());
                    u.setEmail(txtEmail.getText().trim());
                    u.setUsername(txtUsername.getText().trim());
                    u.setRolId(cmbRol.getSelectedIndex() == 1 ? 1 : 2);
                    u.setActivo(usuario.isActivo());
                    
                    // Cambiado: Modificación delegada al servicio
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
                // Cambiado: Redefinición segura a través del servicio
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
            tableModel.addRow(new Object[] {
                    u.getId(), u.getUsername(), u.getNombre(), u.getApellido(),
                    u.getEmail(), u.getRol(),
                    u.isActivo() ? "Activo" : "Inactivo",
                    u.getUltimaSesion() != null ? u.getUltimaSesion().toString().substring(0, 16) : "Nunca"
            });
        }
        lblConteo.setText(lista.size() + " usuario(s)");
    }

    public void refresh() {
        SwingWorker<List<Usuario>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                // Cambiado: Consumo indirecto de persistencia
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