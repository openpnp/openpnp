/*
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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.machine.reference.feeder.AdvancedLoosePartFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class AdvancedLoosePartFeederConfigurationWizard
        extends AbstractReferenceFeederConfigurationWizard {
    private final AdvancedLoosePartFeeder feeder;

    public AdvancedLoosePartFeederConfigurationWizard(AdvancedLoosePartFeeder feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JButton btnEditPipeline = new JButton("Edit");
        btnEditPipeline.addActionListener(new BtnEditPipelineActionListener());
        
        JLabel lblFeedPipeline = new JLabel("Feed Pipeline");
        panel.add(lblFeedPipeline, "2, 2");
        panel.add(btnEditPipeline, "4, 2");

        JButton btnResetPipeline = new JButton("Reset");
        btnResetPipeline.addActionListener(new BtnResetPipelineActionListener());
        panel.add(btnResetPipeline, "6, 2");
        
        JLabel lblTrainingPipeline = new JLabel("Training Pipeline");
        panel.add(lblTrainingPipeline, "2, 4");
        
        JButton btnEditTrainingPipeline = new JButton("Edit");
        btnEditTrainingPipeline.addActionListener(new BtnEditTrainingPipelineActionListener());
        panel.add(btnEditTrainingPipeline, "4, 4");
        
        JButton btnResetTrainingPipeline = new JButton("Reset");
        btnResetTrainingPipeline.addActionListener(new BtnResetTrainingPipelineActionListener());
        panel.add(btnResetTrainingPipeline, "6, 4");
        
        JPanel warningPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) warningPanel.getLayout();
        contentPanel.add(warningPanel, 0);
        
        JLabel lblWarningThisFeeder = new JLabel("Warning: This feeder is incomplete and experimental. Use at your own risk.");
        lblWarningThisFeeder.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
        lblWarningThisFeeder.setForeground(Color.RED);
        lblWarningThisFeeder.setHorizontalAlignment(SwingConstants.LEFT);
        warningPanel.add(lblWarningThisFeeder);
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = feeder.getPipeline();
        pipeline.setProperty("camera", Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        pipeline.setProperty("feeder", feeder);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), feeder.getPart().getId() + " Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    private void resetPipeline() {
        feeder.resetPipeline();
    }
    
    private void editTrainingPipeline() throws Exception {
        CvPipeline pipeline = feeder.getTrainingPipeline();
        pipeline.setProperty("camera", Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        pipeline.setProperty("feeder", feeder);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), feeder.getPart().getId() + " Training Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    private void resetTrainingPipeline() {
        feeder.resetTrainingPipeline();
    }
    
    private class BtnEditTrainingPipelineActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editTrainingPipeline();
            });
        }
    }
    private class BtnResetTrainingPipelineActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                resetTrainingPipeline();
            });
        }
    }
    private class BtnEditPipelineActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editPipeline();
            });
        }
    }
    private class BtnResetPipelineActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                resetPipeline();
            });
        }
    }
}
