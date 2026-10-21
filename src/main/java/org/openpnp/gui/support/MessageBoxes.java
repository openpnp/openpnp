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

package org.openpnp.gui.support;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.pmw.tinylog.Logger;

public class MessageBoxes {

    /**
     * Localize common English dialog titles so call sites that still pass hardcoded
     * English titles display correctly when the UI locale is not English.
     */
    static String localizeTitle(String title) {
        if (title == null || title.isEmpty()) {
            return Translations.getString("CommonWords.Error"); //$NON-NLS-1$
        }
        switch (title) {
            case "Error": //$NON-NLS-1$
                return Translations.getString("CommonWords.Error"); //$NON-NLS-1$
            case "error": //$NON-NLS-1$
                return Translations.getString("CommonWords.error"); //$NON-NLS-1$
            case "Copy Failed": //$NON-NLS-1$
            case "Copy failed": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.CopyFailed"); //$NON-NLS-1$
            case "Paste Failed": //$NON-NLS-1$
            case "Paste failed": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.PasteFailed"); //$NON-NLS-1$
            case "Import Failed": //$NON-NLS-1$
            case "Import Error": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.ImportFailed"); //$NON-NLS-1$
            case "Export Failed": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.ExportFailed"); //$NON-NLS-1$
            case "Save Error": //$NON-NLS-1$
            case "Save Preferences": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.SaveError"); //$NON-NLS-1$
            case "Validation Error": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.ValidationError"); //$NON-NLS-1$
            case "Camera Error": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.CameraError"); //$NON-NLS-1$
            case "Driver Error": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.DriverError"); //$NON-NLS-1$
            case "Axis Error": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.AxisError"); //$NON-NLS-1$
            case "Reset Failed": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.ResetFailed"); //$NON-NLS-1$
            case "Auto Setup Failure": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.AutoSetupFailure"); //$NON-NLS-1$
            case "Not Yet Implemented": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.NotYetImplemented"); //$NON-NLS-1$
            case "Open Web Browser": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.OpenWebBrowser"); //$NON-NLS-1$
            case "Feeder Error": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.FeederError"); //$NON-NLS-1$
            case "Error: Nozzle Not Deleted": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.NozzleNotDeleted"); //$NON-NLS-1$
            case "Unable to launch update application.": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.UnableToLaunchUpdate"); //$NON-NLS-1$
            case "Warning": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.Warning"); //$NON-NLS-1$
            case "Question": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.Question"); //$NON-NLS-1$
            case "Calibration Complete": //$NON-NLS-1$
                return Translations.getString("MessageBoxes.Title.CalibrationComplete"); //$NON-NLS-1$
            case "Replace file?": //$NON-NLS-1$
                return Translations.getString("DialogMessages.ReplaceFile.title"); //$NON-NLS-1$
            case "Reset to defaults": //$NON-NLS-1$
                return Translations.getString("DialogMessages.ResetToDefaults.title"); //$NON-NLS-1$
            case "Delete all pads?": //$NON-NLS-1$
                return Translations.getString("PackageVisionWizard.Confirm.DeleteAllPads.title"); //$NON-NLS-1$
            case "Closing Pipeline Editor!": //$NON-NLS-1$
                return Translations.getString("CvPipelineEditorDialog.Closing.title"); //$NON-NLS-1$
            default:
                return title;
        }
    }

    // prepare message for use in a message box
    static String prepareMessage(String message) {
        if (message == null) {
            message = "";
        }
        message = message.replace("\r", "").replace("\n", "<br/>");
        if (message.regionMatches(true, 0, "<html>", 0, 6)) {
            message = message.substring(6);
            if (message.toLowerCase().endsWith("</html>")) {
                message = message.substring(0, message.length() - 7);
            }
        }
        message = "<html><body width=\"400\">" + message + "</body></html>";
        return message;
    }

    static class MessageContent extends JPanel {
        final JEditorPane editor;
        final JButton copyButton;
        final JLabel copiedMessage;

        MessageContent(String html, Clipboard clipboard) {
            super(new BorderLayout());
            editor = new JEditorPane("text/html", html);
            editor.setEditable(false);
            editor.setFocusable(true);
            editor.setOpaque(false);
            editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            add(editor, BorderLayout.CENTER);

            copiedMessage = new JLabel(" ");
            add(copiedMessage, BorderLayout.SOUTH);

            copyButton = new JButton(Translations.getString("MessageBoxes.Copy"));
            copyButton.setHorizontalAlignment(SwingConstants.CENTER);
            copyButton.addActionListener(e -> {
                try {
                    Clipboard target = clipboard == null
                            ? Toolkit.getDefaultToolkit().getSystemClipboard()
                            : clipboard;
                    target.setContents(new StringSelection(getPlainText()), null);
                    copiedMessage.setText(Translations.getString("MessageBoxes.CopyDone"));
                }
                catch (RuntimeException ex) {
                    Logger.error(ex, "Unable to copy message dialog text to the clipboard.");
                }
            });
        }

