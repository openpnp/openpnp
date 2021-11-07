package org.openpnp.util;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner.CompletionType;

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
     * Show an error using a message box.
     * @param t
     */
    public static void showError(Throwable t) {
        MessageBoxes.errorBox(MainFrame.get(), "Error", t);
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
        });
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
            if (location.equals(movable.getLocation())) {
                // Already there, just act.
                actionThrunnable.thrun();
            }
            else if (Configuration.get().getMachine().isEnabled()) {
                // We need to move there, ask the user to confirm.
                int result;
                if (allowWithoutMove) {
                    result = JOptionPane.showConfirmDialog(parentComponent,
                            "Do you want to "+moveBeforeActionDescription+"?\n",
                                    null, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
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
                        MovableUtils.moveToLocationAtSafeZ(movable, location);
                        MovableUtils.fireTargetedUserAction(movable);
                        movable.waitForCompletion(CompletionType.WaitForStillstand);
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
}
