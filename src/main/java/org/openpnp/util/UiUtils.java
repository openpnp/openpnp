package org.openpnp.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;

import com.google.common.util.concurrent.FutureCallback;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

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
            MessageBoxes.errorBox(MainFrame.get(), "Error", t);
        });
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
            MessageBoxes.errorBox(MainFrame.get(), "Error", e);
        }
    }

    // From http://stackoverflow.com/questions/26854301/control-javafx-tooltip-delay
    public static void bindTooltip(final Node node, final Tooltip tooltip){
        node.setOnMouseMoved(new EventHandler<MouseEvent>(){
           @Override  
           public void handle(MouseEvent event) {
              // +15 moves the tooltip 15 pixels below the mouse cursor;
              // if you don't change the y coordinate of the tooltip, you
              // will see constant screen flicker
              tooltip.show(node, event.getScreenX(), event.getScreenY() + 15);
           }
        });  
        node.setOnMouseExited(new EventHandler<MouseEvent>(){
           @Override
           public void handle(MouseEvent event){
              tooltip.hide();
           }
        });
     }    

}
