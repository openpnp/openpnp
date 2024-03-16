package org.openpnp.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.SystemUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.pmw.tinylog.Logger;

import com.google.common.util.concurrent.FutureCallback;

public class UiUtils {
    /**
     * Functional interface for a Runnable that can throw an Exception but returns no value. Splits
     * the difference between Runnable and Callable.
     */
    public interface Thrunnable {
        public void thrun() throws Exception;
    }

    /**
     * This extends the exception class by allowing to specify a task to be executed once the user has agreed.
     */
    public static class ExceptionWithContinuation extends Exception {
        private static final long serialVersionUID = 1L;
    
        protected Thrunnable continuation = null;
        
        public ExceptionWithContinuation(Throwable cause, Thrunnable continuation) {
            super(cause);
            this.continuation = continuation;
        }
        
        public ExceptionWithContinuation(String message, Thrunnable continuation) {
            super(message, null);
            this.continuation = continuation;
        }
        
        public Thrunnable getContinuation() {
            return continuation;
        }
    }

    /**
     * Shortcut for submitMachineTask(Callable) which uses a Thrunnable instead. This allows for
     * simple tasks that may throw an Exception but return nothing.
     * 
     * @param thrunnable
     * @return
     */
    public static Future<Void> submitUiMachineTask(final Thrunnable thrunnable) {
        return submitUiMachineTask(() -> {
            thrunnable.thrun();
            return null;
        });
    }

    /**
     * Wrapper for submitMachineTask(Callable, Consumer, Consumer) which ignores the return value in
     * onSuccess and shows a MessageBox when an Exception is thrown. Handy for simple tasks that
     * don't care about the return value but want to notify the user in case of failure. Ideal for
     * running Machine tasks from ActionListeners.
     * 
     * @param callable
     * @return
     */
    public static <T> Future<T> submitUiMachineTask(final Callable<T> callable) {
        return submitUiMachineTask(callable, (result) -> {
        } , (t) -> {
            showError(t);
        });
    }

