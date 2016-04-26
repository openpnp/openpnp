package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.ImageRead;

@SuppressWarnings("serial")
public class StandaloneEditor extends JFrame {
    private JTextField textField;
    private File directory;
    private CvPipelineEditor editor;
    private DefaultListModel<String> listModel;

    public StandaloneEditor() {
        CvPipeline pipeline = new CvPipeline();
        pipeline.add(new ImageRead());
        setTitle("CvPipelineEditor");
        setSize(1328, 1022);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        editor = new CvPipelineEditor(pipeline);
        splitPane.setRightComponent(editor);

        JPanel panel = new JPanel();
        splitPane.setLeftComponent(panel);
        panel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane, BorderLayout.CENTER);

        listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(listModel);
        scrollPane.setViewportView(list);

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.NORTH);
        panel_1.setLayout(new BorderLayout(0, 0));

        textField = new JTextField();
        panel_1.add(textField);
        textField.setColumns(10);

        textField.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputDirectory(new File(textField.getText()));
            }
        });

        JButton browseDirectoryButton = new JButton(new AbstractAction("...") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser j = new JFileChooser();
                j.setSelectedFile(directory);
                j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                j.setMultiSelectionEnabled(false);
                if (j.showOpenDialog(StandaloneEditor.this) == JFileChooser.APPROVE_OPTION) {
                    File directory = j.getSelectedFile();
                    setInputDirectory(directory);
                }
            }
        });

        panel_1.add(browseDirectoryButton, BorderLayout.EAST);

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                String filename = list.getSelectedValue();
                if (filename == null) {
                    return;
                }
                File file = new File(directory, filename);
                if (pipeline.getStages().isEmpty()) {
                    return;
                }
                CvStage stage = pipeline.getStages().get(0);
                if (stage instanceof ImageRead) {
                    ImageRead imageRead = (ImageRead) stage;
                    imageRead.setFile(file);
                    editor.process();
                }
            }
        });

        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                splitPane.setDividerLocation(0.20);
            }
        });


        String defaultInputDirectoryPath =
                Preferences.userNodeForPackage(getClass()).get("inputDirectory", null);
        if (defaultInputDirectoryPath != null) {
            File defaultInputDirectory = new File(defaultInputDirectoryPath);
            if (defaultInputDirectory.exists()) {
                setInputDirectory(defaultInputDirectory);
            }
        }

        setVisible(true);
    }

    private void setInputDirectory(File inputDirectory) {
        textField.setText(inputDirectory.getAbsolutePath());
        directory = new File(textField.getText());
        listModel.clear();
        for (File file : directory.listFiles()) {
            if (!file.isFile()) {
                continue;
            }
            listModel.addElement(file.getName());
        }
        Preferences.userNodeForPackage(getClass()).put("inputDirectory",
                inputDirectory.getAbsolutePath());
    }


    public static void main(String[] args) throws Exception {
        // http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html#//apple_ref/doc/uid/TP40001909-212952-TPXREF134
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            throw new Error(e);
        }
        new StandaloneEditor();
    }
}
