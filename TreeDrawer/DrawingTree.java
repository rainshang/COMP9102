/*
 * DrawingTree.java
 */

package VC.TreeDrawer;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.awt.Point;

public class DrawingTree {

    private final static int PADDING = 20;
    private static int _left = Integer.MAX_VALUE;
    private static int _top = Integer.MAX_VALUE;
    private static int _right = Integer.MIN_VALUE;
    private static int _bottom = Integer.MIN_VALUE;

    String caption;
    int width, height;
    Point pos, offset;
    Polygon contour;
    DrawingTree parent;
    DrawingTree[] children;
    FontMetrics fontMetrics;
    Font font;

    public DrawingTree(String caption, int width, int height, FontMetrics fontMetrics) {
        this.caption = caption;
        this.width = width;
        this.height = height;
        this.parent = null;
        this.children = null;
        this.pos = new Point(0, 0);
        this.offset = new Point(0, 0);
        this.contour = new Polygon();
        this.fontMetrics = fontMetrics;
        this.font = fontMetrics.getFont();
    }

    public void setChildren(DrawingTree[] children) {
        this.children = children;
        for (int i = 0; i < children.length; i++)
            children[i].parent = this;
    }

    private final int FIXED_FONT_HEIGHT = 10;
    private final int FIXED_FONT_ASCENT = 3;
    private final Color nodeColor = new Color(255, 255, 255);

    public void paint(Graphics graphics) {
        graphics.setFont(font);
        graphics.setColor(nodeColor);
        graphics.fillRect(pos.x, pos.y, width, height);
        graphics.setColor(Color.black);
        graphics.drawRect(pos.x, pos.y, width - 1, height - 1);
        graphics.drawString(caption, pos.x + 2,
                pos.y + (height + FIXED_FONT_HEIGHT) / 2);

        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                children[i].paint(graphics);
            }
        }

        if (parent != null) {
            graphics.drawLine(pos.x + width / 2, pos.y,
                    parent.pos.x + parent.width / 2,
                    parent.pos.y + parent.height);
        }
    }

    private void position(Point pos) {

        this.pos.x = pos.x + this.offset.x;
        this.pos.y = pos.y + this.offset.y;

        Point temp = new Point(this.pos.x, this.pos.y);

        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                children[i].position(temp);
                temp.x += children[i].offset.x;
                temp.y = this.pos.y + children[0].offset.y;
            }
        }

        if (_left > pos.x) {
            _left = pos.x;
        }
        if (_top > pos.y) {
            _top = pos.y;
        }
        if (_right < pos.x + width) {
            _right = pos.x + width;
        }
        if (_bottom < pos.y + height) {
            _bottom = pos.y + height;
        }
    }

    /**
     * only tree root node should call this func
     */
    public void fitPerfectDisplayPoint(JPanel jPanel) {
        position(new Point(0, 0));
        // I don't figure out why it displays weird when scrollbar shows. It made me to add the additional 50
        jPanel.setPreferredSize(new Dimension(_right - _left + PADDING * 2 + 50, _bottom - _top + PADDING * 2 + 50));
        position(new Point(-_left + PADDING, PADDING));
    }

}
