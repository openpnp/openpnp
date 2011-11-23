package org.openpnp.app;

import java.io.StringWriter;
import java.util.HashMap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class Test {
	public static void main(String[] args) throws Exception {
		Serializer serializer = new Persister();
		StringWriter writer = new StringWriter();
		serializer.write(new Machine(), writer);
		System.out.println(writer.toString());
	}
	
	@Root
	public static class Machine {
		@ElementMap(attribute=true, key="id")
		private HashMap<String, Head> heads = new HashMap<String, Head>();
		
		public Machine() {
			heads.put("1", new Head("1"));
			heads.put("2", new Head("2"));
		}
	}
	
	public static class Head {
		@Attribute
		private String id;
		
		public Head(String id) {
			this.id = id;
		}
	}
}
