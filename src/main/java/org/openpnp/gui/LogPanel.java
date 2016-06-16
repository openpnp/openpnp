package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.openpnp.gui.support.UiAppender;

public class LogPanel extends JPanel {
    private JTextArea text;

    public LogPanel() {
        setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        text = new JTextArea();
        text.setFont(new Font("Monospaced", Font.PLAIN, 13));
        text.setEditable(false);
        text.setLineWrap(true);
        add(new JScrollPane(text), BorderLayout.CENTER);


        // http://stackoverflow.com/questions/1627028/how-to-set-auto-scrolling-of-jtextarea-in-java-gui
        UiAppender appender = new UiAppender(text);
        appender.setLayout(new PatternLayout("%d{ABSOLUTE} %-5p %20C{1} %m%n"));
        appender.setThreshold(Level.TRACE);
        org.apache.log4j.Logger.getRootLogger().addAppender(appender);
    }

}
