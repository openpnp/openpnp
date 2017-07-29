package org.openpnp.vision.pipeline.ui.editors;

import com.l2fprod.common.annotations.EditorRegistry;
import com.l2fprod.common.beans.editor.StringConverterPropertyEditor;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.model.Length;

@EditorRegistry(
        type = {Length.class}
)
public class LengthEditor extends StringConverterPropertyEditor {
    private LengthConverter lengthConverter = new LengthConverter();

    public LengthEditor() {
    }

    protected Object convertFromString(String text) {
        return lengthConverter.convertReverse(text);
    }
}
