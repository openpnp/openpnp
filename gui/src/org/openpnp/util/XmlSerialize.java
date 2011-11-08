package org.openpnp.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class XmlSerialize {
	public static String serialize(Object o) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		XMLEncoder xmlEncoder = new XMLEncoder(bOut);
		xmlEncoder.writeObject(o);
		xmlEncoder.close();
		return bOut.toString();
	}
	
	public static Object deserialize(String s) {
		XMLDecoder xmlDecoder = new XMLDecoder(new ByteArrayInputStream(s.getBytes()));
		return xmlDecoder.readObject();
	}
}
