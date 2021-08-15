package org.openpnp.gui.components;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public class ThemeInfo implements Serializable {
    private static final long serialVersionUID = -3860795927157635051L;
    final String name;
    final String resourceName;
    final boolean dark;
    final File themeFile;
    final String lafClassName;

    public ThemeInfo(String name, String resourceName, boolean dark, File themeFile, String lafClassName) {
        this.name = name;
        this.resourceName = resourceName;
        this.dark = dark;
        this.themeFile = themeFile;
        this.lafClassName = lafClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThemeInfo themeInfo = (ThemeInfo) o;
        return dark == themeInfo.dark && Objects.equals(name, themeInfo.name) && Objects.equals(resourceName, themeInfo.resourceName) && Objects.equals(themeFile, themeInfo.themeFile) && Objects.equals(lafClassName, themeInfo.lafClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, resourceName, dark, themeFile, lafClassName);
    }
}
