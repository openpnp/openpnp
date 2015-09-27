package org.openpnp.web;

import java.io.File;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.openpnp.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static Logger logger;
    
    public static String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "INTERNAL BUILD";
        }
        return version;
    }
    
    public static void main(String[] args) throws Exception {
        File configurationDirectory = new File(System.getProperty("user.home"));
        configurationDirectory = new File(configurationDirectory, ".openpnp");
        
        if (System.getProperty("configDir") != null) {
            configurationDirectory = new File(System.getProperty("configDir"));
        }
        
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
        
        // We don't create a logger until log4j has been configured or it tries
        // to configure itself.
        logger = LoggerFactory.getLogger(Main.class);
        
        logger.debug(String.format("OpenPnP Web %s Started.", Main.getVersion()));
        
        Configuration.initialize(configurationDirectory);
        Configuration.get().load();
        
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create("http://0.0.0.0:8080/api"), 
                new ResourceConfig().packages("org.openpnp.web"));
        
        server.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(
                        ClassLoader.getSystemClassLoader(), 
                        "/web/"),
                "/");
        
        // For development, comment out the CLStaticStaticHttpHandler above use
        // the below so restart is not required
        
//        StaticHttpHandler handler = new StaticHttpHandler("/Users/jason/Projects/openpnp/openpnp/src/main/resources/web");
//        handler.setFileCacheEnabled(false);
//        server.getServerConfiguration().addHttpHandler(handler, "/");
    }
}
