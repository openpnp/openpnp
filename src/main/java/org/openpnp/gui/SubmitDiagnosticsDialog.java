package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FileUtils;
import org.onvif.ver10.device.wsdl.GetSystemSupportInformation;
import org.openpnp.Main;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.imgur.Imgur;
import org.openpnp.imgur.Imgur.Album;
import org.openpnp.imgur.Imgur.Image;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.pmw.tinylog.Logger;

import com.github.kennedyoliveira.pastebin4j.AccountCredentials;
import com.github.kennedyoliveira.pastebin4j.Paste;
import com.github.kennedyoliveira.pastebin4j.PasteBin;
import com.github.kennedyoliveira.pastebin4j.PasteExpiration;
import com.github.kennedyoliveira.pastebin4j.PasteVisibility;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;

public class SubmitDiagnosticsDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JTextArea descriptionTa;
    private JCheckBox includeMachineXmlChk;
    private JCheckBox includePartsXmlChk;
    private JCheckBox includePackagesXmlChk;
    private JCheckBox includeLogChk;
    private JCheckBox includeScreenShotChk;
    private JCheckBox includeVisionChk;
    private JCheckBox includeSystemInfoChk;
    private JCheckBox includeJobChk;
    private JLabel lblSubmitAHelp;
    private JProgressBar progressBar;
    private JButton okButton;
    private JButton cancelButton;
    private Thread thread;

    private BufferedImage screenShot;

    /**
     * Create the dialog.
     */
    public SubmitDiagnosticsDialog() {
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,});
        fl_contentPanel.setColumnGroups(new int[][]{new int[]{4, 2}});
        contentPanel.setLayout(fl_contentPanel);
        {
            lblSubmitAHelp = new JLabel("Submit Diagnostics");
            lblSubmitAHelp.setFont(new Font("Lucida Grande", Font.PLAIN, 24));
            contentPanel.add(lblSubmitAHelp, "2, 2, 3, 1");
        }
        {
            JTextPane txtpnToSubmitA = new JTextPane();
            txtpnToSubmitA.setBackground(UIManager.getColor("Label.background"));
            txtpnToSubmitA.setEditable(false);
            txtpnToSubmitA.setText(
                    "Describe the problem you are experiencing below, select the checkboxes to include content that will help the developers resolve your issue, then click send.\n\nWhen the upload finishes your browser will open to Pastebin. You may be prompted to enter a captcha to complete the process - this is normal. You can then copy the URL to share it.\n\nBe aware that the information you send may be visible to the OpenPnP community, so you should not include private or proprietary information.");
            contentPanel.add(txtpnToSubmitA, "2, 6, 3, 1, fill, fill");
        }
        {
            txtpnWarningIfYou = new JTextPane();
            txtpnWarningIfYou.setText("Warning: If you include a screenshot or Vision Debug Images these images may include output from your machine's cameras. If these images contain content you don't want to share you should uncheck these options. You can review the images from the generated link before sharing it.");
            txtpnWarningIfYou.setForeground(Color.RED);
            txtpnWarningIfYou.setBackground(UIManager.getColor("Label.background"));
            txtpnWarningIfYou.setEditable(false);
            contentPanel.add(txtpnWarningIfYou, "2, 8, 3, 1, fill, fill");
        }
        {
            JLabel lblComments = new JLabel("Please Describe The Issue");
            lblComments.setFont(new Font("Lucida Grande", Font.BOLD, 14));
            contentPanel.add(lblComments, "2, 12");
        }
        {
            descriptionTa = new JTextArea();
            descriptionTa.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
            descriptionTa.setColumns(60);
            descriptionTa.setRows(10);
            contentPanel.add(descriptionTa, "2, 14, 3, 1, fill, fill");
        }
        {
            JLabel lblInclude = new JLabel("Include");
            lblInclude.setFont(new Font("Lucida Grande", Font.BOLD, 14));
            contentPanel.add(lblInclude, "2, 18");
        }
        {
            includeMachineXmlChk = new JCheckBox("machine.xml");
            includeMachineXmlChk.setSelected(true);
            contentPanel.add(includeMachineXmlChk, "2, 20");
        }
        {
            includePartsXmlChk = new JCheckBox("parts.xml");
            includePartsXmlChk.setSelected(true);
            contentPanel.add(includePartsXmlChk, "4, 20");
        }
        {
            includePackagesXmlChk = new JCheckBox("packages.xml");
            includePackagesXmlChk.setSelected(true);
            contentPanel.add(includePackagesXmlChk, "2, 22");
        }
        {
            includeLogChk = new JCheckBox("Latest Log File");
            includeLogChk.setSelected(true);
            contentPanel.add(includeLogChk, "4, 22");
        }
        {
            includeSystemInfoChk = new JCheckBox("Anonymous System Information");
            includeSystemInfoChk.setSelected(true);
            contentPanel.add(includeSystemInfoChk, "2, 24");
        }
        {
            includeJobChk = new JCheckBox("Current Job Data (Job Will Be Saved First)");
            includeJobChk.setSelected(true);
            contentPanel.add(includeJobChk, "4, 24");
        }
        {
            includeScreenShotChk = new JCheckBox("OpenPnP Window Screen Shot");
            includeScreenShotChk.setSelected(true);
            contentPanel.add(includeScreenShotChk, "2, 26");
        }
        {
            includeVisionChk = new JCheckBox("Vision Debug Images (10 Newest)");
            includeVisionChk.setSelected(true);
            contentPanel.add(includeVisionChk, "4, 26");
        }
        {
            progressBar = new JProgressBar();
            progressBar.setStringPainted(true);
            contentPanel.add(progressBar, "2, 30, 3, 1");
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                cancelButton = new JButton(cancelAction);
                buttonPane.add(cancelButton);
            }
            {
                okButton = new JButton(sendAction);
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
        }

        // We have to grab a screenshot before the dialog is shown or the screenshot
        // will include the dialog. So we grab it and if we don't need it it just gets
        // ignored.
        try {
            screenShot = new Robot().createScreenCapture(MainFrame.get().getBounds());
        }
        catch (Exception e) {

        }
    }

    @SuppressWarnings("serial")
    public Action sendAction = new AbstractAction("Send") {
        @Override
        public void actionPerformed(ActionEvent e) {
            /**
             * Hide the dialog Grab screenshot Show the dialog Start send Disable Send Cancel
             * cancels... Update progress bar
             */
            okButton.setEnabled(false);

            thread = new Thread(() -> {
                try {
                    Configuration.get().save();

                    List<File> images = new ArrayList<>();
                    File configDir = Configuration.get().getConfigurationDirectory();
                    File logDir = new File(configDir, "log");
                    File visionDir = new File(logDir, "vision");
                    String content = "";
                    if (includeMachineXmlChk.isSelected()) {
                        content += createPasteStringFromFile(new File(configDir, "machine.xml"));
                    }
                    if (includePartsXmlChk.isSelected()) {
                        content += createPasteStringFromFile(new File(configDir, "parts.xml"));
                    }
                    if (includePackagesXmlChk.isSelected()) {
                        content += createPasteStringFromFile(new File(configDir, "packages.xml"));
                    }
                    if (includeLogChk.isSelected()) {
                        content += createPasteStringFromFile(new File(logDir, "OpenPnP.log"));
                    }
                    if (includeSystemInfoChk.isSelected()) {
                        content += String.format("**** %s ****\n\n%s\n\n", "SystemInfo.txt", getSystemInfo());
                    }
                    if (includeJobChk.isSelected()) {
                        File file = File.createTempFile("OpenPnp-Diagnostics", ".job.xml");
                        Job job = MainFrame.get().getJobTab().getJob();
                        Configuration.get().saveJob(job, file);
                        content += createPasteStringFromFile(file);
                        HashSet<Board> boards = new HashSet<>();
                        for (BoardLocation bl : job.getBoardLocations()) {
                            boards.add(bl.getBoard());
                        }
                        for (Board board : boards) {
                            content += createPasteStringFromFile(board.getFile());
                        }
                    }
                    if (includeScreenShotChk.isSelected() && screenShot != null) {
                        File file = File.createTempFile("OpenPnP-Screenshot", ".png");
                        ImageIO.write(screenShot, "PNG", file);
                        images.add(file);
                    }
                    if (includeVisionChk.isSelected()) {
                        File[] visionFiles = visionDir.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.toLowerCase().endsWith(".png");
                            }
                        });

                        if (visionFiles != null) {
                            Arrays.sort(visionFiles, new Comparator<File>() {
                                public int compare(File f1, File f2) {
                                    return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                                }
                            });

                            for (int i = 0; i < Math.min(visionFiles.length, 10); i++) {
                                images.add(visionFiles[i]);
                            }
                        }
                    }

                    // Image count + the album + the paste
                    progressBar.setMaximum(images.size() + (images.isEmpty() ? 0 : 1) + 1);
                    progressBar.setValue(1);

                    Album album = null;
                    if (!images.isEmpty()) {
                        Imgur imgur = new Imgur(Configuration.get().getImgurClientId());
                        List<Image> albumImages = new ArrayList<>();
                        for (File file : images) {
                            if (Thread.interrupted()) {
                                return;
                            }
                            Image image = imgur.uploadImage(file);
                            albumImages.add(image);
                            progressBar.setValue(progressBar.getValue() + 1);
                        }

                        if (Thread.interrupted()) {
                            return;
                        }
                        album = imgur.createAlbum("OpenPnP Diagnostics Images",
                                albumImages.toArray(new Image[] {}));
                        progressBar.setValue(progressBar.getValue() + 1);
                    }
                    
                    final PasteBin pasteBin =
                            new PasteBin(new AccountCredentials("37ccaf49071a6226ad8f96efdfa9e936"));

                    // Basic creation
                    final Paste paste = new Paste();

                    paste.setTitle("OpenPnP Diagnostics");
                    paste.setExpiration(PasteExpiration.ONE_MONTH);
                    paste.setVisibility(PasteVisibility.UNLISTED);
                    paste.setContent(String.format("OpenPnP Diagnostics\n\nImages: %s\n\nDescription: %s\n\nFiles:\n\n%s",
                            album == null ? "None" : "http://imgur.com/a/" + album.id,
                            descriptionTa.getText(),
                            content));

                    final String url = pasteBin.createPaste(paste);

                    progressBar.setValue(progressBar.getValue() + 1);

                    if (Thread.interrupted()) {
                        return;
                    }
                    Logger.info("Created diagnostics package at: " + url);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(url));
                    }
                    setVisible(false);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    MessageBoxes.errorBox(MainFrame.get(), "Submit Failed", e1);
                    okButton.setEnabled(true);
                }
                thread = null;
            });
            thread.setDaemon(true);
            thread.start();
        }
    };

    @SuppressWarnings("serial")
    public Action cancelAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                if (thread != null) {
                    thread.interrupt();
                    thread.join();
                }
            }
            catch (Exception e) {

            }
            setVisible(false);
        }
    };
    private JTextPane txtpnWarningIfYou;

    static String getSystemInfo() throws Exception {
        StringBuffer sb = new StringBuffer();
        String[] keys = new String[] {"os.name", "os.arch", "java.runtime.name", "java.vm.vendor",
                "java.vm.name", "user.country", "java.runtime.version", "os.version",
                "java.vm.info", "java.version",};
        for (String key : keys) {
            sb.append(String.format("%s: %s\n", key, System.getProperty(key)));
        }
        sb.append(String.format("Memory Total: %.2f\n",
                Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0));
        sb.append(String.format("Memory Free: %.2f\n",
                Runtime.getRuntime().freeMemory() / 1024.0 / 1024.0));
        sb.append(String.format("Memory Max: %.2f\n",
                Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0));
        sb.append(String.format("OpenPnp Version: %s", Main.getVersion()));
        return sb.toString();
    }

    static String createPasteStringFromFile(File file) throws Exception {
        return String.format("**** %s ****\n\n%s\n\n", file.getName(), FileUtils.readFileToString(file));
    }

    public static interface ProgressCallback {
        public void progress(int total, int current, String status);
    }
}
