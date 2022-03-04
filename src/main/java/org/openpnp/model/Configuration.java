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

package org.openpnp.model;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.commons.io.FileUtils;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.ThemeInfo;
import org.openpnp.gui.components.ThemeSettingsPanel;
import org.openpnp.model.Board.Side;
import org.openpnp.scripting.Scripting;
import org.openpnp.spi.Machine;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.ResourceUtils;
import org.openpnp.util.Utils2D;
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

    private static final String PREF_TABLE_LINKS = "Configuration.tableLinks";

    private static final String PREF_THEME_INFO = "Configuration.theme.info";
    private static final String PREF_THEME_FONT_SIZE = "Configuration.theme.fontSize";
    private static final String PREF_THEME_ALTERNATE_ROWS = "Configuration.theme.alternateRows";

    private static final String PREF_LENGTH_DISPLAY_FORMAT = "Configuration.lengthDisplayFormat";
    private static final String PREF_LENGTH_DISPLAY_FORMAT_DEF = "%.3f";

    private static final String PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS =
            "Configuration.lengthDisplayFormatWithUnits";
    private static final String PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS_DEF = "%.3f%s";

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

    public String getLengthDisplayFormatWithUnits() {
        return prefs.get(PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS,
                PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS_DEF);
    }

    public void setLengthDisplayFormatWithUnits(String format) {
        prefs.put(PREF_LENGTH_DISPLAY_FORMAT_WITH_UNITS, format);
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

    public List<Board> getBoards() {
        return Collections.unmodifiableList(new ArrayList<>(boards.values()));
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
        panels.put(file, panel);
        firePropertyChange("panels", null, panels);
        return panel;
    }
    
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
        boards.put(file, board);
        firePropertyChange("boards", null, boards);
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

    public Job loadJob(File file) throws Exception {
        Serializer serializer = createSerializer();
        Job job = serializer.read(Job.class, file);
        job.setFile(file);

        
        resolvePanels(job, job.panelLocations);
        
        // Once the Job is loaded we need to resolve any Boards that it
        // references.
        resolveBoards(job);

        for (PanelLocation panelLocation : job.panelLocations) {
            job.getRootPanelLocation().addChild(new PanelLocation(panelLocation));
        }
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            job.getRootPanelLocation().addChild(new BoardLocation(boardLocation));
        }
        
        Logger.trace("Dump of the job panelLocations");
        for (PanelLocation panelLocation : job.panelLocations) {
            panelLocation.dump("");
        }
        Logger.trace("Dump of the job boardLocations");
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            boardLocation.dump("");
        }
        
        Logger.trace("Now a dump of the jobRootPanelLocation");
        job.getRootPanelLocation().dump("");
        
        job.setDirty(false);
        
        return job;
    }

    private void resolveBoards(Job job) throws Exception {
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            resolveBoard(job, boardLocation);
        }
    }
    
    public void resolveBoard(Job job, BoardLocation boardLocation) throws Exception {
        String boardFilename = boardLocation.getBoardFile();
        // First see if we can find the board at the given filename
        // If the filename is not absolute this will be relative
        // to the working directory
        File boardFile = new File(boardFilename);
        if (!boardFile.exists() && boardLocation.getParent() != null) {
            //If that didn't work, try to find it in the parent panel directory
            boardFile = new File(boardLocation.getParent().getFiducialLocatable().getFile().getParentFile(), boardFilename);
        }
        if (!boardFile.exists()) {
            // If that fails, see if we can find it relative to the
            // directory the job was in
            boardFile = new File(job.getFile().getParentFile(), boardFilename);
        }
        if (!boardFile.exists()) {
            throw new Exception("Board file not found: " + boardFilename);
        }
        Board board = getBoard(boardFile);
        boardLocation.setBoard(board);
    }

    public void resolvePanels(Job job, List<PanelLocation> panelLocations) throws Exception {
        if (panelLocations == null || panelLocations.isEmpty()) {
            return;
        }
        for (PanelLocation panelLocation : panelLocations) {
            resolvePanel(job, panelLocation);
        }
    }
    
    public void resolvePanel(Job job, PanelLocation panelLocation) throws Exception {
        if (panelLocation == null) {
            return;
        }
        Panel panel;
        if (panelLocation.getPanel() == null || panelLocation.getPanel().getVersion() != null) {
            String panelFileName = panelLocation.getFileName();
            File panelFile = new File(panelFileName);
            if (!panelFile.exists() && panelLocation.getParent() != null) {
                //If that didn't work, try to find it in the parent panel directory
                panelFile = new File(panelLocation.getParent().getPanel().getFile().getParentFile(), panelFileName);
            }
            if (!panelFile.exists()) {
                // If that fails, see if we can find it relative to the
                // directory the job was in
                panelFile = new File(job.getFile().getParentFile(), panelFileName);
            }
            if (!panelFile.exists()) {
                throw new Exception("Panel file not found: " + panelFileName);
            }
            panel = getPanel(panelFile);
            panelLocation.setPanel(panel);
        }
        else {
            //This fixes old style panels that were part of the job file - sets the panel file 
            //and moves the boards from the job to the panel
            
            //First resolve the root PCB as we need its dimensions
            BoardLocation rootPcb = job.getBoardLocations().get(0);
            resolveBoard(job, rootPcb);
            
            panel = panelLocation.getPanel();
            String boardFileName = rootPcb.getFileName();
            String panelFileName = boardFileName.substring(0, boardFileName.indexOf(".board.xml")) + ".panel.xml";
            File panelFile = new File(job.getFile().getParentFile(), panelFileName);
            panelLocation.setFileName(panelFileName);
            panelLocation.setParent(null);
            panel.setName(panelFileName);
            panel.setFile(panelFile);
            
            Location rootDims = rootPcb.getBoard().getDimensions().
                    convertToUnits(Configuration.get().getSystemUnits());
            
            double pcbStepX = rootDims.getLengthX().add(panel.xGap).getValue();
            double pcbStepY = rootDims.getLengthY().add(panel.yGap).getValue();
          
            for (int j = 0; j < panel.rows; j++) {
                for (int i = 0; i < panel.columns; i++) {
                    // deep copy the existing rootPcb
                    BoardLocation newPcb = new BoardLocation(rootPcb);
//                    newPcb.setLocation(rootPcb.getLocation().derive(0.0, 0.0, null, 0.0));
                    newPcb.setLocation(Location.origin);
                    
                    // Offset the sub PCB
                    newPcb.setLocation(newPcb.getLocation()
                            .add(new Location(Configuration.get().getSystemUnits(),
                                    pcbStepX * i,
                                    pcbStepY * j, 0, 0)));
                    
                    panel.getChildren().add(newPcb);
                }
            }
            panel.setDimensions(Location.origin.deriveLengths(
                rootDims.getLengthX().add(panel.xGap).multiply(panel.columns).subtract(panel.xGap),
                rootDims.getLengthY().add(panel.yGap).multiply(panel.rows).subtract(panel.yGap),
                null, null));
            
            //Remove all the old boards from the job - they will get added back below
            job.removeAllBoards();
        }
        
        PanelLocation newPanelLocation = new PanelLocation(panelLocation);
        for (FiducialLocatableLocation child : panel.getChildren()) {
            if (child instanceof PanelLocation) {
                PanelLocation childPanelLocation = new PanelLocation((PanelLocation) child);
                childPanelLocation.setParent(panelLocation);
                
                resolvePanel(job, childPanelLocation);
                
                ((PanelLocation) child).setPanel(childPanelLocation.getPanel());
            }
            if (child instanceof BoardLocation) {
                BoardLocation boardLocation = new BoardLocation((BoardLocation) child);
                boardLocation.setParent(panelLocation);

                resolveBoard(job, boardLocation);
                
                ((BoardLocation) child).setBoard(boardLocation.getBoard());
            }
        }
    }
    
    public void saveJob(Job job, File file) throws Exception {
        Serializer serializer = createSerializer();

        Set<Panel> panels = new HashSet<>();
        // Fix the paths to any panels in the Job
        for (PanelLocation panelLocation : job.getPanelLocations()) {
            Panel panel = (Panel) panelLocation.getPanel();
            panels.add(panel);
            try {
                String relativePath = ResourceUtils.getRelativePath(
                        panel.getFile().getAbsolutePath(), file.getAbsolutePath(), File.separator);
                panelLocation.setPanelFile(relativePath);
            }
            catch (ResourceUtils.PathResolutionException ex) {
                panelLocation.setPanelFile(panel.getFile().getAbsolutePath());
            }
        }
        // Save any panels in the job
        for (Panel panel : panels) {
            savePanel(panel);
        }

        List<BoardLocation> savedBoardLocations = job.getBoardLocations();
        Set<Board> boards = new HashSet<>();
        // Fix the paths to any boards in the Job
        for (BoardLocation boardLocation : savedBoardLocations) {
            Board board = (Board) boardLocation.getBoard();
            boards.add(board);
            try {
                String relativePath = ResourceUtils.getRelativePath(
                        board.getFile().getAbsolutePath(), file.getAbsolutePath(), File.separator);
                boardLocation.setBoardFile(relativePath);
            }
            catch (ResourceUtils.PathResolutionException ex) {
                boardLocation.setBoardFile(board.getFile().getAbsolutePath());
            }
            if (boardLocation.getParent() != null) {
                job.removeBoardLocation(boardLocation);
            }
        }
        // Save any boards in the job
        for (Board board : boards) {
            saveBoard(board);
        }
        
        // Save the job - minus any board locations that are owned by a panel
        serializer.write(job, new ByteArrayOutputStream());
        serializer.write(job, file);
        job.setFile(file);
        job.setDirty(false);
        
        // Restore the board locations
        job.removeAllBoards();
        for (BoardLocation boardLocation : savedBoardLocations) {
            job.addBoardLocation(boardLocation);
        }
    }
    
    public String getImgurClientId() {
        return imgurClientId;
    }

    public void savePanel(Panel panel) throws Exception {
        Serializer serializer = createSerializer();
        serializer.write(panel, new ByteArrayOutputStream());
        serializer.write(panel, panel.getFile());
        panel.setDirty(false);
    }

    private Panel loadPanel(File file) throws Exception {
        Serializer serializer = createSerializer();
        Panel panel = serializer.read(Panel.class, file);
        panel.setFile(file);
        panel.setDirty(false);
        return panel;
    }

    public void saveBoard(Board board) throws Exception {
        Serializer serializer = createSerializer();
        serializer.write(board, new ByteArrayOutputStream());
        serializer.write(board, board.getFile());
        board.setDirty(false);
    }

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
     * Used to provide a fixed root for the VisionSettings when serializing.
     */
    @Root(name = "openpnp-vision-settings")
    public static class VisionSettingsConfigurationHolder {
        @ElementList(inline = true, entry = "visionSettings", required = false)
        public ArrayList<AbstractVisionSettings> visionSettings = new ArrayList<>();
    }

}
