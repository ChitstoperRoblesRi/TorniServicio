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
import java.math.BigDecimal;
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
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.tornillos.config.AppTheme;
import com.tornillos.model.Entrada;
import com.tornillos.model.Tornillo;
import com.tornillos.service.EntradaService;
import com.tornillos.service.SessionManager;
import com.tornillos.ui.MainFrame;
import com.tornillos.util.FolioGenerator;

public class EntradasPanel extends JPanel {
    private final MainFrame mainFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtBuscar, txtDesde, txtHasta;
    private JLabel lblConteo;
    private SwingWorker<?, ?> currentWorker;

    // Cambiado: El panel ahora depende estrictamente de su Capa de Servicio dedicada
    private final EntradaService entradaService = new EntradaService();

    public EntradasPanel(MainFrame frame) {
        this.mainFrame = frame;
        setBackground(AppTheme.BG_SURFACE);
        setLayout(new BorderLayout());
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
        JLabel title = new JLabel("Módulo de Entradas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        lblConteo = new JLabel("");
        lblConteo.setFont(AppTheme.FONT_SMALL);
        lblConteo.setForeground(AppTheme.TEXT_MUTED);
        left.add(title);
        left.add(lblConteo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton btnNueva = AppTheme.successButton("+ Nueva Entrada");
        btnNueva.addActionListener(e -> abrirFormularioEntrada());
        right.add(btnNueva);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        txtBuscar = AppTheme.styledField("Buscar por folio, tornillo...");
        txtBuscar.setPreferredSize(new Dimension(260, 34));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                buscar();
            }
        });
        txtDesde = AppTheme.styledField("Desde YYYY-MM-DD");
        txtDesde.setPreferredSize(new Dimension(150, 34));
        txtDesde.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });
        txtHasta = AppTheme.styledField("Hasta YYYY-MM-DD");
        txtHasta.setPreferredSize(new Dimension(150, 34));
        txtHasta.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) refresh();
            }
        });

        JButton btnFiltrar = AppTheme.primaryButton("Filtrar");
        JButton btnLimpiar = AppTheme.secondaryButton("X Limpiar");
        btnFiltrar.addActionListener(e -> refresh());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            txtDesde.setText("");
            txtHasta.setText("");
            refresh();
        });

        bar.add(txtBuscar);
        bar.add(AppTheme.label("Desde:"));
        bar.add(txtDesde);
        bar.add(AppTheme.label("Hasta:"));
        bar.add(txtHasta);
        bar.add(btnFiltrar);
        bar.add(btnLimpiar);
        p.add(bar, BorderLayout.NORTH);

        String[] cols = { "ID", "Folio", "Tornillo", "Codigo", "Cantidad", "P.Unitario", "Total", "Usuario", "Fecha" };
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
                    c.setForeground(AppTheme.TEXT_PRIMARY);
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
            public void mousePressed(MouseEvent e) {
                evaluarClicContextual(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                evaluarClicContextual(e);
            }

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

        // Solución al glitch visual de renderizado al hacer scroll
        JScrollPane scrollPane = AppTheme.darkScrollPane(table);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        
        p.add(scrollPane, BorderLayout.CENTER);
        return p;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu popup = AppTheme.darkPopup();
        JMenuItem itemEliminar = AppTheme.darkMenuItem("Eliminar entrada (revierte stock)", null);
        itemEliminar.addActionListener(e -> eliminarSeleccionada());
        if (SessionManager.getInstance().isGerente()) {
            popup.add(itemEliminar);
        }
        return popup;
    }

    private void buscar() {
        refresh();
    }

    private void eliminarSeleccionada() {
        if (!SessionManager.getInstance().isGerente())
            return;
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una entrada.");
            return;
        }
        String folio = tableModel.getValueAt(row, 1).toString();
        int opt = JOptionPane.showConfirmDialog(this,
                "Eliminar entrada " + folio + "?\nEsto revertira el stock del tornillo.",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                // Cambiado: Ahora la UI llama al servicio para orquestar la eliminación y actualización
                entradaService.eliminarEntrada((int) tableModel.getValueAt(row, 0));
                mainFrame.actualizarBadgeAlertas();
                refresh();
                JOptionPane.showMessageDialog(this, "Entrada eliminada y stock revertido.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void abrirFormularioEntrada() {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                "Registrar Entrada", true);
        dlg.setSize(520, 480);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AppTheme.BG_CARD);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        List<Tornillo> tornillos;
        try {
            // Cambiado: Se le solicitan los tornillos de inventario al Servicio
            tornillos = entradaService.obtenerCatalogoTornillos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando datos: " + e.getMessage());
            return;
        }

        JComboBox<Tornillo> cmbTornillo = com.tornillos.util.SearchableComboBoxFactory.create(tornillos);
        cmbTornillo.setSelectedIndex(-1);
        cmbTornillo.setBackground(AppTheme.BG_CARD_HOVER);
        cmbTornillo.setForeground(AppTheme.TEXT_PRIMARY);

        cmbTornillo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
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
        cmbTornillo.setBorder(new javax.swing.border.Border() {
            @Override
            public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
            @Override public boolean isBorderOpaque() { return false; }
        });
        unificarEstiloEditorCombo(cmbTornillo, true);

        JTextField txtCantidad = AppTheme.styledField("0");
        JTextField txtPrecio = AppTheme.styledField("0.00");
        JTextField txtFactura = AppTheme.styledField("Numero de factura (opcional)");
        JTextArea txtObs = AppTheme.styledTextArea();
        txtObs.setRows(2);

        cmbTornillo.addActionListener(e -> {
            // 🌟 CLAVE: Obtenemos la selección como Object genérico primero
            Object seleccionadoObj = cmbTornillo.getSelectedItem();
            
            // Solo procesamos la lógica si lo seleccionado es verdaderamente una instancia de Tornillo
            if (seleccionadoObj instanceof Tornillo) {
                Tornillo seleccionado = (Tornillo) seleccionadoObj;
                if (seleccionado.getPrecioCosto() != null) {
                    txtPrecio.setText(seleccionado.getPrecioCosto().toPlainString());
                } else {
                    txtPrecio.setText("0.00");
                }
            } else {
                // Si es un String de texto libre porque el usuario estaba escribiendo,
                // ignoramos pacíficamente el evento sin romper la consola
                txtPrecio.setText("0.00");
            }
        });

        String folio = FolioGenerator.generarEntrada();
        JTextField txtFolio = AppTheme.styledField(folio);
        txtFolio.setText(folio);
        txtFolio.setEditable(false);
        txtFolio.setForeground(AppTheme.TEXT_MUTED);

        addRow(form, gbc, 0, "Folio:", txtFolio);
        addRow(form, gbc, 1, "Tornillo:", cmbTornillo);
        addRow(form, gbc, 2, "Cantidad:", txtCantidad);
        addRow(form, gbc, 3, "Precio Unitario:", txtPrecio);
        addRow(form, gbc, 4, "No. Factura:", txtFactura);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        form.add(AppTheme.label("Observaciones:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(AppTheme.darkScrollPane(txtObs), gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnCancel = AppTheme.secondaryButton("Cancelar");
        JButton btnGuardar = AppTheme.successButton("Registrar Entrada");
        btnCancel.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            try {
                Tornillo t = (Tornillo) cmbTornillo.getSelectedItem();
                if (t == null) throw new IllegalArgumentException("Debe seleccionar un tornillo de la lista.");
                
                int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
                if (cantidad <= 0)
                    throw new IllegalArgumentException("Cantidad debe ser mayor a 0");
                if (precio.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("El precio unitario debe ser un número positivo mayor a 0.");

                BigDecimal totalCalculado = precio.multiply(BigDecimal.valueOf(cantidad));
                String resumenTicket = String.format(
                    "¿Confirmar el registro de esta Entrada?\n\n" +
                    "▪ Producto:  %s\n" +
                    "▪ Cantidad:  %d unidades\n" +
                    "▪ P. Unitario: $%,.2f\n" +
                    "▪ Total Operación: $%,.2f\n\n",
                    t.getNombre(), cantidad, precio, totalCalculado
                );
                int confirmacion = JOptionPane.showConfirmDialog(
                    dlg, resumenTicket, "Confirmar Entrada de Inventario",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
                );
                if (confirmacion != JOptionPane.YES_OPTION) return;

                Entrada entrada = new Entrada();
                entrada.setFolio(folio);
                entrada.setTornilloId(t.getId());
                entrada.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
                entrada.setCantidad(cantidad);
                entrada.setPrecioUnitario(precio);
                entrada.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)));
                entrada.setNumeroFactura(txtFactura.getText().trim());
                entrada.setObservaciones(txtObs.getText().trim());

                // Cambiado: Invocación delegada limpiamente a través de EntradaService
                entradaService.registrarEntrada(entrada);
                
                dlg.dispose();
                refresh();
                mainFrame.actualizarBadgeAlertas();

                JOptionPane.showMessageDialog(this,
                        "Entrada registrada. Folio: " + folio, "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Cantidad y precio deben ser numeros validos.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0; 
        form.add(btns, gbc);
        btns.add(btnCancel);
        btns.add(btnGuardar);
        dlg.add(new JScrollPane(form));
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        p.add(AppTheme.label(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        p.add(field, gbc);
    }

    /**
     * Modifica los componentes visuales del editor interno del JComboBox para adaptarlo
     * al tema oscuro sin alterar ni destruir los listeners funcionales del componente.
     */
    private void unificarEstiloEditorCombo(JComboBox<?> combo, boolean esBuscable) {
        // 🌟 CLAVE: Obtenemos el componente editor actual en lugar de destruirlo con setEditor()
        Component editorComp = combo.getEditor().getEditorComponent();
        
        if (editorComp instanceof JTextField) {
            JTextField txt = (JTextField) editorComp;
            
            // Aplicamos los colores del tema oscuro premium
            txt.setBackground(AppTheme.BG_CARD_HOVER);
            txt.setForeground(AppTheme.TEXT_PRIMARY);
            txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6)); // Padding interno
            
            // Si es buscable (como los tornillos), permitimos la edición. 
            // Si no (como los motivos), se bloquea para que actúe como dropdown cerrado.
            txt.setEditable(esBuscable);
            
            // El MouseListener de despliegue automático solo se añade si NO es buscable.
            // Si es buscable, el usuario necesita hacer clic para posicionar el cursor y escribir.
            if (!esBuscable) {
                txt.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        if (combo.isEnabled()) {
                            if (combo.isPopupVisible()) combo.hidePopup();
                            else combo.showPopup();
                        }
                    }
                });
            }
        }
        
        // Forzamos a que el combo reconozca que debe pintar su editor interno
        combo.setEditable(true);
    }

    private void poblarTabla(List<Entrada> lista) {
        tableModel.setRowCount(0);
        for (Entrada e : lista) {
            tableModel.addRow(new Object[] {
                    e.getId(), e.getFolio(),
                    e.getTornilloNombre(), e.getTornilloCodigo(),
                    e.getCantidad(), e.getPrecioUnitario(), e.getTotal(),
                    e.getUsuarioNombre(),
                    e.getFecha() != null ? e.getFecha().toString().substring(0, 16) : ""
            });
        }
        lblConteo.setText(lista.size() + " entrada(s)");
    }

    public void refresh() {
        if (currentWorker != null && !currentWorker.isDone())
            currentWorker.cancel(true);
        currentWorker = new SwingWorker<List<Entrada>, Void>() {
            @Override
            protected List<Entrada> doInBackground() throws Exception {
                String termino = txtBuscar.getText().trim();
                String desde = txtDesde.getText().trim().isEmpty() ? null : txtDesde.getText().trim();
                String hasta = txtHasta.getText().trim().isEmpty() ? null : txtHasta.getText().trim();
                
                // Cambiado: Consumo seguro a través de la Capa de Servicios
                return entradaService.buscarEntradas(termino, desde, hasta);
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        poblarTabla(get());
                    }
                } catch (Exception ex) {
                    // Control preventivo de flujo asíncrono
                }
            }
        };
        currentWorker.execute();
    }
}