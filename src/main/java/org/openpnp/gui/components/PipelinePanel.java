/*
 * Copyright (C) 2022 <mark@makr.zone>
 * inspired and based on work by
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opencv.core.Mat;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvAbstractParameterStage;
import org.openpnp.vision.pipeline.CvAbstractScalarParameterStage;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.openpnp.vision.pipeline.stages.ParameterInteger;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public abstract class PipelinePanel extends JPanel {
    private CvPipeline pipeline;
    private Map<String, Object> pipelineParameterAssignments;

    private JButton btnEdit;
    private JButton btnReset;
    private boolean editable = true;
    private boolean resetable = true;
    private Timer timer;

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        Object oldValue = this.pipeline;
        this.pipeline = pipeline;
        firePropertyChange("pipeline", oldValue, pipeline);
        invokeRebuildUi();
    }

    private void invokeRebuildUi() {
        if (SwingUtilities.isEventDispatchThread()) {
            rebuildUi();
        }
        else {
            SwingUtilities.invokeLater(() -> { 
                rebuildUi();
            });
        }
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        if (btnEdit != null) { 
            btnEdit.setVisible(editable);
        }
    }

    public boolean isResetable() {
        return resetable;
    }

    public void setResetable(boolean resetable) {
        this.resetable = resetable;
        if (btnReset != null) { 
            btnReset.setVisible(resetable);
        }
    }

    public Map<String, Object> getPipelineParameterAssignments() {
        return pipelineParameterAssignments;
    }

    public void setPipelineParameterAssignments(Map<String, Object> pipelineParameterAssignments) {
        this.pipelineParameterAssignments = pipelineParameterAssignments;
        firePropertyChange("pipelineParameterAssignments", null, this.pipelineParameterAssignments);
        invokeRebuildUi();
    }

    @Override
    public void setEnabled(boolean enabled) {
        for (Component comp : getComponents()) { 
            comp.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    /**
     * Override this method to prepare the pipeline properties. As a minimum the "camera" property must be set.
     * Use getPipeline() and getPipelineParameterAssignments() and pass them to the preparation function.
     * 
     * @throws Exception
     */
    public abstract void preparePipeline() throws Exception;
    /**
     * Override this method to edit the pipeline. 
     * Use getPipeline() and getPipelineParameterAssignments() and pass them to the edit function.
     * You can call the {@link #openPipelineEditor} method handle the Editor dialog and all the 
     * necessary before and after handling.
     * 
     * @throws Exception
     */
    public abstract void editPipeline() throws Exception;
    /**
     * Override this method to reset the pipeline to the default.
     * 
     * @throws Exception
     */
    public abstract void resetPipeline() throws Exception;

    /**
     * Open the Pipeline Editor with all the necessary handling before/after.
     * Including to move the camera or subject (Nozzle) to the right location before the editing takes place. 
     * The user is asked to confirm and can also skip the move. 
     * 
     * @param pipelineTitle
     * @param pipeline
     * @param moveBeforeEditDescription
     * @param movable
     * @param location
     */
    public void openPipelineEditor(String pipelineTitle, CvPipeline pipeline,
            String moveBeforeEditDescription, HeadMountable movable, Location location) {
        UiUtils.confirmMoveToLocationAndAct(getTopLevelAncestor(), 
                moveBeforeEditDescription, 
                movable, 
                location, true, () -> {
                    CvPipelineEditor editor = new CvPipelineEditor(pipeline);
                    CvPipelineEditorDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), pipelineTitle, editor) {

                        @Override
                        public void pipelineChanged() {
                            super.pipelineChanged();
                            // We need to make sure, the settings is recognized as a change, otherwise 
                            // somehow the firePropertyChange() will not be propagated. 
                            setPipeline(pipeline);
                        }
                    };
                    dialog.setVisible(true);
                });
    }

    /**
     * Open the Pipeline Editor with all the necessary handling before/after.
     * 
     * @param pipelineTitle
     * @param pipeline
     */
    public void openPipelineEditor(String pipelineTitle, CvPipeline pipeline) {
        openPipelineEditor(pipelineTitle, pipeline, null, null, null);
    }

    private Object getParameterValue(CvAbstractParameterStage paramStage) {
        if (pipelineParameterAssignments != null 
                && pipelineParameterAssignments.containsKey(paramStage.getParameterName())) {
            return pipelineParameterAssignments.get(paramStage.getParameterName());
        }
        return paramStage.getDefaultValue();
    }

    private void setParameterValue(CvAbstractParameterStage paramStage, Object value) {
        if (value != null
                && !value.equals(getParameterValue(paramStage))) {
            Map<String, Object> newMap = new HashMap<>();
            if (this.pipelineParameterAssignments != null) {
                newMap.putAll(this.pipelineParameterAssignments);
            }
            this.pipelineParameterAssignments = newMap;
            pipelineParameterAssignments.put(paramStage.getParameterName(), value);
            firePropertyChange("pipelineParameterAssignments", null, pipelineParameterAssignments);
            SwingUtilities.invokeLater(() -> previewParameterChangeEffect(paramStage, value));
        }
    }

    protected void previewParameterChangeEffect(CvAbstractParameterStage paramStage, Object value) {
        if (!value.equals(getParameterValue(paramStage))) {
            // Value has changed since the invokeLater call, no point in previewing.
            return;
        }
        boolean hasEffectStage = paramStage.getEffectStageName() != null 
                && !paramStage.getEffectStageName().isEmpty();
        if (hasEffectStage || paramStage.isPreviewResult()) {
            if (timer != null) {
                // If another preview is triggered, while a former preview timer is still running, the former is stopped.  
                timer.stop();
                timer = null;
            }
            // Process the pipeline to show preview images. 
            try (CvPipeline pipeline = getPipeline()) {
                preparePipeline();
                Camera camera = (Camera) pipeline.getProperty("camera");
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera); 
                pipeline.process();
                List<BufferedImage> showImages = new ArrayList<>(); 
                CvStage effectStage = hasEffectStage ? pipeline.getStage(paramStage.getEffectStageName()) : null;
                if (effectStage != null) {
                    Result result = pipeline.getResult(effectStage);
                    if (result != null && result.getImage() != null) {
                        Mat image = OpenCvUtils.toRGB(result.getImage().clone(), result.getColorSpace());
                        showImages.add(OpenCvUtils.toBufferedImage(image));
                        image.release();
                    }
                }
                if (paramStage.isPreviewResult() && pipeline.getWorkingImage() != null) {
                    Mat image = OpenCvUtils.toRGB(pipeline.getWorkingImage().clone(), pipeline.getWorkingColorSpace());
                    showImages.add(OpenCvUtils.toBufferedImage(image));
                    image.release();
                }
                if (showImages.size() > 0) {
                    // Show the first image directly.
                    cameraView.showFilteredImage(showImages.get(0), paramStage.getParameterName()+" = "+paramStage.displayValue(value), 3000);
                    showImages.remove(0);
                    if (showImages.size() > 0) {
                        // Show subsequent images with a timer.
                        timer = new Timer(1000, e -> {
                            cameraView.showFilteredImage(showImages.get(0), 
                                    paramStage.getParameterName()+" = "+paramStage.displayValue(value), 3000);
                            showImages.remove(0);
                            if (showImages.isEmpty()) {
                                // No more images, stop.
                                timer.stop();
                                timer = null;
                            }
                        });
                        timer.start();
                    }
                }
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
    }

    public PipelinePanel() { 
        rebuildUi();
    }

    private RowSpec[] dynamicRowspec(int rows) {
        RowSpec[] rowspec = new RowSpec[rows*2];
        for (int i = 0; i < rows*2; i+=2) {
            rowspec[i] = FormSpecs.RELATED_GAP_ROWSPEC;
            rowspec[i+1] = FormSpecs.DEFAULT_ROWSPEC;
        }
        return rowspec;
    }

    private int invokation = 0;
    private void rebuildUi() {
        removeAll();
        invokation++;
        JPanel panel = this;
        List<CvAbstractParameterStage> parameterStages = getPipeline() != null ? 
                getPipeline().getParameterStages() : new ArrayList<>();
        int rows = 1 + parameterStages.size();
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.UNRELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                getPipeline() == null ? new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC
        } : dynamicRowspec(rows)));

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 2, right, default");

        btnEdit = new JButton("Edit");
        btnEdit.setEnabled(isEnabled());
        btnEdit.setVisible(editable);
        btnEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                UiUtils.messageBoxOnException(() -> editPipeline());
            }
        });
        panel.add(btnEdit, "4, 2");

        btnReset = new JButton("Reset");
        btnReset.setEnabled(isEnabled());
        btnReset.setVisible(resetable);
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> resetPipeline());
            }
        });
        panel.add(btnReset, "6, 2");

        int formRow = 2;
        for (CvAbstractParameterStage parameter : parameterStages) {
            //org.pmw.tinylog.Logger.trace("rebuild "+stage.getParameterName()+" invokation "+invokation);
            if (parameter instanceof CvAbstractScalarParameterStage) {
                CvAbstractScalarParameterStage scalarParameter = (ParameterInteger) parameter;
                try {
                    JLabel lbl = new JLabel(parameter.getParameterName());
                    lbl.setToolTipText(parameter.getParameterDescription());
                    lbl.setEnabled(isEnabled());
                    panel.add(lbl, "2, "+(formRow*2)+", right, default");
                    JSlider slider = new JSlider(JSlider.HORIZONTAL,
                            scalarParameter.getMinimumScalar(), scalarParameter.getMaximumScalar(), 
                            scalarParameter.convertToScalar(getParameterValue(parameter)));
                    slider.setEnabled(isEnabled());
                    slider.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            int value = slider.getValue();
                            // Store as value.
                            setParameterValue(parameter, scalarParameter.convertToValue(value));
                            // Convert back, it may have been sanitized.
                            int newValue = scalarParameter.convertToScalar(getParameterValue(parameter));
                            if (newValue != value) {
                                slider.setValue(newValue);
                            }
                        }
                    });
                    // Just clicking the slider also shows the preview.
                    slider.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            int value = (int) slider.getValue();
                            SwingUtilities.invokeLater(() -> previewParameterChangeEffect(parameter, scalarParameter.convertToValue(value)));
                            super.mousePressed(e);
                        }
                    });
                    slider.setToolTipText(parameter.getParameterDescription());
                    slider.setEnabled(isEnabled());
                    panel.add(slider, "4, "+(formRow*2)+", 3, 1, fill, default");
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
            }
            formRow++;
        }
        revalidate();
        repaint();
    }
}
