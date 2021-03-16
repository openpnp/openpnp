package org.openpnp.gui.support;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.commons.beanutils.PropertyUtils;

public class PropertyEdit extends AbstractUndoableEdit {
    private static final long serialVersionUID = 1L;
    
    final String presentationName;
    final Object target;
    final String property;
    final Object oldValue;
    final Object newValue;
    
    public PropertyEdit(String presentationName, Object target, String property, Object newValue) {
        if (presentationName != null) {
            this.presentationName = presentationName;
        }
        else {
            this.presentationName = "";
        }
        this.target = target;
        this.property = property;
        this.newValue = newValue;
        try {
            this.oldValue = PropertyUtils.getProperty(target, property);
        }
        catch (Exception e) {
            throw new CannotRedoException();
        }
        try {
            PropertyUtils.setProperty(target, property, newValue);
        }
        catch (Exception e) {
            throw new CannotRedoException();
        }
    }
    
    public PropertyEdit(Object target, String property, Object newValue) {
        this(null, target, property, newValue);
    }

    @Override
    public void undo() {
        super.undo();
        try {
            PropertyUtils.setProperty(target, property, oldValue);
        }
        catch (Exception e) {
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() {
        super.redo();
        try {
            PropertyUtils.setProperty(target, property, newValue);
        }
        catch (Exception e) {
            throw new CannotRedoException();
        }
    }
}