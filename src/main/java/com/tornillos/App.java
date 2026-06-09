package com.tornillos;

import com.tornillos.config.AppTheme;
import com.tornillos.ui.LoginFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // Habilitar anti-aliasing global
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", "true");

        AppTheme.applyGlobalLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error iniciando la aplicación:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}