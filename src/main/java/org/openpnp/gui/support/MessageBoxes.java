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

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

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
        message = message.replaceAll("\n", "<br/>");
        message = message.replaceAll("\r", "");
        message = "<html><body width=\"400\">" + message + "</body></html>";
        return message;
    }    

    public static boolean errorBox(Component parent, String title, Throwable cause, boolean withContinuation) {
        title = localizeTitle(title);
        String message = null;
        boolean ret = false;
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
        Logger.debug("{}: {}", title, cause);
        message = message.replaceAll("<", "&lt;");
        message = message.replaceAll(">", "&gt;");
        message = prepareMessage(message);

        // if this errorBox shall ask for Continuation, show a ConfirmDialog and return if the user selected YES
        if (withContinuation) {
            ret = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.OK_OPTION;
        } else {
            JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
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
        message = prepareMessage(message);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean errorBoxWithRetry(Component parent, String title, String message) {
        title = localizeTitle(title);
        if (message == null) {
            message = "";
        }
        Logger.debug("{}: {}", title, message);
        message = prepareMessage(message);
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static void infoBox(String title, String message) {
        title = localizeTitle(title);
        if (message == null) {
            message = "";
        }
        message = prepareMessage(message);
        JOptionPane.showMessageDialog(MainFrame.get(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void notYetImplemented(Component parent) {
        errorBox(parent,
                Translations.getString("MessageBoxes.Title.NotYetImplemented"), //$NON-NLS-1$
                Translations.getString("MessageBoxes.NotYetImplemented")); //$NON-NLS-1$
    }
}
