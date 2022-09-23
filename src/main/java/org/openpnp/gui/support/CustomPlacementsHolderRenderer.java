package org.openpnp.gui.support;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import org.openpnp.model.PanelLocation;
import org.openpnp.model.PlacementsHolderLocation;

import java.awt.*;
import java.util.Arrays;

@SuppressWarnings("serial")
public class CustomPlacementsHolderRenderer extends JLabel implements TableCellRenderer, UIResource
{
    private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    private final JLabel left;
    private final JLabel right;
    
    public CustomPlacementsHolderRenderer() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(true);
        left = new JLabel();
        left.setHorizontalTextPosition(LEFT);
        left.setHorizontalAlignment(LEFT);
//        left.setOpaque(true);
        right = new JLabel();
        right.setHorizontalTextPosition(RIGHT);
        right.setHorizontalAlignment(LEFT);
//        right.setOpaque(true);
        this.add(left);
        this.add(right);
        setHorizontalAlignment(LEFT);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Color alternateRowColor = UIManager.getColor("Table.alternateRowColor");
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
            left.setForeground(table.getSelectionForeground());
            left.setBackground(table.getSelectionBackground());
            right.setForeground(table.getSelectionForeground());
            right.setBackground(table.getSelectionBackground());
        }
        else {
            setForeground(table.getForeground());
            setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
            left.setForeground(table.getForeground());
            left.setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
            right.setForeground(table.getForeground());
            right.setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
        }

        String uniqueId = (String) value;
        String id = uniqueId.substring(uniqueId.lastIndexOf(PlacementsHolderLocation.ID_DELIMITTER)+1);
        int depth = 0;
        int idx = -1;
        while ((idx = uniqueId.indexOf(PlacementsHolderLocation.ID_DELIMITTER, idx+1)) >= 0) {
            depth += 4;
        }
        
        char[] charArray = new char[depth];
        Arrays.fill(charArray, ' ');
        left.setText(new String(charArray));
        right.setText(id);
        
        right.setIcon(id.startsWith("Brd") ? Icons.board : Icons.panel);
        
        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            setBorder(noFocusBorder);
        }

        return this;
    }
}
