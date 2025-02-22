/*
 * DrawerPanel.java
 */

package VC.TreeDrawer;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

class DrawerPanel extends JPanel {
    private Drawer drawer;

    public DrawerPanel(Drawer drawer) {
        this.drawer = drawer;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawer.paintAST(g);
    }
}
