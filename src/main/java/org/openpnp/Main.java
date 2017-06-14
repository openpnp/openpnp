/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp;

import java.awt.EventQueue;
import java.io.File;

import javax.swing.UIManager;

import org.openpnp.gui.MainFrame;
import org.openpnp.logging.ConsoleWriter;
import org.openpnp.logging.SystemLogger;
import org.openpnp.model.Configuration;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.RollingFileWriter;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Start with -Xdock:name=OpenPnP on Mac to make it prettier.
 * 
 * @author jason
 *
 */
public class Main {
    public static String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "INTERNAL BUILD";
        }
        return version;
    }

    private static void configureLogging(File configurationDirectory) {
        File logDirectory = new File(configurationDirectory, "log");
        File logFile = new File(logDirectory, "OpenPnP.log");
        Configurator
            .currentConfig()
            .writer(new RollingFileWriter(logFile.getAbsolutePath(), 100))
            .addWriter(new ConsoleWriter(System.out, System.err))
            .activate();
        Configurator.currentConfig()
            .formatPattern("{date:yyyy-MM-dd HH:mm:ss} {class_name} {level}: {message}")
            .activate();

        // Redirect the stdout and stderr to the LogPanel
        SystemLogger out = new SystemLogger(System.out, Level.INFO);
        SystemLogger err = new SystemLogger(System.err, Level.ERROR);
        System.setOut(out);
        System.setErr(err);
    }
    
    private static void monkeyPatchBeansBinding() {
        // This hack fixes a bug in BeansBinding that will never be released due to to the library
        // being abandoned. The bug is that in BeansBinding.bind, it chooses to call an uncached
        // introspection method rather than a cached one. This causes each binding to take upwards
        // of 50ms on my machine. On a form with many bindings this can cause a huge load time
        // when loading wizards. This was most apparent on Feeders.
        // Note that the bug was fixed in Subversion in revision 629:
        // https://java.net/projects/beansbinding/sources/svn/revision/629
        // But it is unlikely this will ever be released to Maven.
        // This hack was found at http://blog.marcnuri.com/beansbinding-performance-issue-37/
        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get("org.jdesktop.beansbinding.ELProperty");
            CtMethod m = cc.getDeclaredMethod("getBeanInfo");
            m.setBody("{" +
            // "assert $1 != null;" +
                    "try {" + "return java.beans.Introspector.getBeanInfo($1.getClass());"
                    + "} catch (java.beans.IntrospectionException ie) {"
                    + "throw new org.jdesktop.beansbinding.PropertyResolutionException(\"Exception while introspecting \" + $1.getClass().getName(), ie);"
                    + "} }");
            Class c = cc.toClass();
            cc = cp.get("org.jdesktop.beansbinding.BeanProperty");
            m = cc.getDeclaredMethod("getBeanInfo");
            m.setBody("{" +
            // "assert $1 != null;" +
                    "try {" + "return java.beans.Introspector.getBeanInfo($1.getClass());"
                    + "} catch (java.beans.IntrospectionException ie) {"
                    + "throw new org.jdesktop.beansbinding.PropertyResolutionException(\"Exception while introspecting \" + $1.getClass().getName(), ie);"
                    + "} }");
            c = cc.toClass();
        }
        catch (NotFoundException ex) {
            ex.printStackTrace();
        }
        catch (CannotCompileException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        monkeyPatchBeansBinding();
        
        for (String s : args) {
            if (s.equals("--version")) {
                System.out.println(getVersion());
                System.exit(0);
            }
        }
        
        // http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html#//apple_ref/doc/uid/TP40001909-212952-TPXREF134
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            throw new Error(e);
        }

        File configurationDirectory = new File(System.getProperty("user.home"));
        configurationDirectory = new File(configurationDirectory, ".openpnp");

        if (System.getProperty("configDir") != null) {
            configurationDirectory = new File(System.getProperty("configDir"));
        }

        configurationDirectory.mkdirs();

        configureLogging(configurationDirectory);

        Configuration.initialize(configurationDirectory);
        final Configuration configuration = Configuration.get();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainFrame frame = new MainFrame(configuration);
                    frame.setVisible(true);
                    Logger.debug(String.format("Bienvenue, Willkommen, Hello, Namaskar, Welkom to OpenPnP version %s.", Main.getVersion()));
                    configuration.getScripting().on("Startup", null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
