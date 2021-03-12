package org.openpnp.util;

import com.github.weisj.darklaf.settings.ThemeSettings;
import com.github.weisj.darklaf.theme.event.ThemeChangeEvent;
import com.github.weisj.darklaf.theme.event.ThemeChangeListener;
import org.openpnp.model.Configuration;
import org.pmw.tinylog.Logger;

public class ThemeChangeStoreListener implements ThemeChangeListener {
    @Override
    public void themeChanged(ThemeChangeEvent themeChangeEvent) {
        Configuration.get().setTheme(ThemeSettings.getInstance().exportConfiguration());
        Logger.info("Theme Change");
    }

    @Override
    public void themeInstalled(ThemeChangeEvent themeChangeEvent) {
    }
}
