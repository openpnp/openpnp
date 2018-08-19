package org.openpnp.model.eagle;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The NormalizedStringAdapter replaces all non valid characters for an xml import
 * Replaces all control characters and characters not suitable for creating filenames
 */
public class NormalizedStringAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(String text) throws Exception {
        if (text == null) {
            return null;
        }

        String invalidCharactersXML10 = "[^"
                + "\u0009\r\n"
                + "\u0020-\uD7FF"
                + "\uE000-\uFFFD"
                + "\ud800\udc00-\udbff\udfff"
                + "]";

        String invalidFilenameCharacters = "[\\\\/*:-?]";

        return text
                .replaceAll(invalidCharactersXML10, " ")
                .replaceAll(invalidFilenameCharacters, " ")
                .trim();
    }

    @Override
    public String marshal(String s) throws Exception {
        return s;
    }
}
