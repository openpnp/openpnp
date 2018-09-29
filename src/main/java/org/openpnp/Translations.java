package org.openpnp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Translations {
    private static final String BUNDLE_NAME = "org.openpnp.translations"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Translations() {}

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
