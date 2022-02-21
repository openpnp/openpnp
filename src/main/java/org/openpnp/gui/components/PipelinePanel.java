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
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvAbstractParamStage;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.openpnp.vision.pipeline.stages.ExposeParameterInteger;
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

    public abstract void preparePipeline() throws Exception;
    public abstract void editPipeline() throws Exception;
    public abstract void resetPipeline() throws Exception;

    private Object getParameterValue(CvAbstractParamStage paramStage) {
        if (pipelineParameterAssignments != null 
                && pipelineParameterAssignments.containsKey(paramStage.getParameterName())) {
            return pipelineParameterAssignments.get(paramStage.getParameterName());
        }
        return paramStage.getDefaultValue();
    }

    private void setParameterValue(CvAbstractParamStage paramStage, Object value) {
        if (value != null
                && !value.equals(getParameterValue(paramStage))) {
            Map<String, Object> newMap = new HashMap<>();
            newMap.putAll(this.pipelineParameterAssignments);
            this.pipelineParameterAssignments = newMap;
            pipelineParameterAssignments.put(paramStage.getParameterName(), value);
            firePropertyChange("pipelineParameterAssignments", null, pipelineParameterAssignments);
            SwingUtilities.invokeLater(() -> previewParameterChangeEffect(paramStage, value));
        }
    }

    protected void previewParameterChangeEffect(CvAbstractParamStage paramStage, Object value) {
        if (!value.equals(getParameterValue(paramStage))) {
            // Value has changed since the invokeLater call, no point in previewing.
            return;
        }
        boolean hasEffectStage = paramStage.getEffectStageName() != null && !paramStage.getEffectStageName().isEmpty();
        if (paramStage.isPreviewResult()
                || hasEffectStage) {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            try (CvPipeline pipeline = getPipeline()) {
                preparePipeline();
                pipeline.setProperties(pipelineParameterAssignments);
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
                    cameraView.showFilteredImage(showImages.get(0), paramStage.getParameterName()+" = "+value, 3000);
                    showImages.remove(0);
                    if (showImages.size() > 0) {
                        timer = new Timer(1000, e -> {
                            // Parameter was stable all that time: also display the pipeline end image. 
                            cameraView.showFilteredImage(showImages.get(0), 
                                    paramStage.getParameterName()+" = "+value, 3000);
                            showImages.remove(0);
                            if (showImages.isEmpty()) {
                                timer.stop();
                                timer = null;
                            }
                        });
                        timer.start();
                    }
                }
                else if (paramStage.isPreviewResult()) {
                    cameraView.showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), paramStage.getParameterName()+" = "+value, 3000);
                }
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
    }

    public PipelinePanel() { 
        //rebuildUi();
    }

    private RowSpec[] dynamicRowspec(int rows) {
        RowSpec[] rowspec = new RowSpec[rows*2];
        for (int i = 0; i < rows*2; i+=2) {
            rowspec[i] = FormSpecs.RELATED_GAP_ROWSPEC;
            rowspec[i+1] = FormSpecs.DEFAULT_ROWSPEC;
        }
        return rowspec;
    }

    private void rebuildUi() {
        org.pmw.tinylog.Logger.trace("rebuild "+hashCode());
        removeAll();
        List<CvAbstractParamStage> parameterStages = getPipeline() != null ? 
                getPipeline().getParameterStages() : new ArrayList<>();
        int rows = 1 + parameterStages.size();
        setLayout(new FormLayout(new ColumnSpec[] {
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
        add(lblPipeline, "2, 2, right, default");

        btnEdit = new JButton("Edit");
        btnEdit.setEnabled(isEnabled());
        btnEdit.setVisible(editable);
        btnEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                UiUtils.messageBoxOnException(() -> editPipeline());
            }
        });
        add(btnEdit, "4, 2");

        btnReset = new JButton("Reset");
        btnReset.setEnabled(isEnabled());
        btnReset.setVisible(resetable);
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> resetPipeline());
            }
        });
        add(btnReset, "6, 2");

        int formRow = 2;
        for (CvAbstractParamStage stage : parameterStages) {
            org.pmw.tinylog.Logger.trace("rebuild "+stage.getParameterName());
            if (stage instanceof ExposeParameterInteger) {
                ExposeParameterInteger stageInteger = (ExposeParameterInteger) stage;
                try {
                    JLabel lbl = new JLabel(stage.getParameterName());
                    lbl.setToolTipText(stage.getParameterDescription());
                    lbl.setEnabled(isEnabled());
                    add(lbl, "2, "+(formRow*2)+", right, default");
                    int parameterValue = Math.max(stageInteger.getMinimumValue(), 
                            Math.min(stageInteger.getMaximumValue(),
                                    (int)getParameterValue(stage)));
                    JSlider slider = new JSlider(JSlider.HORIZONTAL,
                            stageInteger.getMinimumValue(), stageInteger.getMaximumValue(), 
                            parameterValue);
                    slider.setEnabled(isEnabled());
                    slider.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            int value = (int) slider.getValue();
                            setParameterValue(stage, value);
                            int newValue = (int)getParameterValue(stage);
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
                            SwingUtilities.invokeLater(() -> previewParameterChangeEffect(stage, value));
                            super.mousePressed(e);
                        }
                    });
                    slider.setToolTipText(stage.getParameterDescription());
                    slider.setEnabled(isEnabled());
                    add(slider, "4, "+(formRow*2)+", 3, 1, fill, default");
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
            }
            formRow++;
        }
        SwingUtilities.invokeLater(() -> { validate(); });
    }
}
