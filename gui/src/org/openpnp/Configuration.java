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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
	private static Configuration instance;
	
	private HashMap<String, Package> packages = new HashMap<String, Package>();
	private HashMap<String, Part> parts = new HashMap<String, Part>();
	private Machine machine;
	private HashMap<File, Board> boards = new HashMap<File, Board>();
	
	public static Configuration get() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}
	
	private Configuration() {
		
	}
	
	public void load(String configurationDirectoryPath) throws Exception {
		File configurationDirectory = new File(configurationDirectoryPath);
		
		loadMachine(new File(configurationDirectory, "machine.xml"));
		loadPackages(new File(configurationDirectory, "packages.xml"));
		loadParts(new File(configurationDirectory, "parts.xml"));
	}
	
	public void save(String configurationDirectoryPath) throws Exception {
		File configurationDirectory = new File(configurationDirectoryPath);
		
		saveMachine(new File(configurationDirectory, "machine.xml"));
		savePackages(new File(configurationDirectory, "packages.xml"));
		saveParts(new File(configurationDirectory, "parts.xml"));
	}
	
	public Package getPackage(String id) {
		return packages.get(id);
	}
	
	public Part getPart(String id) {
		return parts.get(id);
	}
	
	public Collection<Part> getParts() {
		return parts.values();
	}
	
	public Machine getMachine() {
		return machine;
	}
	
	public Board getBoard(String filename) throws Exception {
		File file = new File(filename);
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
		
		StringWriter writer = new StringWriter();
		serializer.write(job, writer);
		System.out.println(writer.toString());
		
		return job;
	}
	
	private Board loadBoard(File file) throws Exception {
		Serializer serializer = createSerializer();
		Board board = serializer.read(Board.class, file);
		
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
