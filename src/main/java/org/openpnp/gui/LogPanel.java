package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.openpnp.gui.support.JTextAreaAppender;

public class LogPanel extends JPanel {
    private JTextArea text;
    private JTextAreaAppender appender;

    private Preferences prefs = Preferences.userNodeForPackage(LogPanel.class);

    private static final String PREF_LOG_LEVEL = "LogPanel.logLevel";
    private static final int PREF_LOG_LEVEL_DEF = Level.INFO.toInt();

    private static final String PREF_LOG_LINE_LIMIT = "LogPanel.lineLimit";
    private static final int PREF_LOG_LINE_LIMIT_DEF = 1000;

    HashMap<String, Integer> lineLimits = new HashMap<String, Integer>() {{
        put("100", 100);
        put("1000", 1000);
        put("10000", 10000);
        put("Unlimited", -1);
    }};

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

        // http://stackoverflow.com/questions/1627028/how-to-set-auto-scrolling-of-jtextarea-in-java-gui
        appender = new JTextAreaAppender(text);
        appender.setLayout(new PatternLayout("%d{ABSOLUTE} %-5p %20C{1} %m%n"));
        appender.setThreshold(Level.toLevel(prefs.getInt(PREF_LOG_LEVEL, PREF_LOG_LEVEL_DEF)));
        appender.setLineLimit(prefs.getInt(PREF_LOG_LINE_LIMIT, PREF_LOG_LINE_LIMIT_DEF));

        org.apache.log4j.Logger.getRootLogger().addAppender(appender);
        
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
            if(limit == prefs.getInt(PREF_LOG_LINE_LIMIT, PREF_LOG_LINE_LIMIT_DEF)) {
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
        
        for (Level level : new Level[] { Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            menuItem = new JRadioButtonMenuItem(level.toString());
            if (level.toInt() == prefs.getInt(PREF_LOG_LEVEL, PREF_LOG_LEVEL_DEF)) {
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
            prefs.putInt(PREF_LOG_LEVEL, Level.toLevel(s).toInt());
            Level level = Level.toLevel(s);
            appender.setThreshold(level);
        }
    };

    private Action setLineLimitAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int lineLimit = Integer.parseInt(e.getActionCommand());
            prefs.putInt(PREF_LOG_LINE_LIMIT, lineLimit);
            appender.setLineLimit(lineLimit);
        }
    };

}
