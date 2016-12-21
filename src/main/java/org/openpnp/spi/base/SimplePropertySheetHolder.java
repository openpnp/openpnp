package org.openpnp.spi.base;

import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.spi.PropertySheetHolder;

public class SimplePropertySheetHolder implements PropertySheetHolder {
    protected String title;
    protected List<? extends PropertySheetHolder> children;
    protected Icon icon;

    public SimplePropertySheetHolder(String title, List<? extends PropertySheetHolder> children) {
        this(title, children, null);
    }

    public SimplePropertySheetHolder(String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        this.title = title;
        this.children = children;
        this.icon = icon;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return title;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        if (children == null) {
            return null;
        }
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return icon;
    }
}
