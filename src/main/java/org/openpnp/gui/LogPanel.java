package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        appender.setThreshold(Level.INFO);
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
        JRadioButtonMenuItem menuItem;

        JPopupMenu menu = new JPopupMenu();
        
        menuItem = new JRadioButtonMenuItem("100");
        menuItem.addActionListener(setLineLimitAction);
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("1000");
        menuItem.setSelected(true);
        menuItem.addActionListener(setLineLimitAction);
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("10000");
        menuItem.addActionListener(setLineLimitAction);
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Unlimited");
        menuItem.addActionListener(setLineLimitAction);
        buttonGroup.add(menuItem);
        menu.add(menuItem);

        return menu;
    }

    private JPopupMenu createLogLevelMenu() {
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem menuItem;

        JPopupMenu menu = new JPopupMenu();
        
        for (Level level : new Level[] { Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL}) {
            menuItem = new JRadioButtonMenuItem(level.toString());
            if (level.toString().equals("INFO")) {
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
            Level level = Level.toLevel(s);
            appender.setThreshold(level);
        }
    };

    private Action setLineLimitAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String s = e.getActionCommand();
            if (s.equals("Unlimited")) {
                appender.setLineLimit(-1);
            }
            else {
                appender.setLineLimit(Integer.parseInt(e.getActionCommand()));
            }
        }
    };

}
