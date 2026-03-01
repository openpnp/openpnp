package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.BoardLocation;
import org.openpnp.spi.Camera;

public class BoardScannerDialog extends JDialog {
    private final BoardLocation boardLocation;
    private final Camera camera;

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JPanel contentPanel;
    private JButton btnSave;

    public BoardScannerDialog(BoardLocation boardLocation, Camera camera) {
        super(MainFrame.get(), "Board Scanner", false);
        this.boardLocation = boardLocation;
        this.camera = camera;

        setSize(800, 600);
        setLocationRelativeTo(MainFrame.get());
        setLayout(new BorderLayout());

        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Initializing selection...");
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        btnSave = new JButton("Save Image...");
        btnSave.setEnabled(false);
        btnSave.addActionListener(e -> saveImage());
        // Initially hidden or added but disabled

        startScan();
    }

    private void startScan() {
        SwingWorker<BufferedImage, Integer> worker = new SwingWorker<BufferedImage, Integer>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                publish(0);
                return scan();
            }

            @Override
            protected void process(List<Integer> chunks) {
                for (int p : chunks) {
                    progressBar.setValue(p);
                    statusLabel.setText("Scanning... " + p + "%");
                }
            }

            @Override
            protected void done() {
                try {
                    BufferedImage result = get();
                    if (result != null) {
                        showResult(result);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    MessageBoxes.errorBox(BoardScannerDialog.this, "Scan Failed", e);
                    dispose();
                }
            }

            private BufferedImage scan() throws Exception {
                return org.openpnp.gui.support.BoardScanner.scanBoard(boardLocation, camera, this::setProgress);
            }
        };
        worker.execute();
    }

    private void showResult(BufferedImage result) {
        statusLabel.setText("Done.");
        progressBar.setVisible(false);
        contentPanel.removeAll();
        JLabel label = new JLabel(new ImageIcon(result));
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(label);
        org.openpnp.util.UiUtils.enableDragPanning(scrollPane);

        // Limit size to 75% of screen or image size, whichever is smaller
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.75);
        int maxHeight = (int) (screenSize.height * 0.75);
        scrollPane.setPreferredSize(new java.awt.Dimension(
                Math.min(result.getWidth(), maxWidth),
                Math.min(result.getHeight(), maxHeight)));

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Add save button at the bottom
        add(btnSave, BorderLayout.SOUTH);
        btnSave.setEnabled(true);
        btnSave.putClientProperty("image", result); // Store image for save action

        pack();
        revalidate();
    }

    private void saveImage() {
        BufferedImage image = (BufferedImage) btnSave.getClientProperty("image");
        if (image == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("board_scan.png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getParentFile(), file.getName() + ".png");
                }
                ImageIO.write(image, "png", file);
            } catch (Exception ex) {
                ex.printStackTrace();
                MessageBoxes.errorBox(this, "Save Failed", ex);
            }
        }
    }
}
