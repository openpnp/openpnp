/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * Copyright (C) 2021 Johannes Formann <johannes-openpnp@formann.de>

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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
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
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Binding;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableComparator;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferenceHeapFeeder;
import org.openpnp.machine.reference.feeder.ReferenceHeapFeeder.DropBox;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.SwingConstants;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class ReferenceHeapFeederConfigurationWizard
        extends AbstractConfigurationWizard {
    private final ReferenceHeapFeeder feeder;
    private JTextField dropBoxNameTf;
    private MutableLocationProxy dropBoxLocation;
    private MutableLocationProxy dropBoxDropLocation;
    
    @SuppressWarnings("rawtypes")
    public class PartsComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
        private IdentifiableComparator<Part> comparator = new IdentifiableComparator<>();

        public PartsComboBoxModel() {
            addAllElements();
            Configuration.get().addPropertyChangeListener("parts", this);
        }

        @SuppressWarnings("unchecked")
        private void addAllElements() {
            ArrayList<Part> parts = new ArrayList<>(Configuration.get().getParts());
            Collections.sort(parts, comparator);
            for (Part part : parts) {
                addElement(part);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Object selected = getSelectedItem();
            removeAllElements();
            addAllElements();
            setSelectedItem(selected);
        }
    }

    
    public ReferenceHeapFeederConfigurationWizard(ReferenceHeapFeeder feeder) {
        this.feeder = feeder;
        
        JPanel heapPanel = new JPanel();
        heapPanel.setBorder(new TitledBorder(null, "Heap", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(heapPanel);
        heapPanel.setLayout(new BoxLayout(heapPanel, BoxLayout.Y_AXIS));
        
        JPanel whateverPanel = new JPanel();
        heapPanel.add(whateverPanel);
        FormLayout fl_whateverPanel = new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.MIN_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,},
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
        whateverPanel.add(panel_1, "11, 2");
        
        JLabel lblDropBox = new JLabel("DropBox");
        whateverPanel.add(lblDropBox, "2, 2, right, default");
        
        dropBoxCb = new JComboBox<DropBox>();
        dropBoxCb.setMaximumRowCount(4);
        whateverPanel.add(dropBoxCb, "4, 2, 2, 1");
        
        JPanel dropBoxPanel = new JPanel();
        dropBoxPanel.setBorder(new TitledBorder(null, "DropBox", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(dropBoxPanel);
        FormLayout fl_dropBoxPanel = new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.MIN_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,},
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
                FormSpecs.RELATED_GAP_ROWSPEC,});
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
        
        tfCenterBottomLocation_x = new JTextField();
        dropBoxPanel.add(tfCenterBottomLocation_x, "4, 4");
        tfCenterBottomLocation_x.setColumns(10);
        
        tfCenterBottomLocation_y = new JTextField();
        dropBoxPanel.add(tfCenterBottomLocation_y, "6, 4");
        tfCenterBottomLocation_y.setColumns(10);
        
        tfCenterBottomLocation_z = new JTextField();
        dropBoxPanel.add(tfCenterBottomLocation_z, "8, 4");
        tfCenterBottomLocation_z.setColumns(10);
        
        dropBoxLocButtons = new LocationButtonsPanel(tfCenterBottomLocation_x, tfCenterBottomLocation_y, tfCenterBottomLocation_z, (JTextField) null);
        FlowLayout flowLayout_5 = (FlowLayout) dropBoxLocButtons.getLayout();
        flowLayout_5.setAlignment(FlowLayout.LEFT);
        dropBoxPanel.add(dropBoxLocButtons, "10, 4, default, fill");
        
        lblDropLocation = new JLabel("Drop Location");
        dropBoxPanel.add(lblDropLocation, "2, 6");
        
        tfDropLocation_x = new JTextField();
        dropBoxPanel.add(tfDropLocation_x, "4, 6, fill, default");
        tfDropLocation_x.setColumns(10);
        
        tfDropLocation_y = new JTextField();
        dropBoxPanel.add(tfDropLocation_y, "6, 6, fill, default");
        tfDropLocation_y.setColumns(10);
        
        tfDropLocation_z = new JTextField();
        dropBoxPanel.add(tfDropLocation_z, "8, 6, fill, default");
        tfDropLocation_z.setColumns(10);
        
        dropBoxDropLocButtons = new LocationButtonsPanel(tfDropLocation_x, tfDropLocation_y, tfDropLocation_z, (JTextField) null);
        FlowLayout fl_dropBoxDropLocButtons = (FlowLayout) dropBoxDropLocButtons.getLayout();
        fl_dropBoxDropLocButtons.setAlignment(FlowLayout.LEFT);
        dropBoxPanel.add(dropBoxDropLocButtons, "10, 6, left, fill");
        
        lblPartsPipeline = new JLabel("Parts Pipeline");
        dropBoxPanel.add(lblPartsPipeline, "2, 8, right, default");
        
        btnEditPartsPipeline = new JButton(actionPipelineEditDropBox);
        dropBoxPanel.add(btnEditPartsPipeline, "4, 8");
        
        btnResetPartsPipeline = new JButton(actionPipelineResetDropBox);
        dropBoxPanel.add(btnResetPartsPipeline, "6, 8");
        
        lblDummyPart = new JLabel("Dummy Part");
        lblDummyPart.setToolTipText("");
        dropBoxPanel.add(lblDummyPart, "2, 10, right, default");
        
        cbDummyPartForUnknownParts = new JComboBox<Part>();
        cbDummyPartForUnknownParts.setModel(new PartsComboBoxModel());
        cbDummyPartForUnknownParts.setRenderer(new IdentifiableListCellRenderer<Part>());
        cbDummyPartForUnknownParts.setToolTipText("Dummy part for moving unknown parts (e.g. to the trash). Is also used to determine the used nozzle.");
        dropBoxPanel.add(cbDummyPartForUnknownParts, "4, 10, fill, default");
        
        btnCleanDropbox = new JButton(actionCleanDropbox);
        dropBoxPanel.add(btnCleanDropbox, "10, 10");
                
        JLabel lblX = new JLabel("X");
        whateverPanel.add(lblX, "4, 4, center, default");
        
        JLabel lblY = new JLabel("Y");
        whateverPanel.add(lblY, "6, 4, center, default");
        
        JLabel lblZ = new JLabel("Z");
        whateverPanel.add(lblZ, "8, 4, center, default");
        
        JLabel lblHeapCenter = new JLabel("Center (Top)");
        whateverPanel.add(lblHeapCenter, "2, 6, right, default");
        
        tfCenter_x = new JTextField();
        whateverPanel.add(tfCenter_x, "4, 6");
        tfCenter_x.setColumns(10);
        
        tfCenter_y = new JTextField();
        whateverPanel.add(tfCenter_y, "6, 6");
        tfCenter_y.setColumns(10);
        
        tfCenter_z = new JTextField();
        whateverPanel.add(tfCenter_z, "8, 6");
        tfCenter_z.setColumns(10);
        
        centerLocButtons = new LocationButtonsPanel(tfCenter_x, tfCenter_y, tfCenter_z, null);
        FlowLayout flowLayout = (FlowLayout) centerLocButtons.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(centerLocButtons, "10, 6");
        
        JLabel lblMove1 = new JLabel("Move 1");
        whateverPanel.add(lblMove1, "2, 8, right, default");
        
        tfMove1_x = new JTextField();
        tfMove1_x.setColumns(10);
        whateverPanel.add(tfMove1_x, "4, 8, fill, default");
        
        tfMove1_y = new JTextField();
        tfMove1_y.setColumns(10);
        whateverPanel.add(tfMove1_y, "6, 8, fill, default");
        
        move1LocButtons = new LocationButtonsPanel(tfMove1_x, tfMove1_y, null, null);
        FlowLayout flowLayout_2 = (FlowLayout) move1LocButtons.getLayout();
        flowLayout_2.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(move1LocButtons, "10, 8, fill, default");
        
        JLabel lblMove2 = new JLabel("Move 2");
        whateverPanel.add(lblMove2, "2, 10, right, default");
        
        tfMove2_x = new JTextField();
        tfMove2_x.setColumns(10);
        whateverPanel.add(tfMove2_x, "4, 10, fill, default");
        
        tfMove2_y = new JTextField();
        tfMove2_y.setColumns(10);
        whateverPanel.add(tfMove2_y, "6, 10, fill, default");
        
        move2LocButtons = new LocationButtonsPanel(tfMove2_x, tfMove2_y, null, null);
        FlowLayout flowLayout_3 = (FlowLayout) move2LocButtons.getLayout();
        flowLayout_3.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(move2LocButtons, "10, 10, fill, fill");
        
        JLabel lblMove3 = new JLabel("Move 3");
        whateverPanel.add(lblMove3, "2, 12, right, default");
        
        tfMove3_x = new JTextField();
        tfMove3_x.setColumns(10);
        whateverPanel.add(tfMove3_x, "4, 12, fill, default");
        
        tfMove3_y = new JTextField();
        tfMove3_y.setColumns(10);
        whateverPanel.add(tfMove3_y, "6, 12, fill, default");
        
        move3LocButtons = new LocationButtonsPanel(tfMove3_x, tfMove3_y, null, null);
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
        
        chckbxPokeForParts = new JCheckBox("Poke for Parts");
        chckbxPokeForParts.setToolTipText("If enabled the nozzle is lifted for each move inside the heap. Reduces the risk to damage (large) parts, but slower.");
        whateverPanel.add(chckbxPokeForParts, "8, 20");
        
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
        
        btnGetSamples = new JButton(actionGetSamples);
        whateverPanel.add(btnGetSamples, "10, 24");
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
        addWrappedBinding(feeder, "lastFeedDepth", lastFeedDepthTf, "text", doubleConverter);
        addWrappedBinding(feeder, "requiredVacuumDifference", vacuumDifferenceTf, "text", intConverter);
        addWrappedBinding(feeder, "part", partCb, "selectedItem");
        addWrappedBinding(feeder, "pokeForParts", chckbxPokeForParts, "selected");


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
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "lengthX", tfCenter_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "lengthY", tfCenter_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "lengthZ", tfCenter_z, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, binCenterLocation, "rotation", null, "text", doubleConverter);
//        bind(UpdateStrategy.READ, binCenterLocation, "location", centerLocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfCenter_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfCenter_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfCenter_z);

        MutableLocationProxy move1Location = new MutableLocationProxy();
        addWrappedBinding(feeder, "way1", move1Location, "location");
        bind(UpdateStrategy.READ_WRITE, move1Location, "lengthX", tfMove1_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move1Location, "lengthY", tfMove1_y, "text", lengthConverter);
//        bind(UpdateStrategy.READ, move1Location, "location", move1LocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfMove1_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfMove1_y);

        MutableLocationProxy move2Location = new MutableLocationProxy();
        addWrappedBinding(feeder, "way2", move2Location, "location");
        bind(UpdateStrategy.READ_WRITE, move2Location, "lengthX", tfMove2_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move2Location, "lengthY", tfMove2_y, "text", lengthConverter);
//        bind(UpdateStrategy.READ, move2Location, "location", move2LocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfMove2_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfMove2_y);
        
        MutableLocationProxy move3Location = new MutableLocationProxy();
        addWrappedBinding(feeder, "way3", move3Location, "location");
        bind(UpdateStrategy.READ_WRITE, move3Location, "lengthX", tfMove3_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, move3Location, "lengthY", tfMove3_y, "text", lengthConverter);
//        bind(UpdateStrategy.READ, move3Location, "location", move3LocButtons, "baseLocation");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfMove3_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfMove3_y);
        
        
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
        bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value.dummyPartForUnknown", cbDummyPartForUnknownParts, "selectedItem");

        dropBoxLocation = new MutableLocationProxy();
        addWrappedBinding(feeder, "dropBoxLocation", dropBoxLocation, "location");
        bind(UpdateStrategy.READ_WRITE, dropBoxLocation, "lengthX", tfCenterBottomLocation_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, dropBoxLocation, "lengthY", tfCenterBottomLocation_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, dropBoxLocation, "lengthZ", tfCenterBottomLocation_z, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfCenterBottomLocation_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfCenterBottomLocation_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfCenterBottomLocation_z);
        //bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value.centerBottomLocation", dropBoxLocation, "location");
        
        dropBoxDropLocation = new MutableLocationProxy();
        addWrappedBinding(feeder, "dropBoxDropLocation", dropBoxDropLocation, "location");
        bind(UpdateStrategy.READ_WRITE, dropBoxDropLocation, "lengthX", tfDropLocation_x, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, dropBoxDropLocation, "lengthY", tfDropLocation_y, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, dropBoxDropLocation, "lengthZ", tfDropLocation_z, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfDropLocation_x);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfDropLocation_y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfDropLocation_z);
        //bind(UpdateStrategy.READ_WRITE, dropBoxWrapper, "value.dropLocation", dropBoxDropLocation, "location");

        
        dropBoxCb.addActionListener(e -> {
            notifyChange();
            dropBoxLocation.setLocation(((DropBox)dropBoxCb.getSelectedItem()).getCenterBottomLocation());
            dropBoxDropLocation.setLocation(((DropBox)dropBoxCb.getSelectedItem()).getDropLocation());
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
        CvPipeline pipeline = ((DropBox)dropBoxCb.getSelectedItem()).getPartPipeline();
        pipeline.setProperty("camera", Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        pipeline.setProperty("dropBox", ((DropBox)dropBoxCb.getSelectedItem()));
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), ((DropBox)dropBoxCb.getSelectedItem()).getId() + " Part-Pipeline", editor);
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
                ((DropBox)dropBoxCb.getSelectedItem()).resetPartPipeline();
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

    private Action actionCleanDropbox = new AbstractAction("Clean DropBox") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                //Feeder feeder = getSelection();
                feeder.getDropBox().clean(Configuration.get().getMachine().getHeads().get(0).getDefaultNozzle());
                });
        }
    };

    private Action actionGetSamples = new AbstractAction("GetSamples") {
        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                //Feeder feeder = getSelection();
                feeder.getSamples(Configuration.get().getMachine().getHeads().get(0).getDefaultNozzle());
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
    private JTextField tfCenterBottomLocation_x;
    private JTextField tfCenterBottomLocation_y;
    private JTextField tfCenterBottomLocation_z;
    private JTextField tfCenter_x;
    private JTextField tfCenter_y;
    private JTextField tfCenter_z;
    private JTextField retryCountTf;
    private LocationButtonsPanel centerLocButtons;
    private LocationButtonsPanel move1LocButtons;
    private LocationButtonsPanel move2LocButtons;
    private LocationButtonsPanel move3LocButtons;
    private JTextField pickRetryCount;
    private JTextField maxFlipAttemptsTf;
    private JTextField tfMove1_x;
    private JTextField tfMove2_x;
    private JTextField tfMove3_x;
    private JTextField tfMove1_y;
    private JTextField tfMove2_y;
    private JTextField tfMove3_y;
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
    private JLabel lblDummyPart;
    private JComboBox<Part> cbDummyPartForUnknownParts;
    private JButton btnEditPartsPipeline;
    private JButton btnResetPartsPipeline;
    private LocationButtonsPanel dropBoxLocButtons;
    private JButton btnNewDropBox;
    private JButton btnDeleteDropBox;
    private JButton btnCleanDropbox;
    private JButton btnGetSamples;
    private JTextField tfDropLocation_x;
    private JTextField tfDropLocation_y;
    private JTextField tfDropLocation_z;
    private JLabel lblDropLocation;
    private LocationButtonsPanel dropBoxDropLocButtons;
    private JCheckBox chckbxPokeForParts;
}
