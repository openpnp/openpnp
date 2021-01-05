package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CvPipelineEditorDialog extends JDialog {
private CvPipelineEditor editor;

public CvPipelineEditorDialog(Frame owner, String title, CvPipelineEditor editor) {
	super(owner, title);
	setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
	        if (editor.isDirty()) {
	            int selection = JOptionPane.showConfirmDialog(owner,
	                    "Save pipeline changes?",
	                    "Closing Pipeline Editor!",
	                    JOptionPane.YES_NO_CANCEL_OPTION,
	                    JOptionPane.QUESTION_MESSAGE,
	                    null
	                    );
	            switch (selection) {
                    case JOptionPane.YES_OPTION:
                        super.windowClosing(e);
                        CvPipelineEditorDialog.this.dispose();
                        return;
                    case JOptionPane.NO_OPTION:
                        editor.undoEdits();
                        super.windowClosing(e);
                        CvPipelineEditorDialog.this.dispose();
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        return;
	            }
	        }
	        else {
                super.windowClosing(e);
                CvPipelineEditorDialog.this.dispose();
	        }
	    }
	});
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