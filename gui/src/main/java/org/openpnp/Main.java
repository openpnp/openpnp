/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp;

import java.awt.EventQueue;
import java.io.File;

import javax.swing.UIManager;

import org.apache.commons.io.FileUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start with -Xdock:name=OpenPnP on Mac to make it prettier.
 * @author jason
 *
 */
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			throw new Error(e);
		}
		
		File configurationDirectory = new File(System.getProperty("user.home"));
		configurationDirectory = new File(configurationDirectory, ".openpnp");
		
		// If the log4j.properties is not in the configuration directory, copy
		// the default over.
		File log4jConfigurationFile = new File(configurationDirectory, "log4j.properties");
		if (!log4jConfigurationFile.exists()) {
			try {
				FileUtils.copyURLToFile(ClassLoader.getSystemResource("log4j.properties"), log4jConfigurationFile);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Use the local configuration if it exists.
		if (log4jConfigurationFile.exists()) {
			System.setProperty("log4j.configuration", log4jConfigurationFile.toURI().toString());
		}
		
		logger.debug("OpenPnP vX.X.X Started.");
		
		Configuration.initialize(configurationDirectory);
		final Configuration configuration = Configuration.get();
		final JobProcessor jobProcessor = new JobProcessor(configuration);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame frame = new MainFrame(configuration, jobProcessor);
					frame.setVisible(true);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
