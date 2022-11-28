package org.openpnp.gui.support;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

@SuppressWarnings("serial")
public class CustomAlignmentRenderer<T extends TableCellRenderer> extends DefaultTableCellRenderer
{
    private T renderer;
    private int alignment;
    
    public CustomAlignmentRenderer(T renderer) {
        this(renderer, SwingConstants.CENTER);
    }

    public CustomAlignmentRenderer(T renderer, int alignment) {
//        super();
        this.renderer = renderer;
        this.alignment = alignment;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        try {
            ((JLabel) component).setHorizontalAlignment(alignment);
        }
        catch (ClassCastException ex) {
            //do nothing
        }
        return component;
    }
}
