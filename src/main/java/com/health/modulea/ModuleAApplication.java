package com.health.modulea;

import com.health.modulea.ui.HealthModuleAFrame;

import javax.swing.SwingUtilities;

public class ModuleAApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new HealthModuleAFrame().setVisible(true);
            }
        });
    }
}
