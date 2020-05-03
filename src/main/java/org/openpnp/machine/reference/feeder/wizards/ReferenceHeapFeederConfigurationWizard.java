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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceHeapFeeder;
import org.openpnp.machine.reference.feeder.ReferenceHeapFeeder.DropBox;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceHeapFeederConfigurationWizard
        extends AbstractConfigurationWizard {
    private final ReferenceHeapFeeder feeder;
    private JTextField dropBoxNameTf;

    public ReferenceHeapFeederConfigurationWizard(ReferenceHeapFeeder feeder) {
        this.feeder = feeder;
        
        JPanel HeapPanel = new JPanel();
        HeapPanel.setBorder(new TitledBorder(null, "Heap", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(HeapPanel);
        HeapPanel.setLayout(new BoxLayout(HeapPanel, BoxLayout.Y_AXIS));
        
        JPanel whateverPanel = new JPanel();
        HeapPanel.add(whateverPanel);
        FormLayout fl_whateverPanel = new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,});
        fl_whateverPanel.setColumnGroups(new int[][]{new int[]{4, 6, 8, 10}});
        whateverPanel.setLayout(fl_whateverPanel);
        
        dropBoxNameTf = new JTextField();
        whateverPanel.add(dropBoxNameTf, "6, 2");
        dropBoxNameTf.setColumns(10);
        
        btnNewDropBox = new JButton(newDropBoxAction);
        whateverPanel.add(btnNewDropBox, "8, 2");
        
        btnDeleteDropBox = new JButton(deleteDropBoxAction);
        whateverPanel.add(btnDeleteDropBox, "10, 2");
        
        JPanel panel_1 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panel_1.getLayout();
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(panel_1, "12, 2");
        
        JLabel lblDropBox = new JLabel("DropBox");
        whateverPanel.add(lblDropBox, "2, 2, right, default");
        
        dropBoxCb = new JComboBox<DropBox>();
        dropBoxCb.setMaximumRowCount(4);
        whateverPanel.add(dropBoxCb, "4, 2, 2, 1");
        
        JPanel dropBoxPanel = new JPanel();
        dropBoxPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "DropBox", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(dropBoxPanel);
        FormLayout fl_dropBoxPanel = new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,});
        fl_dropBoxPanel.setColumnGroups(new int[][]{new int[]{4, 6, 8, 10}});
        dropBoxPanel.setLayout(fl_dropBoxPanel);
        
        for (DropBox box : ReferenceHeapFeeder.getDropBoxes()) {
            dropBoxCb.addItem(box);
        }
        
        JLabel lblX_1 = new JLabel("X");
        dropBoxPanel.add(lblX_1, "4, 2");
        
        JLabel lblY_1 = new JLabel("Y");
        dropBoxPanel.add(lblY_1, "6, 2");
        
        JLabel lblZ_1 = new JLabel("Z");
        dropBoxPanel.add(lblZ_1, "8, 2");
        
        JLabel lblDropBoxCenterBottom = new JLabel("Center Bottom");
        dropBoxPanel.add(lblDropBoxCenterBottom, "2, 4, right, default");
        
        TfCenterBottomLocation_x = new JTextField();
        dropBoxPanel.add(TfCenterBottomLocation_x, "4, 4");
        TfCenterBottomLocation_x.setColumns(10);
        
        TfCenterBottomLocation_y = new JTextField();
        dropBoxPanel.add(TfCenterBottomLocation_y, "6, 4");
        TfCenterBottomLocation_y.setColumns(10);
        
        TfCenterBottomLocation_z = new JTextField();
        dropBoxPanel.add(TfCenterBottomLocation_z, "8, 4");
        TfCenterBottomLocation_z.setColumns(10);
        
        dropBoxLocButtons = new LocationButtonsPanel(TfCenterBottomLocation_x, TfCenterBottomLocation_y, TfCenterBottomLocation_z, (JTextField) null);
        FlowLayout flowLayout_5 = (FlowLayout) dropBoxLocButtons.getLayout();
        flowLayout_5.setAlignment(FlowLayout.LEFT);
        dropBoxPanel.add(dropBoxLocButtons, "10, 4, fill, fill");
        
        lblPartsPipeline = new JLabel("Parts Pipeline");
        dropBoxPanel.add(lblPartsPipeline, "2, 6, right, default");
        
        btnEditPartsPipeline = new JButton(actionPipelineEditDropBox);
        dropBoxPanel.add(btnEditPartsPipeline, "4, 6");
        
        btnResetPartsPipeline = new JButton(actionPipelineResetDropBox);
        dropBoxPanel.add(btnResetPartsPipeline, "6, 6");
        
        lblNozzleUnknown = new JLabel("Nozzle Unknown");
        lblNozzleUnknown.setToolTipText("Nozzle for unknown Parts");
        dropBoxPanel.add(lblNozzleUnknown, "2, 8, right, default");
        
        cbNozzleForUnknownParts = new JComboBox();
        dropBoxPanel.add(cbNozzleForUnknownParts, "4, 8, fill, default");
        
        for (NozzleTip tip : Configuration.get().getMachine().getNozzleTips()) {
            cbNozzleForUnknownParts.addItem(tip);
            if ( feeder.getDropBox() != null && feeder.getDropBox().getNozzleTipForUnknown() == tip) {
                cbNozzleForUnknownParts.setSelectedItem(tip);
            }
        }

        
        JLabel lblX = new JLabel("X");
        whateverPanel.add(lblX, "4, 4, center, default");
        
        JLabel lblY = new JLabel("Y");
        whateverPanel.add(lblY, "6, 4, center, default");
        
        JLabel lblZ = new JLabel("Z");
        whateverPanel.add(lblZ, "8, 4, center, default");
        
        JLabel lblHeapCenter = new JLabel("Center (Top)");
        whateverPanel.add(lblHeapCenter, "2, 6, right, default");
        
        TfCenter_x = new JTextField();
        whateverPanel.add(TfCenter_x, "4, 6");
        TfCenter_x.setColumns(10);
        
        TfCenter_y = new JTextField();
        whateverPanel.add(TfCenter_y, "6, 6");
        TfCenter_y.setColumns(10);
        
        TfCenter_z = new JTextField();
        whateverPanel.add(TfCenter_z, "8, 6");
        TfCenter_z.setColumns(10);
        
        centerLocButtons = new LocationButtonsPanel(TfCenter_x, TfCenter_y, TfCenter_z, null);
        FlowLayout flowLayout = (FlowLayout) centerLocButtons.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(centerLocButtons, "10, 6");
        
        JLabel lblMove1 = new JLabel("Move 1");
        whateverPanel.add(lblMove1, "2, 8, right, default");
        
        TfMove1_x = new JTextField();
        TfMove1_x.setColumns(10);
        whateverPanel.add(TfMove1_x, "4, 8, fill, default");
        
        TfMove1_y = new JTextField();
        TfMove1_y.setColumns(10);
        whateverPanel.add(TfMove1_y, "6, 8, fill, default");
        
        TfMove1_z = new JTextField();
        TfMove1_z.setColumns(10);
        whateverPanel.add(TfMove1_z, "8, 8, fill, default");
        
        move1LocButtons = new LocationButtonsPanel(TfMove1_x, TfMove1_y, TfMove1_z, (JTextField) null);
        FlowLayout flowLayout_2 = (FlowLayout) move1LocButtons.getLayout();
        flowLayout_2.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(move1LocButtons, "10, 8, fill, default");
        
        JLabel lblMove2 = new JLabel("Move 2");
        whateverPanel.add(lblMove2, "2, 10, right, default");
        
        TfMove2_x = new JTextField();
        TfMove2_x.setColumns(10);
        whateverPanel.add(TfMove2_x, "4, 10, fill, default");
        
        TfMove2_y = new JTextField();
        TfMove2_y.setColumns(10);
        whateverPanel.add(TfMove2_y, "6, 10, fill, default");
        
        TfMove2_z = new JTextField();
        TfMove2_z.setColumns(10);
        whateverPanel.add(TfMove2_z, "8, 10, fill, default");
        
        move2LocButtons = new LocationButtonsPanel(TfMove2_x, TfMove2_y, TfMove2_z, (JTextField) null);
        FlowLayout flowLayout_3 = (FlowLayout) move2LocButtons.getLayout();
        flowLayout_3.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(move2LocButtons, "10, 10, fill, fill");
        
        JLabel lblMove3 = new JLabel("Move 3");
        whateverPanel.add(lblMove3, "2, 12, right, default");
        
        TfMove3_x = new JTextField();
        TfMove3_x.setColumns(10);
        whateverPanel.add(TfMove3_x, "4, 12, fill, default");
        
        TfMove3_y = new JTextField();
        TfMove3_y.setColumns(10);
        whateverPanel.add(TfMove3_y, "6, 12, fill, default");
        
        TfMove3_z = new JTextField();
        TfMove3_z.setColumns(10);
        whateverPanel.add(TfMove3_z, "8, 12, fill, default");
        
        move3LocButtons = new LocationButtonsPanel(TfMove3_x, TfMove3_y, TfMove3_z, (JTextField) null);
        FlowLayout flowLayout_4 = (FlowLayout) move3LocButtons.getLayout();
        flowLayout_4.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(move3LocButtons, "10, 12, fill, fill");
        
        JLabel lblFeedRetryCount = new JLabel("Feed Retry Count");
        whateverPanel.add(lblFeedRetryCount, "2, 14, right, default");
        
        retryCountTf = new JTextField();
        whateverPanel.add(retryCountTf, "4, 14");
        retryCountTf.setColumns(10);
        
        lblDepth = new JLabel("Depth");
        whateverPanel.add(lblDepth, "6, 14, right, default");
        
        binDepthTf = new JTextField();
        whateverPanel.add(binDepthTf, "8, 14, fill, default");
        binDepthTf.setColumns(10);
        
        JLabel lblPickRetryCount = new JLabel("Pick Retry Count");
        whateverPanel.add(lblPickRetryCount, "2, 16, right, default");
        
        pickRetryCount = new JTextField();
        pickRetryCount.setColumns(10);
        whateverPanel.add(pickRetryCount, "4, 16, fill, default");
        
        lblLastFeedDepth = new JLabel("Last Feed Depth");
        whateverPanel.add(lblLastFeedDepth, "6, 16, right, default");
        
        lastFeedDepthTf = new JTextField();
        whateverPanel.add(lastFeedDepthTf, "8, 16, fill, default");
        lastFeedDepthTf.setColumns(10);
        
        btnResetLastFeedDepth = new JButton(actionLastFeedDepthReset);
        whateverPanel.add(btnResetLastFeedDepth, "10, 16");
        
        JLabel lblFlipAttempts = new JLabel("Max flip attempts");
        whateverPanel.add(lblFlipAttempts, "2, 18, right, default");
        
        maxFlipAttemptsTf = new JTextField();
        maxFlipAttemptsTf.setToolTipText("After this numer of feed, mark the parts as disposable. So the next feed is done with new parts.");
        whateverPanel.add(maxFlipAttemptsTf, "4, 18, fill, default");
        maxFlipAttemptsTf.setColumns(10);
        
        lblVacuumDifference = new JLabel("Vacuum Difference");
        whateverPanel.add(lblVacuumDifference, "6, 18, right, default");
        
        vacuumDifferenceTf = new JTextField();
        whateverPanel.add(vacuumDifferenceTf, "8, 18, fill, default");
        vacuumDifferenceTf.setColumns(10);
        
        JLabel lblPart = new JLabel("Part");
        whateverPanel.add(lblPart, "2, 20, right, default");
        
        partCb = new JComboBox<Part>();
        partCb.setModel(new PartsComboBoxModel());
        partCb.setRenderer(new IdentifiableListCellRenderer<Part>());
        whateverPanel.add(partCb, "4, 20");
        
        lblDetectionPipeline = new JLabel("Detection Pipeline");
        whateverPanel.add(lblDetectionPipeline, "2, 22");
        
        btnEditDetectionPipeline = new JButton(actionPipelineEditFeeder);
        whateverPanel.add(btnEditDetectionPipeline, "4, 22");
        
        btnResetDetectionPipeline = new JButton(actionPipelineResetFeeder);
        whateverPanel.add(btnResetDetectionPipeline, "6, 22");
        
        lblTemplatePipeline = new JLabel("Template Pipeline");
        whateverPanel.add(lblTemplatePipeline, "2, 24");
        
        btnEditTemplatePipeline = new JButton(actionPipelineEditTraining);
        whateverPanel.add(btnEditTemplatePipeline, "4, 24");
        
        btnResetTemplatePipeline = new JButton(actionPipelineResetTraining);
        whateverPanel.add(btnResetTemplatePipeline, "6, 24");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        
        addWrappedBinding(feeder, "feedRetryCount", retryCountTf, "text", intConverter);
        addWrappedBinding(feeder, "pickRetryCount", pickRetryCount, "text", intConverter);
        addWrappedBinding(feeder, "throwAwayDropBoxContentAfterFailedFeeds", maxFlipAttemptsTf, "text", intConverter);
        addWrappedBinding(feeder, "boxDepth", binDepthTf, "text", doubleConverter);
        addWrappedBinding(feeder, "feedFeedDepth", lastFeedDepthTf, "text", doubleConverter);
        addWrappedBinding(feeder, "requiredVacuumDifference", vacuumDifferenceTf, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
        ComponentDecorators.decorateWithAutoSelect(pickRetryCount);
        ComponentDecorators.decorateWithAutoSelect(maxFlipAttemptsTf);
        ComponentDecorators.decorateWithAutoSelect(binDepthTf);
        ComponentDecorators.decorateWithAutoSelect(lastFeedDepthTf);
        ComponentDecorators.decorateWithAutoSelect(vacuumDifferenceTf);

        
        /**
         * Note that we set up the bindings here differently than everywhere else. In most
         * wizards the fields are bound with wrapped bindings and the proxy is bound with a hard
         * binding. Here we do the opposite so that when the user captures a new location
         * it is set on the proxy immediately. This allows the offsets to update immediately.
         * I'm not actually sure why we do it the other way everywhere else, since this seems
         * to work fine. Might not matter in most other cases. 
         */
        MutableLocationProxy binCenterLocation = new MutableLocationProxy();
        addWrappedBinding(feeder, "location", binCenterLocation, "location");
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "lengthX", TfCenter_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "lengthY", TfCenter_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "lengthZ", TfCenter_z, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "rotation", null, "text", doubleConverter);
        bind(UpdateStrategy.READ, binCenterLocation, "location", centerLocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfCenter_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfCenter_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfCenter_z);

        MutableLocationProxy move1Location = new MutableLocationProxy();
        addWrappedBinding(feeder, "way1", move1Location, "location");
        bind(UpdateStrategy.READ_WRITE, move1Location, "lengthX", TfMove1_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move1Location, "lengthY", TfMove1_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move1Location, "lengthZ", TfMove1_z, "text", lengthConverter);
        bind(UpdateStrategy.READ, move1Location, "location", move1LocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove1_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove1_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove1_z);

        MutableLocationProxy move2Location = new MutableLocationProxy();
        addWrappedBinding(feeder, "way2", move2Location, "location");
        bind(UpdateStrategy.READ_WRITE, move2Location, "lengthX", TfMove2_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move2Location, "lengthY", TfMove2_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move2Location, "lengthZ", TfMove2_z, "text", lengthConverter);
        bind(UpdateStrategy.READ, move2Location, "location", move2LocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove2_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove2_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove2_z);
        
        MutableLocationProxy move3Location = new MutableLocationProxy();
        addWrappedBinding(feeder, "way3", move3Location, "location");
        bind(UpdateStrategy.READ_WRITE, move3Location, "lengthX", TfMove3_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move3Location, "lengthY", TfMove3_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move3Location, "lengthZ", TfMove3_z, "text", lengthConverter);
        bind(UpdateStrategy.READ, move3Location, "location", move3LocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove3_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove3_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfMove3_z);
        
        
        /**
         * The strategy for the bank and feeder properties are a little complex:
         * We create an observable wrapper for bank and feeder. We add wrapped bindings
         * for these against the source feeder, so if they are changed, then upon hitting
         * Apply they will be changed on the source.
         * In addition we add non-wrapped bindings from the bank and feeder wrappers to their
         * instance properties such as name and part. Thus they will be updated immediately.
         */
        Wrapper<DropBox> dropBoxWrapper = new Wrapper<>();
        
        addWrappedBinding(feeder, "dropBox", dropBoxWrapper, "value");
        bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value", dropBoxCb, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value.name", dropBoxNameTf, "text")
            .addBindingListener(new AbstractBindingListener() {
                @Override
                public void synced(Binding binding) {
                    SwingUtilities.invokeLater(() -> dropBoxCb.repaint());
                }
            });
        bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value.nozzleTipForUnknown", cbNozzleForUnknownParts, "selectedItem");

        MutableLocationProxy dropBoxLocation = new MutableLocationProxy();
        addWrappedBinding(feeder.getDropBox(), "centerBottomLocation", dropBoxLocation, "location");
        bind(UpdateStrategy.READ_WRITE, dropBoxLocation, "lengthX", TfCenterBottomLocation_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, dropBoxLocation, "lengthY", TfCenterBottomLocation_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, dropBoxLocation, "lengthZ", TfCenterBottomLocation_z, "text", lengthConverter);
        bind(UpdateStrategy.READ, dropBoxLocation, "location", dropBoxLocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfCenterBottomLocation_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfCenterBottomLocation_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(TfCenterBottomLocation_z);
        bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value.centerBottomLocation", dropBoxLocation, "location");
        
        
        partCb.addActionListener(e -> {
            notifyChange();
        });
    }

    private void editFeederPipeline() throws Exception {
        CvPipeline pipeline = feeder.getFeederPipeline();
        pipeline.setProperty("camera", Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        pipeline.setProperty("feeder", feeder);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getPart().getId() + " Feeder-Pipeline", editor);
        dialog.setVisible(true);
}

    private Action actionPipelineEditFeeder = new AbstractAction("Edit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editFeederPipeline();
            });
        }
    };
    
    private Action actionPipelineResetFeeder = new AbstractAction("Reset") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.resetFeederPipeline();
            });
        }
    };

    
    private void editTrainingPipeline() throws Exception {
        CvPipeline pipeline = feeder.getTrainingPipeline();
        pipeline.setProperty("camera", Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        pipeline.setProperty("feeder", feeder);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getPart().getId() + " Training-Pipeline", editor);
        dialog.setVisible(true);
}

    private Action actionPipelineEditTraining= new AbstractAction("Edit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editTrainingPipeline();
            });
        }
    };
    
    private Action actionPipelineResetTraining = new AbstractAction("Reset") {
        @Override        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.resetTrainingPipeline();
            });
        }
    };

    private void editPartsPipeline() throws Exception {
        CvPipeline pipeline = feeder.getDropBox().getPartPipeline();
        pipeline.setProperty("camera", Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        pipeline.setProperty("dropBox", feeder.getDropBox());
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), feeder.getDropBox().getId() + " Part-Pipeline", editor);
        dialog.setVisible(true);
}

    private Action actionPipelineEditDropBox = new AbstractAction("Edit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editPartsPipeline();
            });
        }
    };
    
    private Action actionPipelineResetDropBox = new AbstractAction("Reset") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.getDropBox().resetPartPipeline();
            });
        }
    };
    
    private Action actionLastFeedDepthReset = new AbstractAction("Reset") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                lastFeedDepthTf.setText("0");
                applyAction.actionPerformed(e);
            });
        }
    };

    
    private Action newDropBoxAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            DropBox box = new DropBox();
            ReferenceHeapFeeder.getDropBoxes().add(box);
            dropBoxCb.addItem(box);
            dropBoxCb.setSelectedItem(box);
            dropBoxNameTf.setText("New");
        }
    };
    
    private Action deleteDropBoxAction = new AbstractAction("Delete") {
        @Override
        public void actionPerformed(ActionEvent e) {
            DropBox box = (DropBox) dropBoxCb.getSelectedItem();
            if (ReferenceHeapFeeder.getDropBoxes().size() < 2) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error", "Can't delete the only DropBox. There must always be one DropBox defined.");
            }
            boolean isUsed = false;
            for (Feeder tFeeder: Configuration.get().getMachine().getFeeders()) {
                if (tFeeder != feeder && tFeeder instanceof ReferenceHeapFeeder && ((ReferenceHeapFeeder)tFeeder).getDropBox() == box) {
                    isUsed = true;
                }
            }
            if (isUsed) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error", "Can't delete a DropBox that is in use by other feeder.");
            }
            ReferenceHeapFeeder.getDropBoxes().remove(box);
            dropBoxCb.removeItem(box);
        }
    };

    private JComboBox<DropBox> dropBoxCb;
    private JComboBox<Part> partCb;
    private JTextField TfCenterBottomLocation_x;
    private JTextField TfCenterBottomLocation_y;
    private JTextField TfCenterBottomLocation_z;
    private JTextField TfCenter_x;
    private JTextField TfCenter_y;
    private JTextField TfCenter_z;
    private JTextField retryCountTf;
    private LocationButtonsPanel centerLocButtons;
    private LocationButtonsPanel move1LocButtons;
    private LocationButtonsPanel move2LocButtons;
    private LocationButtonsPanel move3LocButtons;
    private JTextField pickRetryCount;
    private JTextField maxFlipAttemptsTf;
    private JTextField TfMove1_x;
    private JTextField TfMove2_x;
    private JTextField TfMove3_x;
    private JTextField TfMove1_z;
    private JTextField TfMove2_z;
    private JTextField TfMove3_z;
    private JTextField TfMove1_y;
    private JTextField TfMove2_y;
    private JTextField TfMove3_y;
    private JLabel lblDepth;
    private JLabel lblLastFeedDepth;
    private JTextField binDepthTf;
    private JTextField lastFeedDepthTf;
    private JButton btnResetLastFeedDepth;
    private JLabel lblVacuumDifference;
    private JTextField vacuumDifferenceTf;
    private JLabel lblDetectionPipeline;
    private JLabel lblTemplatePipeline;
    private JButton btnEditDetectionPipeline;
    private JButton btnEditTemplatePipeline;
    private JButton btnResetDetectionPipeline;
    private JButton btnResetTemplatePipeline;
    private JLabel lblPartsPipeline;
    private JLabel lblNozzleUnknown;
    private JComboBox<NozzleTip> cbNozzleForUnknownParts;
    private JButton btnEditPartsPipeline;
    private JButton btnResetPartsPipeline;
    private LocationButtonsPanel dropBoxLocButtons;
    private JButton btnNewDropBox;
    private JButton btnDeleteDropBox;
}
