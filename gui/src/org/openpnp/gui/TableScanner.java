package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.openpnp.gui.support.CameraItem;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JTextPane;

/**
 * Scans the table, saving a photo from the camera at each position.
 * 
 * Plan:
 * Tell the user to home the machine in camera coordinates. 
 * Prompt user for bottom left corner and top right corner of area to scan.
 * Prompt user for head to use. Get the camera based on the head. Probably
 * safe to just use head 0 and whatever camera is attached to it.
 * Prompt for directory to save in.
 * 
 */
public class TableScanner extends JDialog implements Runnable {
	private final Frame frame;
	private final Configuration configuration;
	
	private JTextField txtStartX;
	private JTextField txtStartY;
	private JTextField txtEndX;
	private JTextField txtEndY;
	private JTextField txtOutputDirectory;
	private JComboBox cmbCameras;
	private JProgressBar progressBar;
	private JButton btnStart;
	
	private boolean cancelled = false;
	
	public TableScanner(Frame frame, Configuration configuration) {
		super(frame, "Table Scanner");
		
		this.frame = frame;
		this.configuration = configuration;
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		panel.add(panel_2);
		
		btnStart = new JButton(startAction);
		panel_2.add(btnStart);
		
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		panel.add(progressBar, BorderLayout.NORTH);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JTextPane txtpnSelectThe = new JTextPane();
		txtpnSelectThe.setEditable(false);
		txtpnSelectThe.setText("1. Select the camera to be used.\n2. Select an empty output directory for the resulting images.\n3. Jog the camera to the bottom left corner of the area to be scanned,\nthen press the Start Position Set button.\n4. Jog the camera to the upper right corner of the area to be scanned,\nthen press the End Position Set button.\nThe End Position values should be greater than the Start Position values.\n5. Press the Start button.\n");
		panel_1.add(txtpnSelectThe, "2, 2, 7, 1, fill, fill");
		
		JLabel lblX = new JLabel("X");
		panel_1.add(lblX, "4, 4");
		
		JLabel lblY = new JLabel("Y");
		panel_1.add(lblY, "6, 4");
		
		JLabel label = new JLabel("Start Position");
		panel_1.add(label, "2, 6, right, default");
		
		txtStartX = new JTextField();
		txtStartX.setText("0.000");
		panel_1.add(txtStartX, "4, 6, fill, default");
		txtStartX.setColumns(9);
		
		txtStartY = new JTextField();
		txtStartY.setText("0.000");
		panel_1.add(txtStartY, "6, 6, fill, default");
		txtStartY.setColumns(9);
		
		JButton btnSetStart = new JButton(setStartAction);
		panel_1.add(btnSetStart, "8, 6");
		
		JLabel lblEndPosition = new JLabel("End Position");
		panel_1.add(lblEndPosition, "2, 8, right, default");
		
		txtEndX = new JTextField();
		txtEndX.setText("0.000");
		panel_1.add(txtEndX, "4, 8, fill, default");
		txtEndX.setColumns(9);
		
		txtEndY = new JTextField();
		txtEndY.setText("0.000");
		panel_1.add(txtEndY, "6, 8, fill, default");
		txtEndY.setColumns(9);
		
		JButton btnSetEnd = new JButton(setEndAction);
		panel_1.add(btnSetEnd, "8, 8");
		
		JSeparator separator = new JSeparator();
		panel_1.add(separator, "2, 10, 5, 1");
		
		JLabel lblCamera = new JLabel("Camera");
		panel_1.add(lblCamera, "2, 12, right, default");
		
		cmbCameras = new JComboBox();
		panel_1.add(cmbCameras, "4, 12, 3, 1");
		
		JLabel lblOutputDirectory = new JLabel("Output Directory");
		panel_1.add(lblOutputDirectory, "2, 14, right, default");
		
		txtOutputDirectory = new JTextField();
		panel_1.add(txtOutputDirectory, "4, 14, 3, 1, fill, default");
		txtOutputDirectory.setColumns(10);
		
		JButton btnBrowse = new JButton(browseAction);
		panel_1.add(btnBrowse, "8, 14, left, default");
		
		for (Camera camera : configuration.getMachine().getCameras()) {
			cmbCameras.addItem(new CameraItem(camera));
		}
	}
	
