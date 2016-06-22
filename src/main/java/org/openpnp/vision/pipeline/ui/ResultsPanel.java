package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.opencv.core.Mat;
import org.openpnp.gui.support.Icons;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;

public class ResultsPanel extends JPanel {
    private final CvPipelineEditor editor;

    private CvStage selectedStage;
    private Robot robot;

    public ResultsPanel(CvPipelineEditor editor) {
        this.editor = editor;
        
        try {
            robot = new Robot();
        }
        catch (Exception e) {
            
        }

        JSplitPane splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        setLayout(new BorderLayout(0, 0));

        JPanel headerPanel = new JPanel();
        add(headerPanel, BorderLayout.NORTH);
        headerPanel.setLayout(new BorderLayout(0, 0));

        resultStageNameLabel = new JLabel("New label");
        headerPanel.add(resultStageNameLabel, BorderLayout.NORTH);
        resultStageNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panel = new JPanel();
        headerPanel.add(panel, BorderLayout.SOUTH);

        JToolBar toolBar = new JToolBar();
        panel.add(toolBar);

        JButton firstResultButton = new JButton(firstResultAction);
        firstResultButton.setHideActionText(true);
        toolBar.add(firstResultButton);

        JButton previousResultButton = new JButton(previousResultAction);
        previousResultButton.setHideActionText(true);
        toolBar.add(previousResultButton);

        JButton nextResultButton = new JButton(nextResultAction);
        toolBar.add(nextResultButton);

        JButton lastResultButton = new JButton(lastResultAction);
        lastResultButton.setHideActionText(true);
        toolBar.add(lastResultButton);

        JPanel modelPanel = new JPanel();
        modelPanel.setLayout(new BorderLayout(0, 0));

        modelTextPane = new JTextPane();
        modelPanel.add(new JScrollPane(modelTextPane));

        add(splitPane, BorderLayout.CENTER);
        splitPane.setRightComponent(modelPanel);
        splitPane.setDividerLocation(200);

        JPanel panel_1 = new JPanel();
        panel_1.setLayout(new BorderLayout(0, 0));

        matView = new MatView();
        panel_1.add(matView, BorderLayout.CENTER);
        splitPane.setLeftComponent(panel_1);

        JLabel matStatusLabel = new JLabel("New label");
        panel_1.add(matStatusLabel, BorderLayout.SOUTH);
        
        matView.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Color color = robot.getPixelColor(e.getXOnScreen(), e.getYOnScreen());
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                matStatusLabel.setText(String.format("RGB: %03d, %03d, %03d HSB: %03d, %03d, %03d",
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        (int) (255.0 * hsb[0]),
                        (int) (255.0 * hsb[1]),
                        (int) (255.0 * hsb[2])));
            }
        });

        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                splitPane.setDividerLocation(0.8);
            }
        });
    }

    public void refresh() {
        List<CvStage> stages = editor.getPipeline().getStages();
        // If there are no stages we can't have anything selected, so clear it.
        if (stages.isEmpty()) {
            selectedStage = null;
        }
        // Otherwise if nothing is selected or if the previously selected stage is no longer in the
        // pipeline replace the selection with the first stage.
        else if (selectedStage == null || !stages.contains(selectedStage)) {
            selectedStage = stages.get(0);
        }
        updateAllEverything();
    }

    public void setSelectedStage(CvStage stage) {
        this.selectedStage = stage;
        updateAllEverything();
    }

    private void updateAllEverything() {
        List<CvStage> stages = editor.getPipeline().getStages();

        Result result = null;
        Mat image = null;
        Object model = null;
        if (selectedStage != null) {
            result = editor.getPipeline().getResult(selectedStage);
            if (result != null) {
                image = result.image;
                model = result.model;
            }
        }

        if (model instanceof List) {
            String s = "";
            for (Object o : ((List) model)) {
                if (o != null) {
                    s += o.toString();
                }
                s += "\n";
            }
            modelTextPane.setText(s);
        }
        else {
            modelTextPane.setText(model == null ? "" : model.toString());
        }
        matView.setMat(image);
        resultStageNameLabel.setText(result == null || selectedStage == null ? ""
                : (selectedStage.getName() + " (" + (result.processingTimeNs / 1000000.0)
                        + " ms)"));

        if (selectedStage == null) {
            firstResultAction.setEnabled(false);
            previousResultAction.setEnabled(false);
            nextResultAction.setEnabled(false);
            lastResultAction.setEnabled(false);
        }
        else {
            int index = stages.indexOf(selectedStage);
            firstResultAction.setEnabled(index > 0);
            previousResultAction.setEnabled(index > 0);
            nextResultAction.setEnabled(index < stages.size() - 1);
            lastResultAction.setEnabled(index < stages.size() - 1);
        }
    }

    public final Action firstResultAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.navigateFirst);
            putValue(NAME, "");
            putValue(SHORT_DESCRIPTION, "");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<CvStage> stages = editor.getPipeline().getStages();
            selectedStage = stages.get(0);
            updateAllEverything();
        }
    };

    public final Action previousResultAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.navigatePrevious);
            putValue(NAME, "");
            putValue(SHORT_DESCRIPTION, "");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<CvStage> stages = editor.getPipeline().getStages();
            int index = stages.indexOf(selectedStage);
            selectedStage = stages.get(index - 1);
            updateAllEverything();
        }
    };

    public final Action nextResultAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.navigateNext);
            putValue(NAME, "");
            putValue(SHORT_DESCRIPTION, "");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<CvStage> stages = editor.getPipeline().getStages();
            int index = stages.indexOf(selectedStage);
            selectedStage = stages.get(index + 1);
            updateAllEverything();
        }
    };

    public final Action lastResultAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.navigateLast);
            putValue(NAME, "");
            putValue(SHORT_DESCRIPTION, "");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<CvStage> stages = editor.getPipeline().getStages();
            selectedStage = stages.get(stages.size() - 1);
            updateAllEverything();
        }
    };
    private JTextPane modelTextPane;
    private JLabel resultStageNameLabel;
    private MatView matView;
}
