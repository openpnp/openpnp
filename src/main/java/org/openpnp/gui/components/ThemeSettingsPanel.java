package org.openpnp.gui.components;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import org.openpnp.Translations;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.util.*;
import java.util.List;

public class ThemeSettingsPanel extends JPanel {
    public enum FontSize {
        SMALLEST(10, 0),
        SMALLER(12, 10),
        SMALL(14, 20),
        DEFAULT(16, 30),
        LARGE(18, 40),
        LARGER(20, 50),
        HUGE(24, 60),
        LARGEST(28, 70);

        private final int size;
        private final int percent;
        private static final Map<Integer, FontSize> percentToSize = new HashMap<>();
        private static final Map<Integer, FontSize> fontSizeToSize = new HashMap<>();

        FontSize(int size, int percent) {
            this.size = size;
            this.percent = percent;
        }

        public int getSize() {
            return size;
        }

        public int getPercent() {
            return percent;
        }

        static {
            for (FontSize fSize : FontSize.values()) {
                percentToSize.put(fSize.getPercent(), fSize);
                fontSizeToSize.put(fSize.getSize(), fSize);
            }
        }

        public static FontSize fromPercent(int percent) {
            FontSize fSize = percentToSize.get(percent);
            if (fSize == null) {
                return DEFAULT;
            }
            return fSize;
        }

        public static FontSize fromSize(int size) {
            FontSize fSize = fontSizeToSize.get(size);
            if (fSize == null) {
                return DEFAULT;
            }
            return fSize;
        }
    }

    public static final String THEMES_PACKAGE = "/com/formdev/flatlaf/intellijthemes/themes/";

    private final List<ThemeInfo> themes = new ArrayList<>();

    private JList<ThemeInfo> themesList;
    private JSlider fontSizeSlider;

    public ThemeSettingsPanel() {
        initComponents();
        themesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String name = ((ThemeInfo) value).name;
                int sep = name.indexOf('/');
                if (sep >= 0) {
                    name = name.substring(sep + 1).trim();
                }

                JComponent c = (JComponent) super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
                c.setToolTipText(buildToolTip((ThemeInfo) value));
                return c;
            }

