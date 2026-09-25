package org.openpnp.gui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;
import org.openpnp.Translations;

class MessageBoxesTest {
    @Test
    void createsSelectableReadOnlyEditorForPlainAndNullMessages() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MessageBoxes.MessageContent plain =
                    MessageBoxes.createMessageContent("A plain message", new Clipboard("test"));
            assertFalse(plain.editor.isEditable());
            assertTrue(plain.editor.isFocusable());
            assertEquals("A plain message", plain.getPlainText());
            assertEquals(2, plain.getComponentCount());
            assertNull(plain.copyButton.getParent());

            MessageBoxes.MessageContent empty =
                    MessageBoxes.createMessageContent(null, new Clipboard("test"));
            assertEquals("", empty.getPlainText());
        });
    }

    @Test
    void rendersMultilineAndHtmlWithoutMarkup() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MessageBoxes.MessageContent multiline =
                    MessageBoxes.createMessageContent("First\nSecond", new Clipboard("test"));
            assertEquals("First\nSecond", multiline.getPlainText());

            MessageBoxes.MessageContent html = MessageBoxes.createMessageContent(
                    "<html><b>Bold</b><br><font color=\"red\">Red</font></html>",
                    new Clipboard("test"));
            assertEquals("Bold\nRed", html.getPlainText());
        });
    }

    @Test
    void throwableTextIsEscapedAndFallbackIncludesStackTrace() {
        assertEquals("bad &lt;value&gt; &amp; detail",
                MessageBoxes.prepareThrowableMessage(
                        new IllegalArgumentException("bad <value> & detail")));

        String fallback = MessageBoxes.prepareThrowableMessage(new Exception());
        assertTrue(fallback.contains("java.lang.Exception"));
        assertTrue(fallback.contains("MessageBoxesTest"));
        assertEquals("No message supplied.", MessageBoxes.prepareThrowableMessage(null));
    }

    @Test
    void copyButtonCopiesCompleteRenderedText() throws Exception {
        Clipboard clipboard = new Clipboard("test");
        SwingUtilities.invokeAndWait(() -> {
            MessageBoxes.MessageContent content =
                    MessageBoxes.createMessageContent("<html>One<br><b>Two</b></html>", clipboard);
            content.editor.select(0, 1);
            assertEquals(" ", content.copiedMessage.getText());
            content.copyButton.doClick();
            assertEquals(0, content.editor.getSelectionStart());
            assertEquals(1, content.editor.getSelectionEnd());
            assertEquals(Translations.getString("MessageBoxes.CopyDone"),
                    content.copiedMessage.getText());
        });

        assertEquals("One\nTwo", clipboard.getData(DataFlavor.stringFlavor));
    }

    @Test
    void standardOptionButtonsPreserveLookAndFeelTextAndKeyboardMnemonic() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JOptionPane optionPane = new JOptionPane();
            JButton okButton = MessageBoxes.createOptionButton(optionPane,
                    "OptionPane.okButtonText", "OptionPane.okButtonMnemonic",
                    JOptionPane.OK_OPTION);

            assertEquals(UIManager.getString("OptionPane.okButtonText"), okButton.getText());
            assertEquals(UIManager.getInt("OptionPane.okButtonMnemonic"), okButton.getMnemonic());
            okButton.doClick();
            assertEquals(JOptionPane.OK_OPTION, optionPane.getValue());
        });
    }
}