	public void run() {
		try {
			Machine machine = configuration.getMachine();
			Camera camera = ((CameraItem) cmbCameras.getSelectedItem()).getCamera();
			Head head = camera.getHead();
			// TODO: Make sure the units are native
			double startX = Double.parseDouble(txtStartX.getText());
			double startY = Double.parseDouble(txtStartY.getText());
			double endX = Double.parseDouble(txtEndX.getText());
			double endY = Double.parseDouble(txtEndY.getText());
			// Calculate bounding box
			double width = Math.abs(endX - startX);
			double height = Math.abs(endY - startY);
			// Determine how many images are needed
			BufferedImage image = camera.capture();
			// Figure out how many units per image we are getting
			Location unitsPerPixel = camera.getUnitsPerPixel();
			unitsPerPixel = unitsPerPixel.convertToUnits(machine.getNativeUnits());
			double imageWidthInUnits = unitsPerPixel.getX() * image.getWidth();
			double imageHeightInUnits = unitsPerPixel.getY() * image.getHeight();
			System.out.println(String.format("Images are %d, %d pixels, %2.3f, %2.3f %s", 
					image.getWidth(),
					image.getHeight(),
					imageWidthInUnits,
					imageHeightInUnits,
					unitsPerPixel.getUnits().getShortName()));
			int widthInImages = (int) (width / (imageWidthInUnits / 2));
			int heightInImages = (int) (height / (imageHeightInUnits / 2));
			int totalImages = (widthInImages * heightInImages);
			System.out.println(String.format("Need to capture %d x %d images for a total of %d", 
					widthInImages, 
					heightInImages, 
					totalImages));
			// Start loop, checking for cancelled
			File outputDirectory = new File(txtOutputDirectory.getText());
			if (!outputDirectory.exists()) {
				throw new Exception("Output directory does not exist.");
			}
			if (startX >= endX) {
				throw new Exception("End Position X must be greater than End Position X");
			}
			if (startY >= endY) {
				throw new Exception("End Position Y must be greater than End Position Y");
			}
			progressBar.setMinimum(0);
			progressBar.setMaximum(totalImages - 1);
			progressBar.setValue(0);
			int currentImageX = 0, currentImageY = 0, currentImage = 0;
			while (!cancelled) {
				head.moveTo(
						startX + ((imageWidthInUnits / 2) * currentImageX), 
						startY + ((imageHeightInUnits / 2) * currentImageY), 
						head.getZ(), 
						head.getC());
				// Give the head and camera 500ms to settle
				Thread.sleep(500);
				// We capture two images to make sure that the one we save is
				// not coming from a previous frame.
				image = camera.capture();
				image = camera.capture();
				File outputFile = new File(outputDirectory,
						String.format("%2.3f,%2.3f.png", head.getX(), head.getY()));
				ImageIO.write(image, "png",outputFile);
				progressBar.setValue(currentImage);
				currentImage++;
				currentImageX++;
				if (currentImageX == widthInImages) {
					currentImageX = 0;
					currentImageY++;
					if (currentImageY == heightInImages) {
						break;
					}
				}
			}
		}
		catch (Exception e) {
			MessageBoxes.errorBox(frame, "Scan Error", e.getMessage());
			e.printStackTrace();
		}
		btnStart.setAction(startAction);
	}
	
	private Action browseAction = new AbstractAction("Browse") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser fileDialog = new JFileChooser();
			fileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileDialog.showSaveDialog(frame);
			File file = fileDialog.getSelectedFile();
			txtOutputDirectory.setText(file.getAbsolutePath());
		}
	};
	
	private Action startAction = new AbstractAction("Start") {
		@Override
		public void actionPerformed(ActionEvent e) {
			btnStart.setAction(stopAction);
			cancelled = false;
			new Thread(TableScanner.this).start();
		}
	};
	
	private Action stopAction = new AbstractAction("Cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			cancelled = true;
			btnStart.setAction(startAction);
		}
	};
	
	private Action setStartAction = new AbstractAction("Set") {
		@Override
		public void actionPerformed(ActionEvent e) {
			Camera camera = ((CameraItem) cmbCameras.getSelectedItem()).getCamera();
			Head head = camera.getHead();
			txtStartX.setText(String.format("%2.3f", head.getX()));
			txtStartY.setText(String.format("%2.3f", head.getY()));
		}
	};
	
	private Action setEndAction = new AbstractAction("Set") {
		@Override
		public void actionPerformed(ActionEvent e) {
			Camera camera = ((CameraItem) cmbCameras.getSelectedItem()).getCamera();
			Head head = camera.getHead();
			txtEndX.setText(String.format("%2.3f", head.getX()));
			txtEndY.setText(String.format("%2.3f", head.getY()));
		}
	};
	
}
