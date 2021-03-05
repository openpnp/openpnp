import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.swing.Action;
import javax.swing.Icon;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.HttpActuator;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.util.VisionUtils;

import com.google.common.io.Files;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class HttpActuatorTest {
	
	@Before
	public void before() throws Exception {
		/**
		 * Create a new config directory and load the default configuration.
		 */
		File workingDirectory = Files.createTempDir();
		workingDirectory = new File(workingDirectory, ".openpnp");
		System.out.println("Configuration directory: " + workingDirectory);
		Configuration.initialize(workingDirectory);
		Configuration.get().load();

	}
	 
	 
    @Test
    public void testOffsets() {
        TestHttpServer server=new TestHttpServer ();
        HttpActuator actuator=new HttpActuator();
        
        
        actuator.setRegex("read:(?<Value>-?\\d+)");
        actuator.setReadUrl("http://127.0.0.1:3042/msr");
        
        String stringResult="";
        try {
            Configuration.get().getMachine().setEnabled(true);
            stringResult= Configuration.get().getMachine().execute(() -> {
                 return  actuator.read();
            }, false, 0);
      
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Double result=Double.parseDouble(stringResult);
        
        Assert.assertEquals(result, Double.valueOf( 42.0));
       
    }
    static class TestHttpServer   {
        TestHttpServer() {
            HttpServer server;
            try {
                server = HttpServer.create(new InetSocketAddress(3042), 0);
                server.createContext("/msr", new MyHandler());
                server.setExecutor(null); // creates a default executor
                server.start();  
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
          
        }
        static class MyHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange t) throws IOException {
                String response = "read:42";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

           
        }
    }

}
