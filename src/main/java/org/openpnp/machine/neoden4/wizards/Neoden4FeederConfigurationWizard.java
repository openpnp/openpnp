package org.openpnp.machine.neoden4.wizards;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.BufferedImageIconConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.neoden4.Neoden4Feeder;
import org.openpnp.machine.reference.feeder.wizards.AbstractReferenceFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.python.modules.thread.thread;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class Neoden4FeederConfigurationWizard extends AbstractReferenceFeederConfigurationWizard {
    private final Neoden4Feeder feeder;

    private JLabel lblActuatorId;
    private JButton btnActuateFeeder;
    private JTextField textFieldActuatorId;
    private JPanel panelOther;
    private JPanel panelVision;
    private JCheckBox chckbxVisionEnabled;
    private JPanel panelVisionEnabled;
    private JPanel panelTemplate;
    private JLabel labelTemplateImage;
    private JButton btnChangeTemplateImage;
    private JSeparator separator;
    private JPanel panelVisionTemplateAndAoe;
    private JPanel panelAoE;
    private JLabel lblX_1;
    private JLabel lblY_1;
    private JTextField textFieldAoiX;
    private JTextField textFieldAoiY;
    private JTextField textFieldAoiWidth;
    private JTextField textFieldAoiHeight;
    private JTextField textFieldFeedCount;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;
    private JLabel lblWidth;
    private JLabel lblHeight;
    private JButton btnChangeAoi;
    private JButton btnCancelChangeAoi;
    private JPanel panel;
    private JButton btnCancelChangeTemplateImage;
    private JButton btnResetVisionOffsets;
    
    public Neoden4FeederConfigurationWizard(Neoden4Feeder feeder) {
    	super(feeder, true); //JR
        this.feeder = feeder;

        
        
        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

        panelOther = new JPanel();
        panelOther.setBorder(new TitledBorder(null, "Other", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        panelFields.add(panelOther);
        panelOther.setLayout(new FormLayout(
                new ColumnSpec[] {
                		FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        
        lblActuatorId = new JLabel("Actuator Name");
        panelOther.add(lblActuatorId, "2, 2, right, default");

        textFieldActuatorId = new JTextField();
        panelOther.add(textFieldActuatorId, "4, 2");
        textFieldActuatorId.setColumns(5);
        
        btnActuateFeeder = new JButton(actuateFeederAction);
        panelOther.add(btnActuateFeeder, "6, 2");
        btnActuateFeeder.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblFeedCount = new JLabel("Feed Count");
        panelOther.add(lblFeedCount, "2, 4, right, default");
        
        textFieldFeedCount = new JTextField();
        panelOther.add(textFieldFeedCount, "4, 4, fill, default");
        textFieldFeedCount.setColumns(10);
        

        JButton btnResetFeedCount = new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldFeedCount.setText("0");
            }
        });
        btnResetFeedCount.setHorizontalAlignment(SwingConstants.LEFT);
        panelOther.add(btnResetFeedCount, "6, 4, left, default");
        
        //
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelFields.add(panelVision);
        panelVision.setLayout(new BoxLayout(panelVision, BoxLayout.Y_AXIS));

        panelVisionEnabled = new JPanel();
        FlowLayout fl_panelVisionEnabled = (FlowLayout) panelVisionEnabled.getLayout();
        fl_panelVisionEnabled.setAlignment(FlowLayout.LEFT);
        panelVision.add(panelVisionEnabled);

        chckbxVisionEnabled = new JCheckBox("Vision Enabled?");
        panelVisionEnabled.add(chckbxVisionEnabled);

        separator = new JSeparator();
        panelVision.add(separator);

        panelVisionTemplateAndAoe = new JPanel();
        panelVision.add(panelVisionTemplateAndAoe);
        panelVisionTemplateAndAoe.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.LABEL_COMPONENT_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        panelTemplate = new JPanel();
        panelTemplate.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Template Image", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelVisionTemplateAndAoe.add(panelTemplate, "2, 2, center, fill");
        panelTemplate.setLayout(new BoxLayout(panelTemplate, BoxLayout.Y_AXIS));

        labelTemplateImage = new JLabel("");
        labelTemplateImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelTemplate.add(labelTemplateImage);
        labelTemplateImage.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        labelTemplateImage.setMinimumSize(new Dimension(150, 150));
        labelTemplateImage.setMaximumSize(new Dimension(150, 150));
        labelTemplateImage.setHorizontalAlignment(SwingConstants.CENTER);
        labelTemplateImage.setSize(new Dimension(150, 150));
        labelTemplateImage.setPreferredSize(new Dimension(150, 150));

        panel = new JPanel();
        panelTemplate.add(panel);

        btnChangeTemplateImage = new JButton(selectTemplateImageAction);
        panel.add(btnChangeTemplateImage);
        btnChangeTemplateImage.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnCancelChangeTemplateImage = new JButton(cancelSelectTemplateImageAction);
        panel.add(btnCancelChangeTemplateImage);

        panelAoE = new JPanel();
        panelAoE.setBorder(new TitledBorder(null, "Area of Interest", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelVisionTemplateAndAoe.add(panelAoE, "4, 2, fill, fill");
        panelAoE.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        }));

        lblX_1 = new JLabel("X");
        panelAoE.add(lblX_1, "2, 2");

        lblY_1 = new JLabel("Y");
        panelAoE.add(lblY_1, "4, 2");

        lblWidth = new JLabel("Width");
        panelAoE.add(lblWidth, "6, 2");

        lblHeight = new JLabel("Height");
        panelAoE.add(lblHeight, "8, 2");

        textFieldAoiX = new JTextField();
        panelAoE.add(textFieldAoiX, "2, 4, fill, default");
        textFieldAoiX.setColumns(5);

        textFieldAoiY = new JTextField();
        panelAoE.add(textFieldAoiY, "4, 4, fill, default");
        textFieldAoiY.setColumns(5);

        textFieldAoiWidth = new JTextField();
        panelAoE.add(textFieldAoiWidth, "6, 4, fill, default");
        textFieldAoiWidth.setColumns(5);

        textFieldAoiHeight = new JTextField();
        panelAoE.add(textFieldAoiHeight, "8, 4, fill, default");
        textFieldAoiHeight.setColumns(5);

        btnChangeAoi = new JButton("Change");
        btnChangeAoi.setAction(selectAoiAction);
        panelAoE.add(btnChangeAoi, "10, 4");

        btnCancelChangeAoi = new JButton("Cancel");
        btnCancelChangeAoi.setAction(cancelSelectAoiAction);
        panelAoE.add(btnCancelChangeAoi, "12, 4");

        cancelSelectTemplateImageAction.setEnabled(false);
        cancelSelectAoiAction.setEnabled(false);

        btnResetVisionOffsets = new JButton("Reset offsets");
        btnResetVisionOffsets.setAction(resetVisionOffsets);
        panelAoE.add(btnResetVisionOffsets, "12, 10");

        contentPanel.add(panelFields);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        
        IntegerConverter intConverter = new IntegerConverter();
        BufferedImageIconConverter imageConverter = new BufferedImageIconConverter();
        
        addWrappedBinding(feeder, "actuatorName", textFieldActuatorId, "text");
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);
        
        addWrappedBinding(feeder, "vision.enabled", chckbxVisionEnabled, "selected");
        addWrappedBinding(feeder, "vision.templateImage", labelTemplateImage, "icon", imageConverter);

        addWrappedBinding(feeder, "vision.areaOfInterest.x", textFieldAoiX, "text", intConverter);
        addWrappedBinding(feeder, "vision.areaOfInterest.y", textFieldAoiY, "text", intConverter);
        addWrappedBinding(feeder, "vision.areaOfInterest.width", textFieldAoiWidth, "text", intConverter);
        addWrappedBinding(feeder, "vision.areaOfInterest.height", textFieldAoiHeight, "text", intConverter);
        
        ComponentDecorators.decorateWithAutoSelect(textFieldActuatorId);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiX);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiY);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldAoiHeight);

        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedStart, "actuatorName");
        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedEnd, "actuatorName");
    }

    private Action selectTemplateImageAction = new AbstractAction("Select") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);

                cameraView.setSelectionEnabled(true);
                // org.openpnp.model.Rectangle r =
                // feeder.getVision().getTemplateImageCoordinates();
                org.openpnp.model.Rectangle r = null;
                if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
                    cameraView.setSelection(0, 0, 100, 100);
                }
                else {
                    // cameraView.setSelection(r.getLeft(), r.getTop(),
                    // r.getWidth(), r.getHeight());
                }
                btnChangeTemplateImage.setAction(confirmSelectTemplateImageAction);
                cancelSelectTemplateImageAction.setEnabled(true);
            });
        }
    };

    private Action confirmSelectTemplateImageAction = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);

                BufferedImage image = cameraView.captureSelectionImage();
                if (image == null) {
                    MessageBoxes.errorBox(MainFrame.get(),
                            "No Image Selected",
                            "Please select an area of the camera image using the mouse.");
                }
                else {
                    labelTemplateImage.setIcon(new ImageIcon(image));
                }
                cameraView.setSelectionEnabled(false);
                btnChangeTemplateImage.setAction(selectTemplateImageAction);
                cancelSelectTemplateImageAction.setEnabled(false);
            });
        }
    };

    private Action cancelSelectTemplateImageAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);

                btnChangeTemplateImage.setAction(selectTemplateImageAction);
                cancelSelectTemplateImageAction.setEnabled(false);
                cameraView.setSelectionEnabled(false);
            });
        }
    };


	private Action actuateFeederAction = new AbstractAction("Actuate") {
		@Override
		public void actionPerformed(ActionEvent arg0) {

			UiUtils.messageBoxOnException(() -> {
				String actuatorName = feeder.getActuatorName();
				Actuator actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);

				if (actuator == null) {
					MessageBoxes.errorBox(contentPanel, "Error",
							String.format("Can't find actuator '%s'", actuatorName));
				} else {

					UiUtils.submitUiMachineTask(() -> {
						// Actuate actuator
						actuator.actuate(feeder.getPart().getPitchInTape());

						// Refresh camera after 1s
						Camera cam = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
						if (cam != null) {
							Timer timer = new Timer(1000, new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									try {
										cam.capture();
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								}
							});
							timer.setRepeats(false);
							timer.start();
						}
					});
				}
			});
		}
	};
	
    private Action selectAoiAction = new AbstractAction("Select") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);

                btnChangeAoi.setAction(confirmSelectAoiAction);
                cancelSelectAoiAction.setEnabled(true);

                cameraView.setSelectionEnabled(true);
                org.openpnp.model.Rectangle r = feeder.getVision().getAreaOfInterest();
                if (r == null || r.getWidth() == 0 || r.getHeight() == 0) {
                    cameraView.setSelection(0, 0, 100, 100);
                }
				else {
					// Convert coordinate origin back to top left
					cameraView.setSelection(
							r.getX() + (camera.getHeight() / 2), 
							r.getY() + (camera.getHeight() / 2), 
							r.getWidth(), 
							r.getHeight());
				}
			});
        }
    };

    private Action confirmSelectAoiAction = new AbstractAction("Confirm") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			UiUtils.messageBoxOnException(() -> {
				Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead().getDefaultCamera();
				CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);

				btnChangeAoi.setAction(selectAoiAction);
				cancelSelectAoiAction.setEnabled(false);

				cameraView.setSelectionEnabled(false);
				final Rectangle rect = cameraView.getSelection();

				// Convert ROI origin to center instead of top left corner,
				// because of Neoden4Camera resolution changes
				rect.x = rect.x - (camera.getHeight() / 2); // - (rect.width/2);
				rect.y = rect.y - (camera.getHeight() / 2); // - (rect.height/2);

				feeder.getVision()
						.setAreaOfInterest(new org.openpnp.model.Rectangle(rect.x, rect.y, rect.width, rect.height));

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						textFieldAoiX.setText(Integer.toString(rect.x));
						textFieldAoiY.setText(Integer.toString(rect.y));
						textFieldAoiWidth.setText(Integer.toString(rect.width));
						textFieldAoiHeight.setText(Integer.toString(rect.height));
					}
				});
			});
		}
    };

    private Action cancelSelectAoiAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);

                btnChangeAoi.setAction(selectAoiAction);
                cancelSelectAoiAction.setEnabled(false);
                btnChangeAoi.setAction(selectAoiAction);
                cancelSelectAoiAction.setEnabled(false);
                cameraView.setSelectionEnabled(false);
            });
        }
    };

    private Action resetVisionOffsets = new AbstractAction("Reset vision offsets") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
				feeder.resetVisionOffsets();
            });
        }
    };
}