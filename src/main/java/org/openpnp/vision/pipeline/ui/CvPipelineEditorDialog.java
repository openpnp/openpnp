package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Rectangle;

import javax.swing.JDialog;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

public class CvPipelineEditorDialog extends JDialog {
    private CvPipelineEditor editor;

    public CvPipelineEditorDialog(Frame owner, String title, CvPipelineEditor editor) {
        super(owner, title);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(editor);
        Rectangle rect = owner.getBounds();
        int wmargin = rect.width/20;
        int hmargin = rect.height/20;
        setBounds(new Rectangle(
                rect.x+wmargin, 
                rect.y+hmargin, 
                rect.width-2*wmargin,
                rect.height-2*hmargin));    
        this.editor = editor;
    }

    public void setVisible(boolean b) {
        super.setVisible(b);
        editor.initializeFocus();
    }
}