        String getPlainText() {
            try {
                String text = editor.getDocument().getText(0, editor.getDocument().getLength());
                // Swing's HTML document adds one structural newline before the body.
                int structuralOffset = text.startsWith("\n") ? 1 : 0;
                StringBuilder plainText = new StringBuilder(text.substring(structuralOffset));
                HTMLDocument document = (HTMLDocument) editor.getDocument();
                HTMLDocument.Iterator breaks = document.getIterator(HTML.Tag.BR);
                while (breaks.isValid()) {
                    int offset = breaks.getStartOffset() - structuralOffset;
                    if (offset >= 0 && offset < plainText.length()) {
                        plainText.setCharAt(offset, '\n');
                    }
                    breaks.next();
                }
                return plainText.toString();
            }
            catch (BadLocationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static MessageContent createMessageContent(String message, Clipboard clipboard) {
        return new MessageContent(prepareMessage(message), clipboard);
    }

    private static MessageContent createMessageContent(String message) {
        return createMessageContent(message, null);
    }

    private static int showDialog(Component parent, MessageContent content, String title,
            int optionType, int messageType) {
        JOptionPane optionPane = new JOptionPane(content, messageType, optionType);
        JButton acceptButton;
        JButton rejectButton = null;
        switch (optionType) {
            case JOptionPane.YES_NO_OPTION:
                acceptButton = createOptionButton(optionPane, "OptionPane.yesButtonText",
                        "OptionPane.yesButtonMnemonic", JOptionPane.YES_OPTION);
                rejectButton = createOptionButton(optionPane, "OptionPane.noButtonText",
                        "OptionPane.noButtonMnemonic", JOptionPane.NO_OPTION);
                break;
            case JOptionPane.OK_CANCEL_OPTION:
                acceptButton = createOptionButton(optionPane, "OptionPane.okButtonText",
                        "OptionPane.okButtonMnemonic", JOptionPane.OK_OPTION);
                rejectButton = createOptionButton(optionPane, "OptionPane.cancelButtonText",
                        "OptionPane.cancelButtonMnemonic", JOptionPane.CANCEL_OPTION);
                break;
            default:
                acceptButton = createOptionButton(optionPane, "OptionPane.okButtonText",
                        "OptionPane.okButtonMnemonic", JOptionPane.OK_OPTION);
                break;
        }
        Object[] options = rejectButton == null
                ? new Object[] { acceptButton, content.copyButton }
                : new Object[] { acceptButton, rejectButton, content.copyButton };
        optionPane.setOptions(options);
        optionPane.setInitialValue(acceptButton);

        JDialog dialog = optionPane.createDialog(parent, title);
        dialog.setVisible(true);
        dialog.dispose();

        Object selected = optionPane.getValue();
        return selected instanceof Integer ? (Integer) selected : JOptionPane.CLOSED_OPTION;
    }

    static JButton createOptionButton(JOptionPane optionPane, String textKey,
            String mnemonicKey, int value) {
        JButton button = new JButton(UIManager.getString(textKey));
        int mnemonic = UIManager.getInt(mnemonicKey);
        if (mnemonic != 0) {
            button.setMnemonic(mnemonic);
        }
        button.addActionListener(e -> optionPane.setValue(value));
        return button;
    }

    static String prepareThrowableMessage(Throwable cause) {
        String message = null;
        if (cause != null) {
            message = cause.getMessage();
            if (message == null || message.trim().isEmpty()) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                cause.printStackTrace(writer);
                writer.close();
                message = stringWriter.toString();
            }
        }
        if (message == null) {
            message = Translations.getString("MessageBoxes.NoMessageSupplied"); //$NON-NLS-1$
        }
        return message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static boolean errorBox(Component parent, String title, Throwable cause, boolean withContinuation) {
        title = localizeTitle(title);
        boolean ret = false;
        Logger.debug("{}: {}", title, cause);
        MessageContent content = createMessageContent(prepareThrowableMessage(cause));

        // if this errorBox shall ask for Continuation, show a ConfirmDialog and return if the user selected YES
        if (withContinuation) {
            ret = showDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.ERROR_MESSAGE) == JOptionPane.OK_OPTION;
        } else {
            showDialog(parent, content, title, JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE);
        }
        
        return ret;
    }

    public static void errorBox(Component parent, String title, Throwable cause) {
        errorBox(parent, title, cause, false);
    }

    public static void errorBox(Component parent, String title, String message) {
        title = localizeTitle(title);
        if (message == null) {
            message = "";
        }
        Logger.debug("{}: {}", title, message);
        showDialog(parent, createMessageContent(message), title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE);
    }

    public static boolean errorBoxWithRetry(Component parent, String title, String message) {
        title = localizeTitle(title);
        if (message == null) {
            message = "";
        }
        Logger.debug("{}: {}", title, message);
        return showDialog(parent, createMessageContent(message), title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void infoBox(String title, String message) {
        title = localizeTitle(title);
        if (message == null) {
            message = "";
        }
        message = prepareMessage(message);
        infoBox(MainFrame.get(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void infoBox(Component parent, String title, String message, int messageType) {
        showDialog(parent, createMessageContent(message), title, JOptionPane.DEFAULT_OPTION,
                messageType);
    }

    public static void notYetImplemented(Component parent) {
        errorBox(parent,
                Translations.getString("MessageBoxes.Title.NotYetImplemented"), //$NON-NLS-1$
                Translations.getString("MessageBoxes.NotYetImplemented")); //$NON-NLS-1$
    }
}
