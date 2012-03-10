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

package org.openpnp.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.openpnp.ConfigurationListener;
import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.spi.Machine;
import org.openpnp.util.ResourceUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

public class Configuration extends AbstractModelObject {
	private LinkedHashMap<String, Package> packages = new LinkedHashMap<String, Package>();
	private LinkedHashMap<String, Part> parts = new LinkedHashMap<String, Part>();
	private Machine machine;
	private LinkedHashMap<File, Board> boards = new LinkedHashMap<File, Board>();
	private boolean dirty;
	private Set<ConfigurationListener> listeners = new HashSet<ConfigurationListener>();
	
	
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public void addListener(ConfigurationListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(ConfigurationListener listener) {
		listeners.remove(listener);
	}
	
	public void load(String configurationDirectoryPath) throws Exception {
		File configurationDirectory = new File(configurationDirectoryPath);
		
		try {
			loadMachine(new File(configurationDirectory, "machine.xml"));
		}
		catch (Exception e) {
			String message = e.getMessage();
			if (e.getCause() != null && e.getCause().getMessage() != null) {
				message = e.getCause().getMessage();
			}
			throw new Exception("Error while reading machine.xml (" + message + ")", e);
		}
		try {
			loadPackages(new File(configurationDirectory, "packages.xml"));
		}
		catch (Exception e) {
			String message = e.getMessage();
			if (e.getCause() != null && e.getCause().getMessage() != null) {
				message = e.getCause().getMessage();
			}
			throw new Exception("Error while reading packages.xml (" + message + ")", e);
		}
		try {
			loadParts(new File(configurationDirectory, "parts.xml"));
		}
		catch (Exception e) {
			String message = e.getMessage();
			if (e.getCause() != null && e.getCause().getMessage() != null) {
				message = e.getCause().getMessage();
			}
			throw new Exception("Error while reading parts.xml (" + message + ")", e);
		}
		dirty = false;
		for (ConfigurationListener listener : listeners) {
			listener.configurationLoaded(this);
		}
	}
	
	public void save(String configurationDirectoryPath) throws Exception {
		File configurationDirectory = new File(configurationDirectoryPath);
		
		try {
			saveMachine(new File(configurationDirectory, "machine.xml"));
		}
		catch (Exception e) {
			throw new Exception("Error while saving machine.xml (" + e.getMessage() + ")", e);
		}
		try {
			savePackages(new File(configurationDirectory, "packages.xml"));
		}
		catch (Exception e) {
			throw new Exception("Error while saving packages.xml (" + e.getMessage() + ")", e);
		}
		try {
			saveParts(new File(configurationDirectory, "parts.xml"));
		}
		catch (Exception e) {
			throw new Exception("Error while saving parts.xml (" + e.getMessage() + ")", e);
		}
		dirty = false;
	}
	
	public Package getPackage(String id) {
		if (id == null) {
			return null;
		}
		return packages.get(id.toUpperCase());
	}
	
	public Part getPart(String id) {
		if (id == null) {
			return null;
		}
		return parts.get(id.toUpperCase());
	}
	
	public Collection<Part> getParts() {
		return Collections.unmodifiableCollection(parts.values());
	}
	
	public void addPart(Part part) {
		parts.put(part.getId().toUpperCase(), part);
		part.addPropertyChangeListener("id", partIdPcl);
		dirty = true;
		firePropertyChange("parts", null, parts);
	}
	
	public List<Board> getBoards() {
		return Collections.unmodifiableList(new ArrayList<Board>(boards.values()));
	}
	
	public Machine getMachine() {
		return machine;
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
	
	private void loadMachine(File file) throws Exception {
		Serializer serializer = createSerializer();
		MachineConfigurationHolder holder = serializer.read(MachineConfigurationHolder.class, file);
		machine = holder.machine;
		if (machine instanceof RequiresConfigurationResolution) {
			((RequiresConfigurationResolution) machine).resolve(this);
		}
	}
	
	private void saveMachine(File file) throws Exception {
		MachineConfigurationHolder holder = new MachineConfigurationHolder();
		holder.machine = machine;
		Serializer serializer = createSerializer();
		serializer.write(holder, new ByteArrayOutputStream());
		serializer.write(holder, file);
	}
	
	private void loadPackages(File file) throws Exception {
		Serializer serializer = createSerializer();
		PackagesConfigurationHolder holder = serializer.read(PackagesConfigurationHolder.class, file);
		for (Package pkg : holder.packages) {
			packages.put(pkg.getId().toUpperCase(), pkg);
		}
	}
	
	private void savePackages(File file) throws Exception {
		Serializer serializer = createSerializer();
		PackagesConfigurationHolder holder = new PackagesConfigurationHolder();
		holder.packages = new ArrayList<Package>(packages.values());
		serializer.write(holder, new ByteArrayOutputStream());
		serializer.write(holder, file);
	}
	
	private void loadParts(File file) throws Exception {
		Serializer serializer = createSerializer();
		PartsConfigurationHolder holder = serializer.read(PartsConfigurationHolder.class, file);
		for (Part part : holder.parts) {
			part.resolve(this);
			addPart(part);
		}
	}
	
	private void saveParts(File file) throws Exception {
		Serializer serializer = createSerializer();
		PartsConfigurationHolder holder = new PartsConfigurationHolder();
		holder.parts = new ArrayList<Part>(parts.values());
		serializer.write(holder, new ByteArrayOutputStream());
		serializer.write(holder, file);
	}
	
	public Job loadJob(File file) throws Exception {
		Serializer serializer = createSerializer();
		Job job = serializer.read(Job.class, file);
		job.setFile(file);
		
		// Once the Job is loaded we need to resolve any Boards that it
		// references.
		for (BoardLocation boardLocation : job.getBoardLocations()) {
			String boardFilename = boardLocation.getBoardFile();
			// First see if we can find the board at the given filename
			// If the filename is not absolute this will be relative
			// to the working directory
			File boardFile = new File(boardFilename);
			if (!boardFile.exists()) {
				// If that fails, see if we can find it relative to the
				// directory the job was in
				boardFile = new File(file.getParentFile(), boardFilename);
			}
			if (!boardFile.exists()) {
				throw new Exception("Board file not found: " + boardFilename);
			}
			Board board = getBoard(boardFile);
			boardLocation.setBoard(board);
		}
		
		job.setDirty(false);
		
		return job;
	}
	
	public void saveJob(Job job, File file) throws Exception {
		Serializer serializer = createSerializer();
		Set<Board> boards = new HashSet<Board>();
		// Fix the paths to any boards in the Job
		for (BoardLocation boardLocation : job.getBoardLocations()) {
			Board board = boardLocation.getBoard();
			boards.add(board);
			try {
				String relativePath = ResourceUtils.getRelativePath(
						board.getFile().getAbsolutePath(), 
						file.getAbsolutePath(), 
						File.separator);
				boardLocation.setBoardFile(relativePath);
			}
			catch (ResourceUtils.PathResolutionException ex) {
				boardLocation.setBoardFile(board.getFile().getAbsolutePath());
			}
		}
		// Save the job
		serializer.write(job, new ByteArrayOutputStream());
		serializer.write(job, file);
		job.setFile(file);
		job.setDirty(false);
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
		board.resolve(this);
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
	
	/**
	 * Listens for changes in a Part's Id field and resets it's Id in
	 * the parts Map if it changes.
	 */
	private PropertyChangeListener partIdPcl = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Part part = (Part) evt.getSource();
			parts.remove(part);
			parts.put(part.getId().toUpperCase(), part);
		}
	};
	
	/**
	 * Used to provide a fixed root for the Machine when serializing. 
	 */
	@Root(name="openpnp-machine")
	public static class MachineConfigurationHolder {
		@Element
		private Machine machine;
	}
	
	/**
	 * Used to provide a fixed root for the Packages when serializing. 
	 */
	@Root(name="openpnp-packages")
	public static class PackagesConfigurationHolder {
		@ElementList(inline=true, entry="package")
		private ArrayList<Package> packages;
	}
	
	/**
	 * Used to provide a fixed root for the Parts when serializing. 
	 */
	@Root(name="openpnp-parts")
	public static class PartsConfigurationHolder {
		@ElementList(inline=true, entry="part")
		private ArrayList<Part> parts;
	}
}
