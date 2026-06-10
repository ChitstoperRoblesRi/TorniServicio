package com.tornillos.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import com.tornillos.config.AppTheme;

public class SearchableComboBoxFactory {

    public static <T> JComboBox<T> create(List<T> originalList) {
        DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        for (T item : originalList) {
            model.addElement(item);
        }

        JComboBox<T> comboBox = new JComboBox<T>(model) {
            @Override
            public Object getSelectedItem() {
                Object selected = super.getSelectedItem();
                if (selected instanceof String) {
                    String typedText = ((String) selected).trim();
                    for (T item : originalList) {
                        if (item.toString().equalsIgnoreCase(typedText)) {
                            return item;
                        }
                    }
                }
                return selected;
            }
        };

        // 1. Editor personalizado oscuro para evitar fondo blanco nativo de Windows
        comboBox.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override
            protected JTextField createEditorComponent() {
                JTextField txt = new JTextField();
                txt.setBackground(AppTheme.BG_CARD_HOVER);
                txt.setForeground(AppTheme.TEXT_PRIMARY);
                txt.setCaretColor(AppTheme.TEXT_PRIMARY);
                txt.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                return txt;
            }
        });

        comboBox.setEditable(true);

        // 2. Estilizamos el contenedor base del ComboBox
        comboBox.setBackground(AppTheme.BG_CARD_HOVER);
        comboBox.setForeground(AppTheme.TEXT_PRIMARY);

        // 3. Renderer premium: padding espacioso + colores del tema
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // Padding generoso para evitar la apariencia apretada
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                // Fondo del popup completo: evita área blanca en Windows cuando hay pocos ítems
                list.setBackground(AppTheme.BG_CARD);
                list.setSelectionBackground(AppTheme.ACCENT);
                list.setSelectionForeground(AppTheme.GOLD_LIGHT);

                if (isSelected) {
                    setBackground(AppTheme.ACCENT);
                    setForeground(AppTheme.GOLD_LIGHT);
                } else {
                    setBackground(AppTheme.BG_CARD);
                    setForeground(AppTheme.TEXT_PRIMARY);
                }
                return this;
            }
        });

        JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();

        editor.addKeyListener(new KeyAdapter() {
            private boolean filtering = false;

            @Override
            public void keyReleased(KeyEvent e) {
                if (filtering) return;

                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN ||
                    code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) {
                    return;
                }

                filtering = true;
                String text = editor.getText();

                DefaultComboBoxModel<T> newModel = new DefaultComboBoxModel<>();
                for (T item : originalList) {
                    if (item.toString().toLowerCase().contains(text.toLowerCase())) {
                        newModel.addElement(item);
                    }
                }

                ActionListener[] listeners = comboBox.getActionListeners();
                for (ActionListener l : listeners) comboBox.removeActionListener(l);

                comboBox.setModel(newModel);
                editor.setText(text);

                for (ActionListener l : listeners) comboBox.addActionListener(l);

                if (text.isEmpty()) {
                    comboBox.setPopupVisible(false);
                } else if (newModel.getSize() > 0) {
                    comboBox.setPopupVisible(true);
                }

                filtering = false;
            }
        });

        return comboBox;
    }
}