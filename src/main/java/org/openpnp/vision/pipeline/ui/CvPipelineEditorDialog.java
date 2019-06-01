package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JDialog;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

public class CvPipelineEditorDialog extends JDialog {
private CvPipelineEditor editor;

public CvPipelineEditorDialog(Frame owner, String title, CvPipelineEditor editor) {
	super(owner, title);
	getContentPane().setLayout(new BorderLayout());
    getContentPane().add(editor);
    setSize(1024, 768);    
    this.editor = editor;
}

public void setVisible(boolean b) {
	super.setVisible(b);
	editor.initializeFocus();
}
}