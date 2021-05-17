package org.openpnp.machine.index.sheets.gui;

import javax.swing.JPanel;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.index.IndexFeeder;
import org.openpnp.model.Part;

import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JTextField;
import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JComboBox;
import org.openpnp.gui.components.LocationButtonsPanel;
import javax.swing.JButton;
import javax.swing.border.EtchedBorder;
import java.awt.Color;

public class FeederConfigurationWizard extends AbstractConfigurationWizard {

	private final IndexFeeder feeder;
	
	private final JLabel hardwareIdValue;
	private final JLabel slotAddressValue;
	private final JComboBox partCb;
	private final JTextField partPitchTf;
	private final JTextField feedRetryCountTf;
	private final JTextField pickRetryCountTf;
	private final JTextField xSlotTf;
	private final JTextField ySlotTf;
	private final JTextField zSlotTf;
	private final JTextField rotSlotTf;
	private final JTextField xOffsetTf;
	private final JTextField yOffsetTf;
	private final JTextField zOffsetTf;
	private final JTextField rotOffsetTf;

	/**
	 * Create the panel.
	 */
	public FeederConfigurationWizard(IndexFeeder feeder) {
		this.feeder = feeder;
		
		JPanel infoPanel = new JPanel();
		contentPanel.add(infoPanel);
		infoPanel.setBorder(new TitledBorder(null, "Info", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		infoPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(50dlu;pref)"),
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.BUTTON_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.PREF_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel hardwareIdLabel = new JLabel("Hardware ID: ");
		infoPanel.add(hardwareIdLabel, "2, 2, left, center");
		
		hardwareIdValue = new JLabel("");
		infoPanel.add(hardwareIdValue, "4, 2, 5, 1, left, center");
		
		JLabel slotAddressLabel = new JLabel("Slot Address:");
		infoPanel.add(slotAddressLabel, "2, 4");

		slotAddressValue = new JLabel("");
		infoPanel.add(slotAddressValue, "4, 4");
		
		JButton findButton = new JButton("Find");
		infoPanel.add(findButton, "6, 4");
		
		JPanel partPanel = new JPanel();
		partPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Part", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPanel.add(partPanel);
		partPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.BUTTON_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(50dlu;default)"),
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel partLabel = new JLabel("Part");
		partPanel.add(partLabel, "2, 2, right, default");

		partCb = new JComboBox();
		partPanel.add(partCb, "4, 2, 5, 1, fill, default");
		partCb.setModel(new PartsComboBoxModel());
        partCb.setRenderer(new IdentifiableListCellRenderer<Part>());
		
		JLabel partPitchLabel = new JLabel("Part Pitch");
		partPanel.add(partPitchLabel, "2, 4, right, default");
		
		partPitchTf = new JTextField();
		partPanel.add(partPitchTf, "4, 4, fill, default");
		partPitchTf.setColumns(10);
		
		JButton feedButton = new JButton("Feed");
		partPanel.add(feedButton, "6, 4");
		
		JLabel feedRetryLabel = new JLabel("Feed Retry Count");
		partPanel.add(feedRetryLabel, "2, 6, right, default");
		
		feedRetryCountTf = new JTextField();
		partPanel.add(feedRetryCountTf, "4, 6, fill, default");
		feedRetryCountTf.setColumns(10);
		
		JLabel pickRetryLabel = new JLabel("Pick Retry Count");
		partPanel.add(pickRetryLabel, "2, 8, right, default");
		
		pickRetryCountTf = new JTextField();
		partPanel.add(pickRetryCountTf, "4, 8, fill, default");
		pickRetryCountTf.setColumns(10);
		
		JPanel locationPanel = new JPanel();
		locationPanel.setBorder(new TitledBorder(null, "Location", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPanel.add(locationPanel);
		locationPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,}));
		
		JLabel xOffsetLabel = new JLabel("X");
		locationPanel.add(xOffsetLabel, "4, 2, center, center");
		
		JLabel yOffsetLabel = new JLabel("Y");
		locationPanel.add(yOffsetLabel, "6, 2, center, center");
		
		JLabel zOffsetLabel = new JLabel("Z");
		locationPanel.add(zOffsetLabel, "8, 2, center, center");
		
		JLabel rotationOffsetLabel = new JLabel("Rotation");
		locationPanel.add(rotationOffsetLabel, "10, 2, center, center");
		
		JLabel slotLocationLabel = new JLabel("Slot Location");
		locationPanel.add(slotLocationLabel, "2, 4, right, center");
		
		xSlotTf = new JTextField();
		locationPanel.add(xSlotTf, "4, 4, left, center");
		xSlotTf.setColumns(10);
		
		ySlotTf = new JTextField();
		locationPanel.add(ySlotTf, "6, 4, left, center");
		ySlotTf.setColumns(10);
		
		zSlotTf = new JTextField();
		locationPanel.add(zSlotTf, "8, 4, left, center");
		zSlotTf.setColumns(10);
		
		rotSlotTf = new JTextField();
		locationPanel.add(rotSlotTf, "10, 4, left, center");
		rotSlotTf.setColumns(10);
		
		LocationButtonsPanel slotLocationPanel = new LocationButtonsPanel(xSlotTf, ySlotTf, zSlotTf, rotSlotTf);
		locationPanel.add(slotLocationPanel, "12, 4, left, top");
		
		JLabel offsetLabel = new JLabel("Part Offset");
		locationPanel.add(offsetLabel, "2, 6, right, center");
		
		xOffsetTf = new JTextField();
		locationPanel.add(xOffsetTf, "4, 6, left, center");
		xOffsetTf.setColumns(10);
		
		yOffsetTf = new JTextField();
		locationPanel.add(yOffsetTf, "6, 6, left, center");
		yOffsetTf.setColumns(10);
		
		zOffsetTf = new JTextField();
		locationPanel.add(zOffsetTf, "8, 6, left, center");
		zOffsetTf.setColumns(10);
		
		rotOffsetTf = new JTextField();
		locationPanel.add(rotOffsetTf, "10, 6, left, center");
		rotOffsetTf.setColumns(10);
		
		LocationButtonsPanel offsetLocationPanel = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, rotOffsetTf);
		locationPanel.add(offsetLocationPanel, "12, 6, left, top");
	}

	@Override
	public void createBindings() {
		IntegerConverter intConverter = new IntegerConverter();
		addWrappedBinding(feeder, "hardwareId", hardwareIdValue, "text");
		addWrappedBinding(feeder, "slotAddress", slotAddressValue, "text", intConverter);

		addWrappedBinding(feeder, "part", partCb, "selectedItem");
		addWrappedBinding(feeder, "feedRetryCount", feedRetryCountTf, "text", intConverter);
		addWrappedBinding(feeder, "pickRetryCount", pickRetryCountTf, "text", intConverter);
	}
}
