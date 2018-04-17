/*
 * DrawerFrame.java
 */

package VC.TreeDrawer;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

class DrawerFrame extends JFrame {
    public DrawerFrame(JPanel panel) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        int screenHeight = d.height;
        int screenWidth = d.width;
        setTitle("The VC Compiler Abstract Syntax Tree");
        setSize((screenWidth * 2) / 3, (screenHeight * 2) / 3);
        setLocation(screenWidth / 6, screenHeight / 6);
        // Image img = tk.getImage("icon.gif");
        // setIconImage(img);

        addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                }
        );
        Container contentPane = getContentPane();
        contentPane.add(new JScrollPane(panel));
    }
}
