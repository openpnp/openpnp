package org.openpnp.model.eagle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.openpnp.model.eagle.xml.Board;
import org.openpnp.model.eagle.xml.Drawing;
import org.openpnp.model.eagle.xml.Eagle;
import org.openpnp.model.eagle.xml.Layers;
import org.openpnp.model.eagle.xml.Library;
import org.openpnp.model.eagle.xml.Package;
import org.openpnp.model.eagle.xml.Packages;
import org.openpnp.model.eagle.xml.Schematic;
import org.openpnp.model.eagle.xml.Smd;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class EagleLoader {
	
    private static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
    private static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    public Layers		layers;
    public Eagle		eagle; // TODO remove eagle as this is not strictly required as we peel out the underlying parts, this is the superset
    public Board 		board;
    public Library 		library;
    public Schematic 	schematic;
    
    public EagleLoader(File file) throws Exception {
        this(new FileInputStream(file));
    }

    public EagleLoader(InputStream in) throws Exception {

        String packageName = "org.openpnp.model.eagle.xml";

        JAXBContext ctx = JAXBContext.newInstance(packageName);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        XMLReader xmlreader = XMLReaderFactory.createXMLReader();
        xmlreader.setFeature(FEATURE_NAMESPACES, true);
        xmlreader.setFeature(FEATURE_NAMESPACE_PREFIXES, true);
        xmlreader.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException, IOException {
                InputSource input = new InputSource(ClassLoader.getSystemResourceAsStream("eagle.dtd"));
                input.setPublicId(publicId);
                input.setSystemId(systemId);
                return input;
            }
        });

        InputSource input = new InputSource(in);
        Source source = new SAXSource(xmlreader, input);

        eagle = (Eagle) unmarshaller.unmarshal(source); // TODO change later to    Eagle eagle = (Eagle) unmarshaller.unmarshal(source);
        Drawing 	drawing 	= (Drawing) 	eagle.getCompatibilityOrDrawing().get(0);
        
      //Now see what we have
        try {
        	layers 		= (Layers) 		drawing.getLayers();
        	// Now we need to extract the details
		}
		catch (Exception e) {
			//There were no Layers in the input file
		}
        
        //Now see what we have
        try {
        	board 		= (Board) 		drawing.getLibraryOrSchematicOrBoard().get(0);
        	// Now we need to extract the details
		}
		catch (Exception e) {
			//There were no Boards in the input file
		}

        try {
        	schematic 	= (Schematic) 	drawing.getLibraryOrSchematicOrBoard().get(0);
		}
		catch (Exception e) {
			//There were no Schematics in the input file
		}

        try { 	
        	library 	= (Library) 	drawing.getLibraryOrSchematicOrBoard().get(0);
        
        	Packages packages = library.getPackages();
        
        	System.out.println("<openpnp-packages>");
        
        	for (Package pkg : packages.getPackage()) {
        		System.out.println(String.format("<package id=\"%s\" name=\"%s\">", pkg.getName(), pkg.getName()));
        		System.out.println(String.format("<footprint units=\"Millimeters\">"));
        		for (Object o : pkg.getPolygonOrWireOrTextOrDimensionOrCircleOrRectangleOrFrameOrHoleOrPadOrSmd()) {
        			if (o instanceof Smd) {
        				Smd smd = (Smd) o;
        				System.out.println(String.format("<pad x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"/>", smd.getX(), smd.getY(), smd.getDx(), smd.getDy()));
        			}
        		}
        		System.out.println(String.format("</footprint>"));
        		System.out.println("</package>");
        	}
        
        	System.out.println("</openpnp-packages>");
		}
		catch (Exception e) {
			//There were no Libraries in the input file
		}	
    }
}
