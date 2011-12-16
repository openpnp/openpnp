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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.openpnp.spi.Machine;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

public class Configuration {
	private LinkedHashMap<String, Package> packages = new LinkedHashMap<String, Package>();
	private LinkedHashMap<String, Part> parts = new LinkedHashMap<String, Part>();
	private Machine machine;
	private HashMap<File, Board> boards = new HashMap<File, Board>();
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
		
		loadMachine(new File(configurationDirectory, "machine.xml"));
		loadPackages(new File(configurationDirectory, "packages.xml"));
		loadParts(new File(configurationDirectory, "parts.xml"));
		dirty = false;
		for (ConfigurationListener listener : listeners) {
			listener.configurationLoaded(this);
		}
	}
	
	public void save(String configurationDirectoryPath) throws Exception {
		File configurationDirectory = new File(configurationDirectoryPath);
		
		saveMachine(new File(configurationDirectory, "machine.xml"));
		savePackages(new File(configurationDirectory, "packages.xml"));
		saveParts(new File(configurationDirectory, "parts.xml"));
		dirty = false;
	}
	
	public Package getPackage(String id) {
		return packages.get(id);
	}
	
	public Part getPart(String id) {
		return parts.get(id);
	}
	
	public Collection<Part> getParts() {
		return Collections.unmodifiableCollection(parts.values());
	}
	
	public void addPart(Part part) {
		parts.put(part.getId(), part);
		dirty = true;
	}
	
	public Machine getMachine() {
		return machine;
	}
	
	public Board getBoard(File file) throws Exception {
		file = file.getCanonicalFile();
		if (boards.containsKey(file)) {
			System.out.println("loaded " + file.getCanonicalPath() + " from cache");
			return boards.get(file);
		}
		Board board = loadBoard(file);
		boards.put(file, board);
		System.out.println("loaded " + file.getCanonicalPath() + " from filesystem");
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
		serializer.write(holder, file);
		
//		StringWriter writer = new StringWriter();
//		serializer.write(holder, writer);
//		System.out.println(writer.toString());
	}
	
	private void loadPackages(File file) throws Exception {
		Serializer serializer = createSerializer();
		PackagesConfigurationHolder holder = serializer.read(PackagesConfigurationHolder.class, file);
		for (Package pkg : holder.packages) {
			packages.put(pkg.getId(), pkg);
		}
	}
	
	private void savePackages(File file) throws Exception {
		Serializer serializer = createSerializer();
		PackagesConfigurationHolder holder = new PackagesConfigurationHolder();
		holder.packages = new ArrayList<Package>(packages.values());
		serializer.write(holder, file);
		
//		StringWriter writer = new StringWriter();
//		serializer.write(holder, writer);
//		System.out.println(writer.toString());
	}
	
	private void loadParts(File file) throws Exception {
		Serializer serializer = createSerializer();
		PartsConfigurationHolder holder = serializer.read(PartsConfigurationHolder.class, file);
		for (Part part : holder.parts) {
			part.resolve(this);
			parts.put(part.getId(), part);
		}
	}
	
	private void saveParts(File file) throws Exception {
		Serializer serializer = createSerializer();
		PartsConfigurationHolder holder = new PartsConfigurationHolder();
		holder.parts = new ArrayList<Part>(parts.values());
		serializer.write(holder, file);

//		StringWriter writer = new StringWriter();
//		serializer.write(holder, writer);
//		System.out.println(writer.toString());
	}
	
	public Job loadJob(File file) throws Exception {
		Serializer serializer = createSerializer();
		Job job = serializer.read(Job.class, file);
		
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
		
//		StringWriter writer = new StringWriter();
//		serializer.write(job, writer);
//		System.out.println(writer.toString());
		
		return job;
	}
	
	private void saveJob(Job job, File file) {
		Serializer serializer = createSerializer();
		// Fix the paths to any boards in the Job
	}
	
	private Board loadBoard(File file) throws Exception {
		Serializer serializer = createSerializer();
		Board board = serializer.read(Board.class, file);
		board.resolve(this);
		
//		StringWriter writer = new StringWriter();
//		serializer.write(board, writer);
//		System.out.println(writer.toString());
		
		return board;
	}
	
	public static Serializer createSerializer() {
		Style style = new HyphenStyle();
		Format format = new Format(style);
		AnnotationStrategy strategy = new AnnotationStrategy();
		Serializer serializer = new Persister(strategy, format);
		return serializer;
	}
	
	@Root(name="openpnp-machine")
	public static class MachineConfigurationHolder {
		@Element
		private Machine machine;
	}
	
	@Root(name="openpnp-packages")
	public static class PackagesConfigurationHolder {
		@ElementList(inline=true, entry="package")
		private ArrayList<Package> packages;
	}
	
	@Root(name="openpnp-parts")
	public static class PartsConfigurationHolder {
		@ElementList(inline=true, entry="part")
		private ArrayList<Part> parts;
	}
}
