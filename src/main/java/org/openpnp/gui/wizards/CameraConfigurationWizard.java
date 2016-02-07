/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.gui.wizards;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class CameraConfigurationWizard extends AbstractConfigurationWizard {
	private final Camera camera;
	private JPanel panelUpp;
	private JButton btnMeasure;
	private JButton btnCancelMeasure;
    private JLabel lblUppInstructions;
	
	public CameraConfigurationWizard(Camera camera) {
		this.camera = camera;

		panelUpp = new JPanel();
		contentPanel.add(panelUpp);
		panelUpp.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Units Per Pixel", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelUpp.setLayout(new FormLayout(new ColumnSpec[] {
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
		        FormSpecs.DEFAULT_COLSPEC,},
		    new RowSpec[] {
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,
		        FormSpecs.RELATED_GAP_ROWSPEC,
		        FormSpecs.DEFAULT_ROWSPEC,}));
		
		lblWidth = new JLabel("Width");
		panelUpp.add(lblWidth, "2, 2");
		
		lblHeight = new JLabel("Height");
		panelUpp.add(lblHeight, "4, 2");
		
		lblX = new JLabel("X");
		panelUpp.add(lblX, "6, 2");
		
		lblY = new JLabel("Y");
		panelUpp.add(lblY, "8, 2");
		
		textFieldWidth = new JTextField();
		textFieldWidth.setText("1");
		panelUpp.add(textFieldWidth, "2, 4");
		textFieldWidth.setColumns(8);
		
		textFieldHeight = new JTextField();
		textFieldHeight.setText("1");
		panelUpp.add(textFieldHeight, "4, 4");
		textFieldHeight.setColumns(8);
		
		textFieldUppX = new JTextField();
		textFieldUppX.setColumns(8);
		panelUpp.add(textFieldUppX, "6, 4, fill, default");
		
		textFieldUppY = new JTextField();
		textFieldUppY.setColumns(8);
		panelUpp.add(textFieldUppY, "8, 4, fill, default");
		
		btnMeasure = new JButton("Measure");
		btnMeasure.setAction(measureAction);
		panelUpp.add(btnMeasure, "10, 4");

		btnCancelMeasure = new JButton("Cancel");
		btnCancelMeasure.setAction(cancelMeasureAction);
		panelUpp.add(btnCancelMeasure, "12, 4");
		
		lblUppInstructions = new JLabel("<html>\n<ol>\n<li>Place an object with a known width and height on the table. Graphing paper is a good, easy choice for this.\n<li>Enter the width and height of the object into the Width and Height fields.\n<li>Jog the camera to where it is centered over the object and in focus.\n<li>Press Measure and use the camera selection rectangle to measure the object. Press Confirm when finished.\n<li>The calculated units per pixel values will be inserted into the X and Y fields.\n</ol>\n</html>");
		panelUpp.add(lblUppInstructions, "2, 6, 10, 1, default, fill");
		
		panelVision = new JPanel();
		panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPanel.add(panelVision);
		panelVision.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		lblSettleTimems = new JLabel("Settle Time (ms)");
		panelVision.add(lblSettleTimems, "2, 2, right, default");
		
		textFieldSettleTime = new JTextField();
		panelVision.add(textFieldSettleTime, "4, 2, fill, default");
		textFieldSettleTime.setColumns(10);
	}

	@Override
	public void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
        LongConverter longConverter = new LongConverter();
        
		MutableLocationProxy unitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixel", unitsPerPixel, "location");
        addWrappedBinding(unitsPerPixel, "lengthX", textFieldUppX, "text", lengthConverter);
        addWrappedBinding(unitsPerPixel, "lengthY", textFieldUppY, "text", lengthConverter);
        
        addWrappedBinding(camera, "settleTimeMs", textFieldSettleTime, "text", longConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldUppY);
        
        ComponentDecorators.decorateWithAutoSelect(textFieldWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldHeight);
        ComponentDecorators.decorateWithAutoSelect(textFieldSettleTime);
	}
	
	private Action measureAction = new AbstractAction("Measure") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(confirmMeasureAction);
			cancelMeasureAction.setEnabled(true);
			CameraView cameraView = MainFrame.cameraPanel.setSelectedCamera(camera);
			cameraView.setSelectionEnabled(true);
			cameraView.setSelection(0, 0, 100, 100);
		}
	};
	
	private Action confirmMeasureAction = new AbstractAction("Confirm") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(measureAction);
			cancelMeasureAction.setEnabled(false);
			CameraView cameraView = MainFrame.cameraPanel.getCameraView(camera);
			cameraView.setSelectionEnabled(false);
			Rectangle selection = cameraView.getSelection();
            double width = Double.parseDouble(textFieldWidth.getText());
            double height = Double.parseDouble(textFieldHeight.getText());
			textFieldUppX.setText(String.format(Locale.US,Configuration.get().getLengthDisplayFormat(), (width / selection.width)));
			textFieldUppY.setText(String.format(Locale.US,Configuration.get().getLengthDisplayFormat(), (height / selection.height)));
		}
	};
	
	private Action cancelMeasureAction = new AbstractAction("Cancel") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			btnMeasure.setAction(measureAction);
			cancelMeasureAction.setEnabled(false);
			CameraView cameraView = MainFrame.cameraPanel.getCameraView(camera);
			cameraView.setSelectionEnabled(false);
		}
	};
	private JTextField textFieldWidth;
	private JTextField textFieldHeight;
	private JTextField textFieldUppX;
	private JTextField textFieldUppY;
	private JLabel lblWidth;
	private JLabel lblHeight;
	private JLabel lblX;
	private JLabel lblY;
	private JPanel panelVision;
	private JLabel lblSettleTimems;
	private JTextField textFieldSettleTime;
}