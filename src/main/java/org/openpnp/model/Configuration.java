/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ThemeInfo;
import org.openpnp.gui.components.ThemeSettingsPanel;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.scripting.Scripting;
import org.openpnp.spi.Machine;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

import com.google.common.eventbus.EventBus;

public class Configuration extends AbstractModelObject {
    private static Configuration instance;

    private static final String PREF_LOCALE_LANG = "Configuration.locale.lang";
    private static final String PREF_LOCALE_LANG_DEF = "en";

    private static final String PREF_LOCALE_COUNTRY = "Configuration.locale.country";
    private static final String PREF_LOCALE_COUNTRY_DEF = "US";

    private static final String PREF_UNITS = "Configuration.units";
    private static final String PREF_UNITS_DEF = "Millimeters";

    private static final String PREF_DISTANCE = "Configuration.distance";
    private static final int PREF_DISTANCE_DEF = 2;

    private static final String PREF_TABLE_LINKS = "Configuration.tableLinks";

    private static final String PREF_THEME_INFO = "Configuration.theme.info";
    private static final String PREF_THEME_FONT_SIZE = "Configuration.theme.fontSize";
    private static final String PREF_THEME_ALTERNATE_ROWS = "Configuration.theme.alternateRows";

    private static final String PREF_LENGTH_DISPLAY_FORMAT = "Configuration.lengthDisplayFormat";
    private static final String PREF_LENGTH_DISPLAY_FORMAT_DEF = "%.3f";

    private static final String PREF_LENGTH_DISPLAY_ALIGNED_FORMAT = "Configuration.lengthDisplayAlignedFormat";
    private static final String PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_DEF = "%9.3f  ";

    private static final String PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS =
            "Configuration.lengthDisplayFormatWithUnits";
    private static final String PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS_DEF = "%.3f%s";

    private static final String PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_WITH_UNITS =
            "Configuration.lengthDisplayAlignedFormatWithUnits";
    private static final String PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_WITH_UNITS_DEF = "%9.3f%-2s";

    private static final String PREF_VERTICAL_SCROLL_UNIT_INCREMENT =
            "Configuration.verticalScrollUnitIncrement";
    private static final int PREF_VERTICAL_SCROLL_UNIT_INCREMENT_DEF = 16;
    private static final String imgurClientId = "620fc1fa8ee0180";

    public enum TablesLinked {
        Unlinked,
        Linked
    };

    private LinkedHashMap<String, Package> packages = new LinkedHashMap<>();
    private LinkedHashMap<String, Part> parts = new LinkedHashMap<>();
    private LinkedHashMap<String, AbstractVisionSettings> visionSettings = new LinkedHashMap<>();
    private Machine machine;
    private LinkedHashMap<File, Panel> panels = new LinkedHashMap<>();
    private LinkedHashMap<File, Board> boards = new LinkedHashMap<>();
    private boolean loaded;
    private Set<ConfigurationListener> listeners = Collections.synchronizedSet(new HashSet<>());
    private File configurationDirectory;
    private Preferences prefs;
    private Scripting scripting;
    private EventBus bus = new EventBus();

    public static boolean isInstanceInitialized() {
        return (instance != null);
    }

    public static Configuration get() {
        if (instance == null) {
            throw new Error("Configuration instance not yet initialized.");
        }
        return instance;
    }

    /**
     * Initializes a new persistent Configuration singleton storing configuration files in
     * configurationDirectory.
     * @param configurationDirectory
     */
    public static synchronized void initialize(File configurationDirectory) {
        instance = new Configuration(configurationDirectory);
        instance.setLengthDisplayFormatWithUnits(PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS_DEF);
    }
    
    /**
     * Initializes a new temporary Configuration singleton storing configuration in memory only.
     * @param configurationDirectory
     */
    public static synchronized void initialize() {
        /**
         * TODO STOPSHIP ideally this would use an in memory prefs, too, so that we
         * don't mess with global user prefs.
         */
        instance = new Configuration();
        instance.setLengthDisplayFormatWithUnits(PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS_DEF);
    }

    private Configuration(File configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
        this.prefs = Preferences.userNodeForPackage(Configuration.class);
        File scriptingDirectory = new File(configurationDirectory, "scripts");
        this.scripting = new Scripting(scriptingDirectory);
    }
    
    private Configuration() {
        this.prefs = Preferences.userNodeForPackage(Configuration.class);
        this.scripting = new Scripting(null);
        /**
         * Setting loaded = true allows the mechanism of immediately notifying late
         * Configuration.addListener() calls that the configuration is ready. It's a legacy
         * hack.
         */
        loaded = true;
    }
    
    public void setMachine(Machine machine) {
        this.machine = machine;
    }
    
    public Scripting getScripting() {
        return scripting;
    }
    
    public EventBus getBus() {
        return bus;
    }

    public File getConfigurationDirectory() {
        return configurationDirectory;
    }

    public LengthUnit getSystemUnits() {
        return LengthUnit.valueOf(prefs.get(PREF_UNITS, PREF_UNITS_DEF));
    }

    public void setSystemUnits(LengthUnit lengthUnit) {
        prefs.put(PREF_UNITS, lengthUnit.name());
    }

    public void setDistance(int distance) {
        prefs.putInt(PREF_DISTANCE, distance);
    }

    public int getDistance() {
        return prefs.getInt(PREF_DISTANCE, PREF_DISTANCE_DEF);
    }

    public TablesLinked getTablesLinked() {
        return TablesLinked.valueOf(prefs.get(PREF_TABLE_LINKS, TablesLinked.Unlinked.toString()));
    }

    public void setTablesLinked(TablesLinked tablesLinked) {
        prefs.put(PREF_TABLE_LINKS, tablesLinked.toString());
    }

    public Locale getLocale() {
        return new Locale(prefs.get(PREF_LOCALE_LANG, PREF_LOCALE_LANG_DEF), 
                prefs.get(PREF_LOCALE_COUNTRY, PREF_LOCALE_COUNTRY_DEF));
    }

    public void setLocale(Locale locale) {
        prefs.put(PREF_LOCALE_LANG, locale.getLanguage());
        prefs.put(PREF_LOCALE_COUNTRY, locale.getCountry());
    }