    /**
     * Show an error using a message box, if the GUI is present, otherwise just log the error.
     * @param t
     */
    public static void showError(Throwable t) {
        
        // Go through all causes, creating a combined continuation
        Thrunnable combinedContinuation = null;
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof ExceptionWithContinuation) {
                Thrunnable continuation = ((ExceptionWithContinuation)cause).getContinuation();
                if (continuation != null) {
                    if (combinedContinuation == null) {
                        combinedContinuation = continuation; // just take the first one
                    } else {
                        Logger.warn("Combined continuation not supported yet.");
                    }
                }
            }
        }
            
        if (MainFrame.get() != null) {
            boolean execContinuation = MessageBoxes.errorBox(MainFrame.get(), "Error", t, combinedContinuation != null);

            // execution continuation, if user agrees
            if (combinedContinuation != null && execContinuation) {
                submitUiMachineTask(combinedContinuation);
            }
        }
        else {
            Logger.error(t);
        }
    }


    /**
     * Functional version of Machine.submit which guarantees that the the onSuccess and onFailure
     * handlers will be run on the Swing event thread.
     * 
     * @param callable
     * @param onSuccess
     * @param onFailure
     * @return
     */
    public static <T> Future<T> submitUiMachineTask(final Callable<T> callable,
            final Consumer<T> onSuccess, final Consumer<Throwable> onFailure) {
        return submitUiMachineTask(callable, onSuccess, onFailure, false);
    }

    /**
     * Functional version of Machine.submit which guarantees that the the onSuccess and onFailure
     * handlers will be run on the Swing event thread. Includes the ignoreEnabled argument.
     * 
     * @param callable
     * @param onSuccess
     * @param onFailure
     * @param ignoreEnabled
     * @return
     */
    public static <T> Future<T> submitUiMachineTask(final Callable<T> callable,
            final Consumer<T> onSuccess, final Consumer<Throwable> onFailure, boolean ignoreEnabled) {
        return Configuration.get().getMachine().submit(callable, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                try {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                try {
                    SwingUtilities.invokeLater(() -> onFailure.accept(t));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, ignoreEnabled);
    }

    /**
     * Functional wrapper for actions that may throw an Exception. Presents an error box to the user
     * with the Exception contents if one is thrown. Basically saves like 5 lines of boilerplate in
     * actions.
     * 
     * @param thrunnable
     */
    public static void messageBoxOnException(Thrunnable thrunnable) {
        try {
            thrunnable.thrun();
        }
        catch (Exception e) {
            showError(e);
        }
    }

    /**
     * Functional wrapper for actions that may throw an Exception. Presents an error box to the user
     * with the Exception contents if one is thrown. The action is performed later on the GUI thread.
     * 
     * @param thrunnable
     */
    public static void messageBoxOnExceptionLater(Thrunnable thrunnable) {
        SwingUtilities.invokeLater(() -> messageBoxOnException(thrunnable));
    }

    /**
     * Some UI actions require the machine to move to a certain location as a prerequisite (e.g. editing a pipeline). 
     * For some actions this might be unexpected for the user. This wrapper asks the user to confirm the move, if not 
     * already at the location. 
     * It also handles a disabled machine and lets the user proceed, if this is an option.
     * It then moves to the location at safe Z, and executes the action thrunnable.
     * The wrapper also handles all the proper GUI/machine task dispatching and shows message boxes on Exceptions.
     * 
     * @param parentComponent 
     * @param moveBeforeActionDescription
     * @param movable
     * @param location
     * @param allowWithoutMove
     * @param actionThrunnable
     */
    public static void confirmMoveToLocationAndAct(Component parentComponent, 
            String moveBeforeActionDescription, HeadMountable movable, 
            Location location, boolean allowWithoutMove,
            final Thrunnable actionThrunnable) {

        messageBoxOnException(() -> {
            if (movable == null || location == null || location.equals(movable.getLocation())) {
                // Already there, just act.
                actionThrunnable.thrun();
            }
            else  {
                confirmMoveToLocationAndAct(parentComponent, moveBeforeActionDescription, allowWithoutMove,
                        () -> {
                            MovableUtils.moveToLocationAtSafeZ(movable, location);
                            MovableUtils.fireTargetedUserAction(movable);
                            movable.waitForCompletion(CompletionType.WaitForStillstand);
                        },
                        actionThrunnable);
            }
        });
    }

    public static void confirmMoveToLocationAndAct(Component parentComponent, 
            String moveBeforeActionDescription, boolean allowWithoutMove,
            final Thrunnable motionThrunnable,
            final Thrunnable actionThrunnable) {
    
        messageBoxOnException(() -> {
            if (moveBeforeActionDescription == null) {
                // No motion given.
                actionThrunnable.thrun();
            }
            else if (Configuration.get().getMachine().isEnabled()) {
                // We need to move there, ask the user to confirm.
                int result;
                if (allowWithoutMove) {
                    if (moveBeforeActionDescription != null) {
                        result = JOptionPane.showConfirmDialog(parentComponent,
                                "Do you want to "+moveBeforeActionDescription+"?\n",
                                null, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    }
                    else {
                        result = JOptionPane.NO_OPTION;
                    }
                }
                else {
                    result = JOptionPane.showConfirmDialog(parentComponent,
                            "About to "+moveBeforeActionDescription+".\n"
                                    +"Do you want to proceed?",
                                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }
                if (result == JOptionPane.YES_OPTION) {
                    // Move wanted.
                    UiUtils.submitUiMachineTask(() -> {
                        motionThrunnable.thrun();
                        UiUtils.messageBoxOnExceptionLater(actionThrunnable);
                    });
                }
                else if (result == JOptionPane.NO_OPTION && allowWithoutMove) {
                    // No move wanted.
                    actionThrunnable.thrun();
                }
            }
            else {
                // We can't move but should.
                if (!allowWithoutMove) {
                    // Just say we can't. 
                    throw new Exception("Machine not enabled, unable to "+moveBeforeActionDescription+".");
                }
                else {
                    // Ask the user if it is OK to proceed without moving. 
                    int result = JOptionPane.showConfirmDialog(parentComponent,
                            "Machine not enabled, unable to "+moveBeforeActionDescription+".\n"
                                    +"Do you want to proceed anyway?",
                                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        actionThrunnable.thrun();
                    }
                }
            }
        });
    }

    /**
     * Browse to the given uri, trying different methods.
     * 
     * @param uri
     */
    public static void browseUri(String uri) {
        UiUtils.messageBoxOnException(() -> {
            Logger.trace("Browse to "+uri);
            try {
                // First try the official desktop method.
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
                else {
                    // No luck, try a more direct & hacky method. 
                    // Adapted from Teocci's https://stackoverflow.com/a/51758886 CC BY-SA 4.0
                    Runtime rt = Runtime.getRuntime();
                    if (SystemUtils.IS_OS_WINDOWS) {
                        rt.exec("rundll32 url.dll,FileProtocolHandler " + uri).waitFor();
                    } 
                    else if (SystemUtils.IS_OS_MAC) {
                        String[] cmd = {"open", uri};
                        rt.exec(cmd).waitFor();
                    } 
                    else {
                        // Default to Unix flavor.
                        // See https://portland.freedesktop.org/doc/xdg-open.html
                        String[] cmd = {"xdg-open", uri};
                        rt.exec(cmd).waitFor();
                    }
                }
            }
            catch (Exception e) {
                try {
                    // Still no luck, at least copy the uri to the clipboard.
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(uri), null);
                    // And tell the user.
                    MessageBoxes.infoBox("Open Web Browser", 
                            "<html>"
                                    + "<p>This platform does not support direct web browsing.</p><br/>"
                                    + "<p>However, the URI was copied to the clipboard, please paste into your favorite browser's address line.</p><br/>"
                                    + "<p><a href=\""+uri+"\">"+uri+"</a></p>"
                                    + "</html>");
                }
                catch (Exception e1) {
                    // Even that failed, nothing left but to lament.
                    throw new Exception("No system support for URI browsing found. See the log. "+uri, e1);
                }
            }
        });
    }
}
