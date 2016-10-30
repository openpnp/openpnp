package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import org.openpnp.gui.support.JTextLogWriter;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

public class LogPanel extends JPanel {
    private JTextArea text;
    private JTextLogWriter writer;

    private Preferences prefs = Preferences.userNodeForPackage(LogPanel.class);

    private static final String PREF_LOG_LEVEL = "LogPanel.logLevel";
    private static final String PREF_LOG_LEVEL_DEF = Level.INFO.toString();

    private static final String PREF_LOG_LINE_LIMIT = "LogPanel.lineLimit";
    private static final int PREF_LOG_LINE_LIMIT_DEF = 1000;

    HashMap<String, Integer> lineLimits = new HashMap<String, Integer>() {
        {
            put("100", 100);
            put("1000", 1000);
            put("10000", 10000);
            put("Unlimited", -1);
        }
    };

    public LogPanel() {
        setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        JButton btnLineLimit = new JButton("Line Limit");
        toolBar.add(btnLineLimit);

        JButton btnLogLevel = new JButton("Log Level");
        toolBar.add(btnLogLevel);

        text = new JTextArea();
        text.setFont(new Font("Monospaced", Font.PLAIN, 13));
        text.setEditable(false);
        text.setLineWrap(true);
        add(new JScrollPane(text), BorderLayout.CENTER);


        writer = new JTextLogWriter(text);
        writer.setLineLimit(prefs.getInt(PREF_LOG_LINE_LIMIT, PREF_LOG_LINE_LIMIT_DEF));
        
        // This weird check is here because I mistakenly reused the same config key when
        // switching from slf to tinylog. This meant that some users had an int based
        // value in the key rather than the string. This caused initialization failures.
        Level level = null;
        try {
            level = Level.valueOf(prefs.get(PREF_LOG_LEVEL, PREF_LOG_LEVEL_DEF));
        }
        catch (Exception e) {
        }
        if (level == null ) {
            level = Level.INFO;
        }

        Configurator
            .currentConfig()
            .level(level)
            .activate();
        Configurator
            .currentConfig()
            .addWriter(writer)
            .activate();

        JPopupMenu lineLimitPopupMenu = createLineLimitMenu();
        btnLineLimit.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lineLimitPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        JPopupMenu logLevelPopupMenu = createLogLevelMenu();
        btnLogLevel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                logLevelPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private JPopupMenu createLineLimitMenu() {
        ButtonGroup buttonGroup = new ButtonGroup();

        JPopupMenu menu = new JPopupMenu();

        lineLimits.forEach((label, limit) -> {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(label);
            menuItem.setActionCommand(limit.toString());
            menuItem.addActionListener(setLineLimitAction);
            if (limit == prefs.getInt(PREF_LOG_LINE_LIMIT, PREF_LOG_LINE_LIMIT_DEF)) {
                menuItem.setSelected(true);
            }
            buttonGroup.add(menuItem);
            menu.add(menuItem);
        });

        return menu;
    }

    private JPopupMenu createLogLevelMenu() {
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem menuItem;

        JPopupMenu menu = new JPopupMenu();

        for (Level level : Level.values()) {
            menuItem = new JRadioButtonMenuItem(level.toString());
            if (level.toString().equals(prefs.get(PREF_LOG_LEVEL, PREF_LOG_LEVEL_DEF))) {
                menuItem.setSelected(true);
            }
            menuItem.addActionListener(setThresholdAction);
            buttonGroup.add(menuItem);
            menu.add(menuItem);
        }

        return menu;
    }

    private Action setThresholdAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String s = e.getActionCommand();
            prefs.put(PREF_LOG_LEVEL, s);
            Level level = Level.valueOf(s);
            Configurator
                .currentConfig()
                .level(level)
                .activate();
        }
    };

    private Action setLineLimitAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int lineLimit = Integer.parseInt(e.getActionCommand());
            prefs.putInt(PREF_LOG_LINE_LIMIT, lineLimit);
            writer.setLineLimit(lineLimit);
        }
    };

}
