package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.service.GistService;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.Font;

public class HelpRequestDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JTextField emailTf;
    private JTextArea descriptionTa;
    private JCheckBox includeMachineXmlChk;
    private JCheckBox includePartsXmlChk;
    private JCheckBox includePackagesXmlChk;
    private JCheckBox includeLogChk;
    private JCheckBox includeScreenShotChk;
    private JCheckBox includeVisionChk;
    private JCheckBox includeSystemInfoChk;
    private JCheckBox includeJobChk;
    private JPanel panel;
    private JLabel lblSubmitAHelp;

    /**
     * Create the dialog.
     */
    public HelpRequestDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        FormLayout fl_contentPanel = new FormLayout(new ColumnSpec[] {
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.PREF_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
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
        fl_contentPanel.setColumnGroups(new int[][]{new int[]{4, 2}});
        contentPanel.setLayout(fl_contentPanel);
        {
            lblSubmitAHelp = new JLabel("Submit a Help Request");
            lblSubmitAHelp.setFont(new Font("Lucida Grande", Font.PLAIN, 24));
            contentPanel.add(lblSubmitAHelp, "2, 2, 3, 1");
        }
        {
            JLabel lblInstructions = new JLabel("Instructions:");
            contentPanel.add(lblInstructions, "2, 4");
        }
        {
            JTextPane txtpnToSubmitA = new JTextPane();
            txtpnToSubmitA.setBackground(UIManager.getColor("Label.background"));
            txtpnToSubmitA.setEditable(false);
            txtpnToSubmitA.setText(
                    "To submit a request for help please describe the problem you are experiencing below, and select the checkboxes to include content that will help the developers resolve your issue.\n\nYour email address is optional, but please include it so that we can contact you about your request.\n\nBe aware that the information you send will be visible to the OpenPnP community, so you should not include private or proprietary information.");
            contentPanel.add(txtpnToSubmitA, "2, 6, 3, 1, fill, fill");
        }
        {
            panel = new JPanel();
            panel.setBorder(null);
            FlowLayout flowLayout = (FlowLayout) panel.getLayout();
            flowLayout.setAlignment(FlowLayout.LEFT);
            contentPanel.add(panel, "2, 10, 3, 1, left, fill");
            {
                JLabel lblYourEmailAddress = new JLabel("Email Address (Optional):");
                panel.add(lblYourEmailAddress);
            }
            {
                emailTf = new JTextField();
                panel.add(emailTf);
                emailTf.setColumns(25);
            }
        }
        {
            JLabel lblComments = new JLabel("Describe The Issue:");
            contentPanel.add(lblComments, "2, 14");
        }
        {
            descriptionTa = new JTextArea();
            descriptionTa.setColumns(60);
            descriptionTa.setRows(10);
            contentPanel.add(descriptionTa, "2, 16, 3, 1, fill, fill");
        }
        {
            JLabel lblInclude = new JLabel("Include:");
            contentPanel.add(lblInclude, "2, 20");
        }
        {
            includeMachineXmlChk = new JCheckBox("machine.xml");
            includeMachineXmlChk.setSelected(true);
            contentPanel.add(includeMachineXmlChk, "2, 22");
        }
        {
            includePartsXmlChk = new JCheckBox("parts.xml");
            includePartsXmlChk.setSelected(true);
            contentPanel.add(includePartsXmlChk, "4, 22");
        }
        {
            includePackagesXmlChk = new JCheckBox("packages.xml");
            includePackagesXmlChk.setSelected(true);
            contentPanel.add(includePackagesXmlChk, "2, 24");
        }
        {
            includeLogChk = new JCheckBox("Latest Log File");
            includeLogChk.setSelected(true);
            contentPanel.add(includeLogChk, "4, 24");
        }
        {
            includeScreenShotChk = new JCheckBox("OpenPnP Window Screen Shot");
            includeScreenShotChk.setSelected(true);
            contentPanel.add(includeScreenShotChk, "2, 26");
        }
        {
            includeVisionChk = new JCheckBox("Vision Debug Images");
            includeVisionChk.setSelected(true);
            contentPanel.add(includeVisionChk, "4, 26");
        }
        {
            includeSystemInfoChk = new JCheckBox("Anonymous System Information");
            includeSystemInfoChk.setSelected(true);
            contentPanel.add(includeSystemInfoChk, "2, 28");
        }
        {
            includeJobChk = new JCheckBox("Current Job Data");
            includeJobChk.setSelected(true);
            contentPanel.add(includeJobChk, "4, 28");
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setVisible(false);
                    }
                });
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
            {
                JButton okButton = new JButton("Send");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        UiUtils.messageBoxOnException(() -> {
                            Configuration.get().save();
                            Map<String, GistFile> files = new HashMap<>();
                            Map<String, File> images = new HashMap<>();
                            if (includeMachineXmlChk.isSelected()) {
                                GistFile gistFile = new GistFile();
                                File file = new File(Configuration.get().getConfigurationDirectory(), "machine.xml");
                                gistFile.setContent(FileUtils.readFileToString(file));
                                files.put("machine.xml", gistFile);
                            }
                            if (includePartsXmlChk.isSelected()) {
                                GistFile gistFile = new GistFile();
                                File file = new File(Configuration.get().getConfigurationDirectory(), "parts.xml");
                                gistFile.setContent(FileUtils.readFileToString(file));
                                files.put("parts.xml", gistFile);
                            }
                            if (includePackagesXmlChk.isSelected()) {
                                GistFile gistFile = new GistFile();
                                File file = new File(Configuration.get().getConfigurationDirectory(), "packages.xml");
                                gistFile.setContent(FileUtils.readFileToString(file));
                                files.put("packages.xml", gistFile);
                            }
                            if (includeLogChk.isSelected()) {
                                GistFile gistFile = new GistFile();
                                File file = new File(new File(Configuration.get().getConfigurationDirectory(), "log"), "OpenPnP.log");
                                gistFile.setContent(FileUtils.readFileToString(file));
                                files.put("OpenPnP.log", gistFile);
                            }
                            if (includeSystemInfoChk.isSelected()) {
                                StringBuffer sb = new StringBuffer();
                                String[] keys = new String[] {
                                    "os.name",
                                    "os.arch",
                                    "java.runtime.name",
                                    "java.vm.vendor",
                                    "java.vm.name",
                                    "user.country",
                                    "java.runtime.version",
                                    "os.version",
                                    "java.vm.info",
                                    "java.version",
                                };
                                for (String key : keys) {
                                    sb.append(String.format("%s: %s\n", key, System.getProperty(key)));
                                }
                                sb.append(String.format("Memory Total: %.2f\n", Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0));
                                sb.append(String.format("Memory Free: %.2f\n", Runtime.getRuntime().freeMemory() / 1024.0 / 1024.0));
                                sb.append(String.format("Memory Max: %.2f\n", Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0));
                                GistFile gistFile = new GistFile();
                                gistFile.setContent(sb.toString());
                                files.put("SystemInfo.txt", gistFile);
                            }
                            if (includeScreenShotChk.isSelected()) {
                                setVisible(true);
                                BufferedImage img = new Robot().createScreenCapture(MainFrame.get().getBounds());
                            }
                            if (includeJobChk.isSelected()) {
                                
                            }
                            if (includeVisionChk.isSelected()) {
                                
                            }

                            Gist gist = new Gist();
                            String email = emailTf.getText() == null || emailTf.getText().trim().length() == 0 ? "Anonymous" : emailTf.getText();
                            gist.setDescription(String.format("%s\n\nSubmitted by: %s",  descriptionTa.getText(), email));
                            gist.setFiles(files);
                            gist.setPublic(false);

                            GistService service = new GistService();
                            gist = service.createGist(gist);
                            System.out.println(gist.getHtmlUrl());
                        });
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
        }
    }
    
    public static void main(String[] args) {
        HelpRequestDialog dialog = new HelpRequestDialog();
        dialog.setModal(true);
        dialog.setSize(500, 700);
        dialog.setLocationRelativeTo(MainFrame.get());
        dialog.setVisible(true);
    }
}
