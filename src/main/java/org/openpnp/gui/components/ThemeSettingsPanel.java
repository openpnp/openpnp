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
        themesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ThemeInfo theme = (ThemeInfo) value;
                String name = theme.name;
                int sep = name.indexOf('/');
                if (sep >= 0) {
                    name = name.substring(sep + 1).trim();
                }

                JComponent c;
                if (theme.themeFile == null && theme.lafClassName == null && theme.resourceName == null) {
                    name = " - " + name + " -";
                    c = (JComponent) super.getListCellRendererComponent(list, name, index, false, false);
                    c.setEnabled(false);
                    c.setFocusable(false);
                    c.setAlignmentX(CENTER_ALIGNMENT);
                } else {
                    c = (JComponent) super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
                }
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
        createFontSlider();
    }

    private void createFontSlider() {
        Font defaultFont = UIManager.getLookAndFeelDefaults().getFont("defaultFont");
        Font currentFont = UIManager.getFont("defaultFont");
        if (fontSizeSlider != null) {
            remove(fontSizeSlider);
        }
        if (defaultFont == null || currentFont == null) {
            fontSizeSlider = new JSlider();
            fontSizeSlider.setEnabled(false);
            fontSizeSlider.setMinimum(-1);
            fontSizeSlider.setMaximum(0);
            fontSizeSlider.setValue(-1);
            fontSizeSlider.setPaintTicks(true);
            fontSizeSlider.setOrientation(JSlider.HORIZONTAL);
            fontSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(fontSizeSlider);
            return;
        }
        int defaultPercent = FontSize.fromSize(defaultFont.getSize()).getPercent();
        int currentPercent = FontSize.fromSize(currentFont.getSize()).getPercent();
        fontSizeSlider = new JSlider() {
            @Override
            public String getToolTipText(final MouseEvent event) {
                return getValue() + "%";
            }
        };
        fontSizeSlider.setToolTipText(String.valueOf(fontSizeSlider.getValue()));
        fontSizeSlider.setOrientation(JSlider.HORIZONTAL);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setMinimum(FontSize.SMALLEST.getPercent());
        fontSizeSlider.setMaximum(FontSize.LARGEST.getPercent());
        int tickSpacing = 10;
        @SuppressWarnings("unchecked")
        Dictionary<Integer, JComponent> dict = fontSizeSlider.createStandardLabels(tickSpacing);

        JLabel mid = ((JLabel) dict.get(defaultPercent));

        Enumeration<Integer> e = dict.keys();
        while (e.hasMoreElements()) {
            dict.remove(e.nextElement());
        }

        dict.put(defaultPercent, mid);
        mid.setText(Translations.getString("Theme.FontSize.Default"));
        mid.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        mid.setHorizontalTextPosition(JLabel.RIGHT);

        fontSizeSlider.setLabelTable(dict);
        fontSizeSlider.setMajorTickSpacing(tickSpacing);
        fontSizeSlider.setMinorTickSpacing(tickSpacing);
        fontSizeSlider.setPaintLabels(true);
        fontSizeSlider.setSnapToTicks(true);
        fontSizeSlider.setValue(currentPercent);
        fontSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(fontSizeSlider);
    }

    public void updateThemesList(ThemeInfo oldSel) {
        if (oldSel == null) {
            oldSel = themesList.getSelectedValue();
        }

        themes.clear();

        themes.add(new ThemeInfo("System Themes", null, false, null, null));
        UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
        for( UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels ) {
            String name = lookAndFeel.getName();
            String className = lookAndFeel.getClassName();
            themes.add(new ThemeInfo(name, null, false, null, className));
        }

        themes.add(new ThemeInfo("Extra Themes", null, false, null, null));
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

    public void setTheme(ThemeInfo themeInfo, FontSize fontSize) {
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
        } else if (themeInfo.resourceName != null) {
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
        Color defaultRowColor = UIManager.getColor("Table.background");
        if (FlatLaf.isLafDark()) {
            UIManager.put("Table.alternateRowColor", defaultRowColor.brighter());
        } else {
            UIManager.put("Table.alternateRowColor", defaultRowColor.darker());
        }
        FlatLaf.updateUI();
        removeAll();
        initComponents();
        updateThemesList(themeInfo);
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    public ThemeInfo setTheme() {
        ThemeInfo themeInfo = themesList.getSelectedValue();
        EventQueue.invokeLater(() -> setTheme(themeInfo, getFontSize()));
        return themeInfo;
    }

    public FontSize getFontSize() {
        int value = fontSizeSlider.getValue();
        if (value < 0) {
            return null;
        }
        return FontSize.fromPercent(fontSizeSlider.getValue());
    }
}