            private String buildToolTip(ThemeInfo ti) {
                if (ti.themeFile != null) {
                    return ti.themeFile.getPath();
                }
                if (ti.resourceName == null) {
                    return ti.name;
                }

                return "Name: " + ti.name;
            }
        });
        updateThemesList(null);
    }

    private void initComponents() {
        JLabel themesLabel = new JLabel();
        themesList = new JList<>();
        JScrollPane themesScrollPane = new JScrollPane();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        themesLabel.setText(Translations.getString("Theme.Section.Theme"));
        themesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(themesLabel);
        themesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themesScrollPane.setViewportView(themesList);
        themesScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(themesScrollPane);
        JLabel fontLabel = new JLabel();
        fontLabel.setText(Translations.getString("Theme.Section.FontSize"));
        fontLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setAlignmentY(Component.CENTER_ALIGNMENT);
        separator.setMaximumSize(new Dimension(99999999, 5));
        separator.setMinimumSize(new Dimension(0, 5));
        add(separator);
        add(fontLabel);
        fontSizeSlider = createFontSlider();
        fontSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(fontSizeSlider);
    }

    private JSlider createFontSlider() {
        Font defaultFont = UIManager.getLookAndFeelDefaults().getFont("defaultFont");
        Font currentFont = UIManager.getFont("defaultFont");
        int defaultPercent = FontSize.fromSize(defaultFont.getSize()).getPercent();
        int currentPercent = FontSize.fromSize(currentFont.getSize()).getPercent();
        JSlider fontSlider = new JSlider() {
            @Override
            public String getToolTipText(final MouseEvent event) {
                return getValue() + "%";
            }
        };
        fontSlider.setToolTipText(String.valueOf(fontSlider.getValue()));
        fontSlider.setOrientation(JSlider.HORIZONTAL);
        fontSlider.setPaintTicks(true);
        fontSlider.setMinimum(FontSize.SMALLEST.getPercent());
        fontSlider.setMaximum(FontSize.LARGEST.getPercent());
        int tickSpacing = 10;
        @SuppressWarnings("unchecked")
        Dictionary<Integer, JComponent> dict = fontSlider.createStandardLabels(tickSpacing);

        JLabel mid = ((JLabel) dict.get(defaultPercent));

        Enumeration<Integer> e = dict.keys();
        while (e.hasMoreElements()) {
            dict.remove(e.nextElement());
        }

        dict.put(defaultPercent, mid);
        mid.setText(Translations.getString("Theme.FontSize.Default"));
        mid.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        mid.setHorizontalTextPosition(JLabel.RIGHT);

        fontSlider.setLabelTable(dict);
        fontSlider.setMajorTickSpacing(tickSpacing);
        fontSlider.setMinorTickSpacing(tickSpacing);
        fontSlider.setPaintLabels(true);
        fontSlider.setSnapToTicks(true);
        fontSlider.setValue(currentPercent);
        return fontSlider;
    }

    public void updateThemesList(ThemeInfo oldSel) {
        if (oldSel == null) {
            oldSel = themesList.getSelectedValue();
        }

        themes.clear();

        themes.add(new ThemeInfo("Light", null, false, null, FlatLightLaf.class.getName()));
        themes.add(new ThemeInfo("Dark", null, true, null, FlatDarkLaf.class.getName()));
        themes.add(new ThemeInfo("IntelliJ Light", null, false, null, FlatIntelliJLaf.class.getName()));
        themes.add(new ThemeInfo("Darcula", null, true, null, FlatDarculaLaf.class.getName()));

        themesList.setModel(new AbstractListModel<ThemeInfo>() {
            @Override
            public int getSize() {
                return themes.size();
            }

            @Override
            public ThemeInfo getElementAt(int index) {
                return themes.get(index);
            }
        });

        if (oldSel != null) {
            for (int i = 0; i < themes.size(); i++) {
                ThemeInfo theme = themes.get(i);
                if (oldSel.name.equals(theme.name) &&
                        Objects.equals(oldSel.resourceName, theme.resourceName) &&
                        Objects.equals(oldSel.themeFile, theme.themeFile) &&
                        Objects.equals(oldSel.lafClassName, theme.lafClassName)) {
                    themesList.setSelectedIndex(i);
                    break;
                }
            }

            if (themesList.getSelectedIndex() < 0) {
                themesList.setSelectedIndex(0);
            }
        }

        int sel = themesList.getSelectedIndex();
        if (sel >= 0) {
            Rectangle bounds = themesList.getCellBounds(sel, sel);
            if (bounds != null) {
                themesList.scrollRectToVisible(bounds);
            }
        }
    }

    public static void setTheme(ThemeInfo themeInfo, FontSize fontSize) {
        if (themeInfo == null) {
            return;
        }
        // change look and feel
        if (themeInfo.lafClassName != null) {
            if (!themeInfo.lafClassName.equals(UIManager.getLookAndFeel().getClass().getName())) {
                FlatAnimatedLafChange.showSnapshot();
                try {
                    UIManager.setLookAndFeel(themeInfo.lafClassName);
                } catch (Exception ignore) {
                }
            }
        } else if (themeInfo.themeFile != null) {
            FlatAnimatedLafChange.showSnapshot();
            try {
                if (themeInfo.themeFile.getName().endsWith(".properties")) {
                    FlatLaf.install(new FlatPropertiesLaf(themeInfo.name, themeInfo.themeFile));
                } else {
                    FlatLaf.install(IntelliJTheme.createLaf(new FileInputStream(themeInfo.themeFile)));
                }
            } catch (Exception ignore) {
            }
        } else {
            FlatAnimatedLafChange.showSnapshot();
            IntelliJTheme.install(ThemeSettingsPanel.class.getResourceAsStream(THEMES_PACKAGE + themeInfo.resourceName));
        }
        if (fontSize != null) {
            Font font = UIManager.getDefaults().getFont("defaultFont");
            if (font != null) {
                Font newFont = font.deriveFont((float) fontSize.getSize());
                UIManager.put("defaultFont", newFont);
            }
        }

        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    public ThemeInfo setTheme() {
        ThemeInfo themeInfo = themesList.getSelectedValue();
        EventQueue.invokeLater(() -> setTheme(themeInfo, getFontSize()));
        return themeInfo;
    }

    public FontSize getFontSize() {
        return FontSize.fromPercent(fontSizeSlider.getValue());
    }
}