    public ThemeInfo getThemeInfo() {
        byte[] serializedSettings = prefs.getByteArray(PREF_THEME_INFO, null);
        if (serializedSettings != null) {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedSettings))) {
                ThemeInfo theme = (ThemeInfo) in.readObject();
                return theme;
            } catch (IOException | ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    public void setThemeInfo(ThemeInfo theme) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(theme);
            out.flush();
            prefs.putByteArray(PREF_THEME_INFO, bos.toByteArray());
        } catch (IOException ignore) {
        }
    }

    public ThemeSettingsPanel.FontSize getFontSize() {
        byte[] serializedSettings = prefs.getByteArray(PREF_THEME_FONT_SIZE, null);
        if (serializedSettings != null) {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedSettings))) {
                ThemeSettingsPanel.FontSize fontSize = (ThemeSettingsPanel.FontSize) in.readObject();
                return fontSize;
            } catch (IOException | ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    public void setFontSize(ThemeSettingsPanel.FontSize fontSize) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(fontSize);
            out.flush();
            prefs.putByteArray(PREF_THEME_FONT_SIZE, bos.toByteArray());
        } catch (IOException ignore) {
        }
    }

    public Boolean isAlternateRows() {
        byte[] serializedSettings = prefs.getByteArray(PREF_THEME_ALTERNATE_ROWS, null);
        if (serializedSettings != null) {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedSettings))) {
                Boolean alternateRows = (Boolean) in.readObject();
                return alternateRows;
            } catch (IOException | ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    public void setAlternateRows(Boolean alternateRows) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(alternateRows);
            out.flush();
            prefs.putByteArray(PREF_THEME_ALTERNATE_ROWS, bos.toByteArray());
        } catch (IOException ignore) {
        }
    }

    public String getLengthDisplayFormat() {
        return prefs.get(PREF_LENGTH_DISPLAY_FORMAT, PREF_LENGTH_DISPLAY_FORMAT_DEF);
    }

    public void setLengthDisplayFormat(String format) {
        prefs.put(PREF_LENGTH_DISPLAY_FORMAT, format);
    }

    public String getLengthDisplayAlignedFormat() {
        return prefs.get(PREF_LENGTH_DISPLAY_ALIGNED_FORMAT, PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_DEF);
    }

    public void setLengthDisplayAlignedFormat(String format) {
        prefs.put(PREF_LENGTH_DISPLAY_ALIGNED_FORMAT, format);
    }

    public String getLengthDisplayFormatWithUnits() {
        return prefs.get(PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS,
                PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS_DEF);
    }

    public void setLengthDisplayFormatWithUnits(String format) {
        prefs.put(PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS, format);
    }

    public String getLengthDisplayAlignedFormatWithUnits() {
        return prefs.get(PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_WITH_UNITS,
                PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_WITH_UNITS_DEF);
    }

    public void setLengthDisplayAlignedFormatWithUnits(String format) {
        prefs.put(PREF_LENGTH_DISPLAY_ALIGNED_FORMAT_WITH_UNITS, format);
    }

    public int getVerticalScrollUnitIncrement() {
        return prefs.getInt(PREF_VERTICAL_SCROLL_UNIT_INCREMENT,
                PREF_VERTICAL_SCROLL_UNIT_INCREMENT_DEF);
    }

    public void setVerticalScrollUnitIncrement(int verticalScrollUnitIncrement) {
        prefs.putInt(PREF_VERTICAL_SCROLL_UNIT_INCREMENT, PREF_VERTICAL_SCROLL_UNIT_INCREMENT_DEF);
    }

    /**
     * Gets a File reference for the resources directory belonging to the given class. The directory
     * is guaranteed to exist.
     * 
     * @param forClass
     * @return
     * @throws IOException
     */
    public File getResourceDirectory(Class forClass) throws IOException {
        File directory = new File(configurationDirectory, forClass.getCanonicalName());
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    /**
     * Gets a File reference for the named file within the configuration directory. forClass is used
     * to uniquely identify the file and keep it separate from other classes' files.
     * 
     * @param forClass
     * @param name
     * @return
     */
    public File getResourceFile(Class forClass, String name) throws IOException {
        return new File(getResourceDirectory(forClass), name);
    }

    /**
     * Creates a new file with a unique name within the configuration directory. forClass is used to
     * uniquely identify the file within the application and a unique name is generated within that
     * namespace. suffix is appended to the unique part of the filename. The result of calling
     * File.getName() on the returned file can be used to load the same file in the future by
     * calling getResourceFile(). This method uses NanosecondTime.get() so the files names
     * will be unique and ordered.
     * 
     * @param forClass
     * @param suffix
     * @return
     * @throws IOException
     */
    public File createResourceFile(Class forClass, String prefix, String suffix)
            throws IOException {
        File directory = new File(configurationDirectory, forClass.getCanonicalName());
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, prefix+NanosecondTime.get()+suffix);
        return file;
    }

    public void addListener(ConfigurationListener listener) {
        listeners.add(listener);
        if (loaded) {
            try {
                listener.configurationLoaded(this);
                listener.configurationComplete(this);
            }
            catch (Exception e) {
                // TODO: Need to find a way to raise this to the GUI
                throw new Error(e);
            }
        }
    }

    public void removeListener(ConfigurationListener listener) {
        listeners.remove(listener);
    }

    public synchronized void load() throws Exception {
        boolean forceSave = false;
        boolean overrideUserConfig = Boolean.getBoolean("overrideUserConfig");

        try {
            File file = new File(configurationDirectory, "packages.xml");
            if (overrideUserConfig || !file.exists()) {
                Logger.info("No packages.xml found in configuration directory, loading defaults.");
                file = File.createTempFile("packages", "xml");
                FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/packages.xml"), file);
                forceSave = true;
            }
            loadPackages(file);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            throw new Exception("Error while reading packages.xml (" + message + ")", e);
        }


        try {
            File file = new File(configurationDirectory, "parts.xml");
            if (overrideUserConfig || !file.exists()) {
                Logger.info("No parts.xml found in configuration directory, loading defaults.");
                file = File.createTempFile("parts", "xml");
                FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/parts.xml"), file);
                forceSave = true;
            }
            loadParts(file);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            throw new Exception("Error while reading parts.xml (" + message + ")", e);
        }

        try {
            File file = new File(configurationDirectory, "boards.xml");
            if (overrideUserConfig || !file.exists()) {
                Logger.info("No boards.xml found in configuration directory, loading defaults.");
                file = File.createTempFile("boards", "xml");
                FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/boards.xml"), file);
                forceSave = true;
            }
            loadBoards(file);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            throw new Exception("Error while reading boards.xml (" + message + ")", e);
        }

        try {
            File file = new File(configurationDirectory, "panels.xml");
            if (overrideUserConfig || !file.exists()) {
                Logger.info("No panels.xml found in configuration directory, loading defaults.");
                file = File.createTempFile("panels", "xml");
                FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/panels.xml"), file);
                forceSave = true;
            }
            loadPanels(file);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            throw new Exception("Error while reading panels.xml (" + message + ")", e);
        }

        try {
            File file = new File(configurationDirectory, "vision-settings.xml");
            if (overrideUserConfig || !file.exists()) {
                Logger.info("No vision-settings.xml found in configuration directory, loading defaults.");
                file = File.createTempFile("visionSettings", "xml");
                FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/vision-settings.xml"), file);
                forceSave = true;
            }
            loadVisionSettings(file);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            throw new Exception("Error while reading vision-settings.xml (" + message + ")", e);
        }


        try {
            File file = new File(configurationDirectory, "machine.xml");
            if (overrideUserConfig || !file.exists()) {
                Logger.info("No machine.xml found in configuration directory, loading defaults.");
                file = File.createTempFile("machine", "xml");
                FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/machine.xml"), file);
                forceSave = true;
            }
            loadMachine(file);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            throw new Exception("Error while reading machine.xml (" + message + ")", e);
        }

        loaded = true;

        // Tell all listeners the configuration is loaded. Use a snapshot of the list in order to tolerate new
        // listener additions that may happen through object migration.
        for (ConfigurationListener listener : new ArrayList<>(listeners)) {
            listener.configurationLoaded(this);
        }

        if (forceSave) {
            Logger.info("Defaults were loaded. Saving to configuration directory.");
            configurationDirectory.mkdirs();
            save();
        }

        for (ConfigurationListener listener : listeners) {
            listener.configurationComplete(this);
        }
    }

    public synchronized void save() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        try {
           saveMachine(createBackedUpFile("machine.xml", now));
        }
        catch (Exception e) {
            throw new Exception("Error while saving machine.xml (" + e.getMessage() + ")", e);
        }
        try {
            savePackages(createBackedUpFile("packages.xml", now));
        }
        catch (Exception e) {
            throw new Exception("Error while saving packages.xml (" + e.getMessage() + ")", e);
        }
        try {
            saveParts(createBackedUpFile("parts.xml", now));
        }
        catch (Exception e) {
            throw new Exception("Error while saving parts.xml (" + e.getMessage() + ")", e);
        }
        try {
            saveBoards(createBackedUpFile("boards.xml", now));
        }
        catch (Exception e) {
            throw new Exception("Error while saving boards.xml (" + e.getMessage() + ")", e);
        }
        try {
            savePanels(createBackedUpFile("panels.xml", now));
        }
        catch (Exception e) {
            throw new Exception("Error while saving panels.xml (" + e.getMessage() + ")", e);
        }
        try {
            saveVisionSettings(createBackedUpFile("vision-settings.xml", now));
        }
        catch (Exception e) {
            throw new Exception("Error while saving vision-settings.xml (" + e.getMessage() + ")", e);
        }
    }

    protected File createBackedUpFile(String fileName, LocalDateTime now) throws Exception {
        File file = new File(configurationDirectory, fileName);
        if (file.exists()) {
            File backupsDirectory = new File(configurationDirectory, "backups");
            if (System.getProperty("backups") != null) {
                backupsDirectory = new File(System.getProperty("backups"));
            }

            File singleBackupDirectory = new File(backupsDirectory, DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(now));
            singleBackupDirectory.mkdirs();
            File backupFile = new File(singleBackupDirectory, fileName);
            Files.copy(Paths.get(file.toURI()), Paths.get(backupFile.toURI()), 
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
        return file;
    }

    public Package getPackage(String id) {
        if (id == null) {
            return null;
        }
        return packages.get(id.toUpperCase());
    }

    public List<Package> getPackages() {
        return Collections.unmodifiableList(new ArrayList<>(packages.values()));
    }

    public void addPackage(Package pkg) {
        if (null == pkg.getId()) {
            throw new Error("Package with null Id cannot be added to Configuration.");
        }
        packages.put(pkg.getId().toUpperCase(), pkg);
        firePropertyChange("packages", null, packages);
    }

    public void removePackage(Package pkg) {
        packages.remove(pkg.getId().toUpperCase());
        firePropertyChange("packages", null, packages);
    }

    public Part getPart(String id) {
        if (id == null) {
            return null;
        }
        return parts.get(id.toUpperCase());
    }

    public List<Part> getParts() {
        return Collections.unmodifiableList(new ArrayList<>(parts.values()));
    }

    public void addPart(Part part) {
        if (null == part.getId()) {
            throw new Error("Part with null Id cannot be added to Configuration.");
        }
        parts.put(part.getId().toUpperCase(), part);
        firePropertyChange("parts", null, parts);
    }

    public void removePart(Part part) {
        parts.remove(part.getId().toUpperCase());
        firePropertyChange("parts", null, parts);
    }

    public void addVisionSettings(AbstractVisionSettings visionSettings) {
        if (null == visionSettings.getId()) {
            throw new Error("Vision Settings with null Id cannot be added to Configuration.");
        }
        this.visionSettings.put(visionSettings.getId().toUpperCase(), visionSettings);
        fireVisionSettingsChanged();
    }

    public List<AbstractVisionSettings> getVisionSettings() {
        return Collections.unmodifiableList(new ArrayList<>(visionSettings.values()));
    }

    public AbstractVisionSettings getVisionSettings(String visionSettingsId) {
        if (visionSettingsId == null) {
            return null;
        }

        return this.visionSettings.get(visionSettingsId.toUpperCase());
    }

    public void removeVisionSettings(AbstractVisionSettings visionSettings) {
        this.visionSettings.remove(visionSettings.getId().toUpperCase());
        fireVisionSettingsChanged();
    }

    /**
     * @return an unmodifiable list of Boards loaded in the configuration
     */
    public List<Board> getBoards() {
        return Collections.unmodifiableList(new ArrayList<>(boards.values()));
    }

    /**
     * @return an unmodifiable list of Panels loaded in the configuration
     */
    public List<Panel> getPanels() {
        return Collections.unmodifiableList(new ArrayList<>(panels.values()));
    }

    /**
     * Signal that something about the vision settings has changed. Inheritance makes it hard to track how changes affect
     * single properties, therefore this is simply fired globally.
     */
    public void fireVisionSettingsChanged() {
        firePropertyChange("visionSettings", null, this.visionSettings);
    }

    public Machine getMachine() {
        return machine;
    }

    /**
     * Adds the specified Panel definition to the configuration
     * @param panel - the Panel definition to be added
     */
    public void addPanel(Panel panel) {
        LinkedHashMap<File, Panel> oldValue = new LinkedHashMap<>(panels);
        panels.put(panel.getFile(), panel);
        firePropertyChange("panels", oldValue, panels);
    }
    
    /**
     * Loads a Panel definition into the configuration if it is not already loaded
     * @param file - the file containing the Panel definition
     * @throws Exception if the specified file does not exist or does not contain a valid Panel
     */
    public void addPanel(File file) throws Exception {
        file = file.getCanonicalFile();
        if (panels.containsKey(file)) {
            return;
        }
        Panel panel = loadPanel(file);
        LinkedHashMap<File, Panel> oldValue = new LinkedHashMap<>(panels);
        panels.put(file, panel);
        firePropertyChange("panels", oldValue, panels);
    }
    
    /**
     * Removes the specified Panel definition from the configuration.  If the Panel definition has 
     * been flagged as being modified, a dialog is presented to confirm if the operator desires to 
     * save the Panel definition to the file system before it is removed.
     * @param panel - the Panel definition to remove
     */
    public void removePanel(Panel panel) {
        confirmSaveOfModified(panel);
        LinkedHashMap<File, Panel> oldValue = new LinkedHashMap<>(panels);
        panels.remove(panel.getFile());
        firePropertyChange("panels", oldValue, panels);
        panel.dispose();
    }
    
    /**
     * Returns the Panel definition contained in the specified file. If the Panel definition is 
     * already loaded in the configuration, it is found and returned. If it is not already loaded 
     * into the configuration, the specified file is read and loaded into the configuration.  If the
     * specified file does not exist, a new empty Panel definition is created and saved into the 
     * specified file and then is loaded into the configuration.
     * @param file - the file containing the Panel definition
     * @throws Exception if the specified file exists but does not contain a valid Panel
     */
    public Panel getPanel(File file) throws Exception {
        if (!file.exists()) {
            Panel panel = new Panel(file);
            panel.setName(file.getName());
            Serializer serializer = createSerializer();
            serializer.write(panel, file);
        }
        file = file.getCanonicalFile();
        if (panels.containsKey(file)) {
            return panels.get(file);
        }
        Panel panel = loadPanel(file);
        LinkedHashMap<File, Panel> oldValue = new LinkedHashMap<>(panels);
        panels.put(file, panel);
        firePropertyChange("panels", oldValue, panels);
        return panel;
    }
    
    /**
     * Adds the specified Board definition to the configuration
     * @param board - the Board definition to be added
     */
    public void addBoard(Board board) {
        LinkedHashMap<File, Board> oldValue = new LinkedHashMap<>(boards);
        boards.put(board.getFile(), board);
        firePropertyChange("boards", oldValue, boards);
    }
    
    /**
     * Loads a Board definition into the configuration if it is not already loaded
     * @param file - the file containing the Board definition
     * @throws Exception if the specified file does not exist or does not contain a valid Board
     */
    public void addBoard(File file) throws Exception {
        file = file.getCanonicalFile();
        if (boards.containsKey(file)) {
            return;
        }
        Board board = loadBoard(file);
        LinkedHashMap<File, Board> oldValue = new LinkedHashMap<>(boards);
        boards.put(file, board);
        firePropertyChange("boards", oldValue, boards);
    }
    
    /**
     * Removes the specified Board definition from the configuration. If the Board definition has 
     * been flagged as being modified, a dialog is presented to confirm if the operator desires to 
     * save the Board definition to the file system before it is removed.
     * @param board - the Board to remove
     */
    public void removeBoard(Board board) {
        confirmSaveOfModified(board);
        LinkedHashMap<File, Board> oldValue = new LinkedHashMap<>(boards);
        boards.remove(board.getFile());
        firePropertyChange("boards", oldValue, boards);
        board.dispose();
    }
    
    /**
     * Returns the Board definition contained in the specified file. If the Board definition is 
     * already loaded in the configuration, it is found and returned. If it is not already loaded, 
     * the specified file is read and loaded into the configuration.  If the specified file does not
     * exist, a new empty Board definition is created and saved into the specified file and then is
     * loaded into the configuration.
     * @param file - the file containing the Board definition
     * @return the Board definition
     * @throws Exception if the specified file exists but does not contain a valid Board definition
     */
    public Board getBoard(File file) throws Exception {
        if (!file.exists()) {
            Board board = new Board(file);
            board.setName(file.getName());
            Serializer serializer = createSerializer();
            serializer.write(board, file);
        }
        file = file.getCanonicalFile();
        if (boards.containsKey(file)) {
            return boards.get(file);
        }
        Board board = loadBoard(file);
        LinkedHashMap<File, Board> oldValue = new LinkedHashMap<>(boards);
        boards.put(file, board);
        firePropertyChange("boards", oldValue, boards);
        return board;
    }
    
    private static void serializeObject(Object o, File file) throws Exception {
        Serializer serializer = createSerializer();
        // This write forces any errors that will appear to happen before we start writing to
        // the file, which keeps us from writing a partial configuration to the real file.
        serializer.write(o, new ByteArrayOutputStream());
        FileOutputStream out = new FileOutputStream(file);
        serializer.write(o, out);
        out.write('\n');
        out.close();
    }

    private void loadMachine(File file) throws Exception {
        Serializer serializer = createSerializer();
        MachineConfigurationHolder holder = serializer.read(MachineConfigurationHolder.class, file);
        machine = holder.machine;
    }

    private void saveMachine(File file) throws Exception {
        MachineConfigurationHolder holder = new MachineConfigurationHolder();
        holder.machine = machine;
        serializeObject(holder, file);
    }

    private void loadPackages(File file) throws Exception {
        Serializer serializer = createSerializer();
        PackagesConfigurationHolder holder =
                serializer.read(PackagesConfigurationHolder.class, file);
        for (Package pkg : holder.packages) {
            addPackage(pkg);
        }
    }

    private void savePackages(File file) throws Exception {
        PackagesConfigurationHolder holder = new PackagesConfigurationHolder();
        holder.packages = new ArrayList<>(packages.values());
        serializeObject(holder, file);
    }

    private void loadParts(File file) throws Exception {
        Serializer serializer = createSerializer();
        PartsConfigurationHolder holder = serializer.read(PartsConfigurationHolder.class, file);
        for (Part part : holder.parts) {
            addPart(part);
        }
    }

    private void saveParts(File file) throws Exception {
        PartsConfigurationHolder holder = new PartsConfigurationHolder();
        holder.parts = new ArrayList<>(parts.values());
        serializeObject(holder, file);
    }

    /**
     * Loads the Boards listed in the specified file into the configuration.  Any Boards listed that
     * can't be loaded are skipped and an error message is logged.
     * @param file - the file containing the list of boards
     * @throws Exception - if the specified file can't be read successfully
     */
    private void loadBoards(File file) throws Exception {
        Serializer serializer = createSerializer();
        BoardsConfigurationHolder holder = serializer.read(BoardsConfigurationHolder.class, file);
        for (File boardFile : holder.boards) {
            try {
                addBoard(boardFile);
            }
            catch(FileNotFoundException e) {
                Logger.error("Could not load board " + boardFile.getCanonicalPath() + ", file is missing.");
            }
            catch(Exception e) {
                Logger.error("Could not load board " + boardFile.getCanonicalPath() + ", file may be corrupt.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the Boards that are currently loaded in the configuration to the specified file
     * @param file - the file in which to save the Boards
     * @throws Exception if the file can't be written successfully
     */
    private void saveBoards(File file) throws Exception {
        BoardsConfigurationHolder holder = new BoardsConfigurationHolder();
        holder.boards = new ArrayList<>(boards.keySet());
        serializeObject(holder, file);
        
        for (Board board : getBoards()) {
            confirmSaveOfModified(board);
        }
    }

    /**
     * Loads the Panels listed in the specified file into the configuration.  Any Panels listed that
     * can't be loaded are skipped and an error message is logged.
     * @param file - the file containing the list of panels
     * @throws Exception - if the specified file can't be read successfully
     */
    private void loadPanels(File file) throws Exception {
        Serializer serializer = createSerializer();
        PanelsConfigurationHolder holder = serializer.read(PanelsConfigurationHolder.class, file);
        for (File panelFile : holder.panels) {
            try {
                addPanel(panelFile);
            }
            catch(FileNotFoundException e) {
                Logger.error("Could not load panel " + panelFile.getCanonicalPath() + ", file is missing.");
            }
            catch(Exception e) {
                Logger.error("Could not load panel " + panelFile.getCanonicalPath() + ", file may be corrupt.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the Panels that are currently loaded in the configuration to the specified file
     * @param file - the file in which to save the Panels
     * @throws Exception if the file can't be written successfully
     */
    private void savePanels(File file) throws Exception {
        PanelsConfigurationHolder holder = new PanelsConfigurationHolder();
        holder.panels = new ArrayList<>(panels.keySet());
        serializeObject(holder, file);
        
        for (Panel panel : getPanels()) {
            confirmSaveOfModified(panel);
        }
    }

    private void confirmSaveOfModified(PlacementsHolder<?> placementsHolder) {
        if (placementsHolder.isDirty()) {
            int result = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Do you want to save your changes to " + placementsHolder.getFile().getName() + "?" //$NON-NLS-1$ //$NON-NLS-2$
                            + "\n" + "If you don't save, your changes will be lost.", //$NON-NLS-1$ //$NON-NLS-2$
                    "Save " + placementsHolder.getFile().getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    if (placementsHolder instanceof Board) {
                        saveBoard((Board) placementsHolder);
                    }
                    else if (placementsHolder instanceof Panel) {
                        savePanel((Panel)placementsHolder);
                    }
                    else {
                        throw new UnsupportedOperationException("Instance type " + placementsHolder.getClass() + " not supported.");
                    }
                }
                catch (Exception e) {
                    MessageBoxes.errorBox(MainFrame.get(), "Save Error", //$NON-NLS-1$
                            e.getMessage());
                }
            }
        }
    }
    
    private void loadVisionSettings(File file) throws Exception {
        Serializer serializer = createSerializer();
        VisionSettingsConfigurationHolder holder =
                serializer.read(VisionSettingsConfigurationHolder.class, file);
        for (AbstractVisionSettings visionSettings : holder.visionSettings) {
            addVisionSettings(visionSettings);
        }
    }

    private void saveVisionSettings(File file) throws Exception {
        VisionSettingsConfigurationHolder holder = new VisionSettingsConfigurationHolder();
        holder.visionSettings = new ArrayList<>(visionSettings.values());
        serializeObject(holder, file);
    }

    /**
     * Returns the Job contained within the specified file 
     * @param file - the file containing the Job
     * @return the Job
     * @throws Exception - if the file can't be read successfully
     */
    public Job loadJob(File file) throws Exception {
        Serializer serializer = createSerializer();
        Job job = serializer.read(Job.class, file);
        job.setFile(file);
        convertLegacyJob(job);
        
        job.rootPanelLocation.setPlacementsHolder(job.rootPanel);
        
        resolvePanel(job, job.getRootPanelLocation());
        restoreJobEnabledAndErrorHandlingSettings(job, job.getRootPanelLocation());
        
        Logger.trace("Dump of the jobRootPanelLocation");
        job.getRootPanelLocation().dump("");
        
        job.setDirty(job.getVersion() == null);
        
        return job;
    }

    /**
     * Converts jobs with deprecated lists of panels and/or boards to the newer format. If a legacy
     * panel is found, it is converted to a stand-alone panel and then is added to the job's new 
     * root panel. If a legacy panel is not found, any boards found in the job's deprecated list of
     * boards are moved to the job's new root panel. Note, it would be tempting to do all this in 
     * the job's Commit method but that doesn't work because at the time it runs, the job's filename
     * has not yet been set.
     * @param job - the job to convert
     * @throws Exception 
     */
    private static void convertLegacyJob(Job job) throws Exception {
        if (job.panels != null && !job.panels.isEmpty()) {
            Logger.info("The job file contains a legacy panel definition, it will be converted to the new format.");
            backupLegacyJob(job);
            //Convert deprecated list of Panels to list of PanelLocations - note that the legacy 
            //panelization only ever allowed one panel so we only need to handle that case here.
            
            //We need to create a new panel, populate it with the boards in the job, add the new 
            //panel to the configuration, and then add a panelLocation to the job's rootPanel that 
            //references it.
            
            //First we need the root board location for the panel, this is the one that originally 
            //set the origin of the legacy panel
            BoardLocation rootBoardLocation = job.boardLocations.get(0);
            
            //We need to resolve the board so we can get its dimensions
            Configuration configuration = Configuration.get();
            try {
                configuration.resolveBoard(job, rootBoardLocation);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            //Now create a file for the new panel, we'll just use the job's file name except 
            //change it to end with ".panel.xml"
            String jobFileName = job.getFile().getCanonicalPath();
            String panelFileName = jobFileName.substring(0, jobFileName.indexOf(".job.xml")) + ".panel.xml";
            Logger.info("A new file is being created to hold the panel definition: " + panelFileName);
            File panelFile = new File(panelFileName);
            
            Panel panel = job.panels.get(0);
            panel.setFile(panelFile);
            panel.setName(panelFile.getName());
            panel.setDefinition(panel);
            panel.setDimensions(Location.origin);
            
            Location rootDims = rootBoardLocation.getBoard().getDimensions().
                    convertToUnits(configuration.getSystemUnits());
            
            //We don't know the actual dimensions of the panel so we'll just set it to the bounding
            //box of the array of boards
            panel.setDimensions(Location.origin.deriveLengths(
                    rootDims.getLengthX().add(panel.xGap).multiply(panel.columns).subtract(panel.xGap),
                    rootDims.getLengthY().add(panel.yGap).multiply(panel.rows).subtract(panel.yGap),
                    null, null));

            //The panel fiducials for the bottom side need to moved to the bottom side and have
            //their coordinates adjusted (legacy panelization only had fiducials on the top side of
            //the panel regardless of which way the boards were facing).
            if (rootBoardLocation.getGlobalSide() == Side.Bottom) {
                for (Placement fiducial : panel.getPlacements()) {
                    Location loc = fiducial.getLocation();
                    loc = loc.deriveLengths(panel.getDimensions().getLengthX().subtract(loc.getLengthX()), null, null, null);
                    fiducial.setLocation(loc);
                    fiducial.setSide(Side.Bottom);
                }
            }
            
            double pcbStepX = rootDims.getLengthX().add(panel.xGap).getValue();
            double pcbStepY = rootDims.getLengthY().add(panel.yGap).getValue();
            
            for (int j = 0; j < panel.rows; j++) {
                for (int i = 0; i < panel.columns; i++) {
                    // deep copy the existing root board
                    BoardLocation newPcb = new BoardLocation(rootBoardLocation);
                    newPcb.setParent(null);
                    newPcb.setDefinition(newPcb);
                    newPcb.setGlobalSide(Side.Top);
                    newPcb.getPlaced().clear();
                    
                    // Offset the sub PCB
                    newPcb.setLocation(new Location(configuration.getSystemUnits(),
                                    pcbStepX * i,
                                    pcbStepY * j, 0, 0));
                    
                    panel.addChild(newPcb);
                    
                    String brdId = String.format("%s[%d,%d]", BoardLocation.ID_PREFIX, j+1, i+1);
                    newPcb.setId(brdId);
                    
                    int boardNum = j*panel.columns + (rootBoardLocation.getGlobalSide() == Side.Top ? i : panel.columns - 1 - i);
                    BoardLocation subBoard = job.boardLocations.get(boardNum);
                    
                    newPcb.setLocallyEnabled(subBoard.isLocallyEnabled());
                    
                    String keyRoot = PanelLocation.ID_PREFIX + "1" + 
                            PlacementsHolderLocation.ID_DELIMITTER + newPcb.getUniqueId() + 
                            PlacementsHolderLocation.ID_DELIMITTER;
                    Map<String, Boolean> subBoardPlaced = subBoard.getPlaced();
                    for (String key : subBoardPlaced.keySet()) {
                        job.placedStatusMap.put(keyRoot + key, subBoardPlaced.get(key));
                    }
                }
            }
            boolean savedCheckFids = panel.isCheckFiducials();
            
            configuration.savePanel(panel);
            configuration.addPanel(panel);
            
            PanelLocation panelLocation = new PanelLocation();
            panelLocation.setFileName(panelFileName);
            panelLocation.setGlobalLocation(rootBoardLocation.getGlobalLocation());
            panelLocation.setGlobalSide(rootBoardLocation.getGlobalSide());
            panelLocation.setCheckFiducials(savedCheckFids);
            job.rootPanel.addChild(panelLocation);
            Logger.info("The job file has been updated to use the new panel file.");
            job.dirty = true;
            job.panels = null;
            job.boardLocations = null;
        }
        else if (job.boardLocations != null){
            Logger.info("A legacy job file has been detected, it is being updated to the new format.");
            backupLegacyJob(job);
            //Add board locations to the root panel
            for (BoardLocation boardLocation : job.boardLocations) {
                boardLocation.setParent(job.rootPanelLocation);
                job.rootPanel.addChild(boardLocation);
                
                //Move the deprecated placement status from the boardLocation to the job
                Map<String, Boolean> temp = boardLocation.getPlaced();
                if (temp != null) {
                    for (String placementId : temp.keySet()) {
                        job.storePlacedStatus(boardLocation, placementId, temp.get(placementId));
                    }
                }
            }
            job.dirty = true;
            job.panels = null;
            job.boardLocations = null;
        }
    }
    
    /**
     * Creates a backup copy of the existing job.xml file with a .legacy.job.xml extension
     * @param job - the job to backup
     * @throws Exception
     */
    private static void backupLegacyJob(Job job) throws Exception {
        File file = job.getFile();
        String fileName = file.getName();
        String backupFileName = fileName.substring(0, fileName.indexOf(".job.xml")) + ".legacy.job.xml";
        File backupFile = new File(file.getParentFile(), backupFileName);
        Files.copy(Paths.get(file.toURI()), Paths.get(backupFile.toURI()), 
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        Logger.info("A backup of the legacy job file has been copied to: " + backupFile.getCanonicalPath());
    }
    
    /**
     * Attempts to find the Board definition associated with the given BoardLocation, create a copy
     * of it, and then assign the copy to the given BoardLocation
     * @param job - the Job
     * @param boardLocation - the BoardLocation
     * @throws Exception if the Board definition associated with the BoardLocation can't be loaded 
     * successfully
     */
    public void resolveBoard(Job job, BoardLocation boardLocation) throws Exception {
        String boardFilename = boardLocation.getBoardFile();
        // First see if we can find the board at the given filename
        // If the filename is not absolute this will be relative
        // to the working directory
        File boardFile = new File(boardFilename);
        if (!boardFile.exists() && boardLocation.getParent() != null) {
            //If that didn't work, try to find it in the parent's panel directory
            boardFile = new File(boardLocation.getParent().getPlacementsHolder().getFile().
                    getParentFile(), boardFilename);
        }
        if (!boardFile.exists() && job != null) {
            // If that fails, see if we can find it relative to the
            // directory the job was in
            boardFile = new File(job.getFile().getParentFile(), boardFilename);
        }
        if (!boardFile.exists()) {
            throw new Exception("Board file not found: " + boardFilename);
        }
        
        Board boardDefinition = getBoard(boardFile);
        Board board = boardLocation.getBoard();
        if (board == null || board.getDefinition() != boardDefinition) {
            //Create a deep copy of the board's definition and assign it to the BoardLocation
            board = new Board(boardDefinition);
            boardLocation.setBoard(board);
        }
        
        if (job != null) {
            boardLocation.addPropertyChangeListener(job);
            board.addPropertyChangeListener(job);
        }
    }

    /**
     * Attempts to find the Panel definition associated with the given PanelLocation, create a copy
     * of it, and assign the copy to the PanelLocation. Recursively resolves the Panel's children.
     * @param job - the Job
     * @param panelLocation - the PanelLocation
     * @throws Exception if the Panel and all of its descendants can't be loaded successfully
     */
    public void resolvePanel(Job job, PanelLocation panelLocation) throws Exception {
        if (panelLocation == null) {
            return;
        }
        Panel panel = panelLocation.getPanel();
        if (job != null && panelLocation == job.getRootPanelLocation()) {
            panel.setFile(job.getFile());
        }
        else {
            String panelFileName = panelLocation.getFileName();
            File panelFile = new File(panelFileName);
            if (!panelFile.exists() && panelLocation.getParent() != null) {
                //If that didn't work, try to find it in the parent panel directory
                panelFile = new File(panelLocation.getParent().getPanel().getFile().getParentFile(), panelFileName);
            }
            if (!panelFile.exists() && job != null) {
                // If that fails, see if we can find it relative to the
                // directory the job was in
                panelFile = new File(job.getFile().getParentFile(), panelFileName);
            }
            if (!panelFile.exists()) {
                throw new Exception("Panel file not found: " + panelFileName);
            }
            
            Panel panelDefinition = getPanel(panelFile);
            if (panel == null || panel.getDefinition() != panelDefinition) {
                //Create a deep copy of the Panel definition and assign it to the PanelLocation
                panel = new Panel(panelDefinition);
                panelLocation.setPanel(panel);
            }
        }
        
        if (job != null) {
            panelLocation.addPropertyChangeListener(job);
            panel.addPropertyChangeListener(job);
        }

        //Recursively resolve all the panel's children
        for (PlacementsHolderLocation<?> child : panel.getChildren()) {
            if (child instanceof PanelLocation) {
                PanelLocation childPanelLocation = (PanelLocation) child;
                childPanelLocation.setParent(panelLocation);
                resolvePanel(job, childPanelLocation);
            }
            else if (child instanceof BoardLocation) {
                BoardLocation boardLocation = (BoardLocation) child;
                boardLocation.setParent(panelLocation);
                resolveBoard(job, boardLocation);
            }
            else {
                throw new UnsupportedOperationException("Instance type " + child.getClass() + " not supported.");
            }
        }
        
        panel.setDirty(false);
    }
    
    /**
     * Checks to see if the specified PlacementsHolder is used by the current job or any of the 
     * currently loaded panels
     * @param placementsHolder - the PlacementsHolder to check
     * @return true if the PlacementsHolder is in use
     */
    public boolean isInUse(PlacementsHolder<?> placementsHolder) {
        if (MainFrame.get().getJobTab().getJob().instanceCount(placementsHolder) > 0) {
            return true;
        }
        for (Panel panel : getPanels()) {
            if (panel.getDefinition() != placementsHolder.getDefinition() && 
                    panel.getInstanceCount(placementsHolder) > 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Recursively checks the enabled and error handling settings of all the boards, panels, and 
     * placements of a job to see if they match that of the defining boards and panels. If not, 
     * those that are different than their definition, are saved into the job so that when the 
     * job is reloaded, their settings can be restored.
     * @param job - the job to check
     * @param panelLocation - the PanelLocation where the checking should begin
     */
    private static void saveJobEnabledAndErrorHandlingSettings(Job job, PanelLocation panelLocation) {
        if (panelLocation == job.getRootPanelLocation()) {
            //Clear everything when starting from the root panel so that we don't end up with a mix
            //of old and new settings
            job.removeAllEnabledState();
            job.removeAllErrorHandlingState();
        }
        if (panelLocation != null) {
            Panel panel = panelLocation.getPanel();
            if (panel != null) {
                for (PlacementsHolderLocation<?> child : panel.getChildren()) {
                    if (child.isLocallyEnabled() != child.getDefinition().isLocallyEnabled()) {
                        job.storeEnabledState(child, null, child.isLocallyEnabled());
                    }
                    if (child.isCheckFiducials() != child.getDefinition().isCheckFiducials()) {
                        job.storeCheckFiducialsState(child, child.isCheckFiducials());
                    }
                    if (child instanceof BoardLocation || child instanceof PanelLocation) {
                        for (Placement placement : child.getPlacementsHolder().getPlacements()) {
                            if (placement.isEnabled() != ((Placement) placement.getDefinition()).isEnabled()) {
                                job.storeEnabledState(child, placement, placement.isEnabled());
                            }
                            if (placement.getErrorHandling() != ((Placement) placement.getDefinition()).getErrorHandling()) {
                                job.storeErrorHandlingState(child, placement, placement.getErrorHandling());
                            }
                        }
                        if (child instanceof PanelLocation) {
                            saveJobEnabledAndErrorHandlingSettings(job, (PanelLocation) child);
                        }
                    }
                    else {
                        throw new UnsupportedOperationException("Instance type " + child.getClass() + " not supported.");
                    }
                }
            }
        }
    }
    
    /**
     * Recursively restores the enabled and error handling settings of all the boards, panels, and 
     * placements of a job.
     * @param job - the job to restore
     * @param panelLocation - the PanelLocation where the restoration should begin
     */
    private static void restoreJobEnabledAndErrorHandlingSettings(Job job, PanelLocation panelLocation) {
        if (panelLocation != null) {
            Panel panel = panelLocation.getPanel();
            if (panel != null) {
                for (PlacementsHolderLocation<?> child : panel.getChildren()) {
                    child.setLocallyEnabled(job.retrieveEnabledState(child, null));
                    child.setCheckFiducials(job.retrieveCheckFiducialsState(child));
                    if (child instanceof BoardLocation || child instanceof PanelLocation) {
                        if (child.getPlacementsHolder() != null) {
                            for (Placement placement : child.getPlacementsHolder().getPlacements()) {
                                placement.setEnabled(job.retrieveEnabledState(child, placement));
                                placement.setErrorHandling(job.retrieveErrorHandlingState(child, placement));
                            }
                        }
                        if (child instanceof PanelLocation) {
                            restoreJobEnabledAndErrorHandlingSettings(job, (PanelLocation) child);
                        }
                    }
                    else {
                        throw new UnsupportedOperationException("Instance type " + child.getClass() + " not supported.");
                    }
                }
            }
        }
    }
    
    /**
     * Serializes the specified job and writes it to the specified file
     * @param job - the job to save
     * @param file - the file into which the job is to be saved
     * @throws Exception if the file can't be written successfully
     */
    public void saveJob(Job job, File file) throws Exception {
        saveJobEnabledAndErrorHandlingSettings(job, job.getRootPanelLocation());
        Serializer serializer = createSerializer();
        serializer.write(job, new ByteArrayOutputStream());
        serializer.write(job, file);
        job.setFile(file);
        job.setDirty(false);
    }
    
    public String getImgurClientId() {
        return imgurClientId;
    }

    /**
     * Saves the specified Panel into its file
     * @param panel - the Panel to save
     * @throws Exception if the file can't be written successfully
     */
    public void savePanel(Panel panel) throws Exception {
        Serializer serializer = createSerializer();
        serializer.write(panel, new ByteArrayOutputStream());
        serializer.write(panel, panel.getFile());
        panel.setDirty(false);
    }

    /**
     * Returns the Panel definition contained in the specified file.  Recursively loads the Panel's
     * descendants.
     * @param file - the file containing the Panel definition
     * @return the Panel definition
     * @throws Exception if the specified file can't be read successfully or if any of the
     * descendants of the panel can't be found
     */
    private Panel loadPanel(File file) throws Exception {
        Serializer serializer = createSerializer();
        Panel panel = serializer.read(Panel.class, file);
        panel.setFile(file);
        for (PlacementsHolderLocation<?> child : panel.getChildren()) {
            File childFile = new File(child.getFileName());
            if (!childFile.exists()) {
                childFile = new File(file.getParentFile(), childFile.getName());
            }
            if (childFile.exists()) {
                if (child instanceof BoardLocation) {
                    child.setPlacementsHolder(new Board(getBoard(childFile)));
                }
                else if (child instanceof PanelLocation) {
                    child.setPlacementsHolder(new Panel(getPanel(childFile)));
                    PanelLocation.setParentsOfAllDescendants((PanelLocation) child);
                }
                else {
                    throw new UnsupportedOperationException("Instance type " + child.getClass() + " not supported.");
                }
            }
            else {
                throw new Exception(String.format("Unable to find child %s of panel %s", childFile.getCanonicalPath(), file.getCanonicalPath()));
            }
        }
        for (String id : panel.getPseudoPlacementIds()) {
            panel.addPseudoPlacement(panel.createPseudoPlacement(id));
        }
        panel.setDirty(false);
        return panel;
    }
    
    /**
     * Saves the specified Board into its file
     * @param panel - the Board to save
     * @throws Exception if the file can't be written successfully
     */
    public void saveBoard(Board board) throws Exception {
        Serializer serializer = createSerializer();
        serializer.write(board, new ByteArrayOutputStream());
        serializer.write(board, board.getFile());
        board.setDirty(false);
    }

    /**
     * Creates and returns a Board based on the contents of the specified file
     * @param file - the file to read
     * @return the Board
     * @throws Exception if the specified file can't be read successfully
     */
    private Board loadBoard(File file) throws Exception {
        Serializer serializer = createSerializer();
        Board board = serializer.read(Board.class, file);
        board.setFile(file);
        board.setDirty(false);
        return board;
    }

    public static Serializer createSerializer() {
        Style style = new HyphenStyle();
        Format format = new Format(style);
        AnnotationStrategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, format);
        return serializer;
    }

    public static String createId(String prefix) {
        // NanosecondTime guarantees unique Ids, even if created in rapid succession such as in migration code.
        return prefix + NanosecondTime.get().toString(16);
    }

    /**
     * Used to provide a fixed root for the Machine when serializing.
     */
    @Root(name = "openpnp-machine")
    public static class MachineConfigurationHolder {
        @Element
        private Machine machine;
    }

    /**
     * Used to provide a fixed root for the Packages when serializing.
     */
    @Root(name = "openpnp-packages")
    public static class PackagesConfigurationHolder {
        @ElementList(inline = true, entry = "package", required = false)
        private ArrayList<Package> packages = new ArrayList<>();
    }

    /**
     * Used to provide a fixed root for the Parts when serializing.
     */
    @Root(name = "openpnp-parts")
    public static class PartsConfigurationHolder {
        @ElementList(inline = true, entry = "part", required = false)
        private ArrayList<Part> parts = new ArrayList<>();
    }

    /**
     * Used to provide a fixed root for the Boards when serializing.
     */
    @Root(name = "openpnp-boards")
    public static class BoardsConfigurationHolder {
        @ElementList(inline = true, entry = "board", required = false)
        private ArrayList<File> boards = new ArrayList<>();
    }

    /**
     * Used to provide a fixed root for the Panels when serializing.
     */
    @Root(name = "openpnp-panels")
    public static class PanelsConfigurationHolder {
        @ElementList(inline = true, entry = "panel", required = false)
        private ArrayList<File> panels = new ArrayList<>();
    }

    /**
     * Used to provide a fixed root for the VisionSettings when serializing.
     */
    @Root(name = "openpnp-vision-settings")
    public static class VisionSettingsConfigurationHolder {
        @ElementList(inline = true, entry = "visionSettings", required = false)
        public ArrayList<AbstractVisionSettings> visionSettings = new ArrayList<>();
    }

}
