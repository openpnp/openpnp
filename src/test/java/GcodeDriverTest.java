import java.io.File;

import org.junit.Test;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.machine.reference.driver.TcpCommunications;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.GcodeServer;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

import com.google.common.io.Files;

public class GcodeDriverTest {
    @Test
    public void testSimpleJob() throws Exception {
        /**
         * Configure logging to show everything.
         */
        Configurator.currentConfig()
                    .formatPattern("{date:yyyy-MM-dd HH:mm:ss.SSS} {class_name} {level}: {message}")
                    .level(Level.TRACE)
                    .activate();

        /**
         * Set up a TCP based GcodeServer that we'll connect to from GcodeDriver.
         */
        GcodeServer server = new GcodeServer();
        server.addCommandResponse("G21 ; Set millimeters mode", "ok");
        server.addCommandResponse("G90 ; Set absolute positioning mode", "ok");
        server.addCommandResponse("M82 ; Set absolute mode for extruder", "ok");
        server.addCommandResponse("G28 ; Home all axes", "ok");
        server.addCommandResponse("G0 X10.0000 Y10.0000 Z10.0000 E10.0000 F1000 ; Send standard Gcode move", "ok");
        server.addCommandResponse("M400 ; Wait for moves to complete before returning", "ok");
        server.addCommandResponse("READ A1", "read:a1:497\nok");
        
        /**
         * Create a new config directory and load the default configuration.
         */
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        /**
         * Create a GcodeDriver and set it up to connect to the GcodeServer that was just
         * created.
         */
        GcodeDriver gcodeDriver = new GcodeDriver();
        gcodeDriver.createDefaults();
        gcodeDriver.setCommunicationsType("tcp");
        TcpCommunications tcp = (TcpCommunications) gcodeDriver.getCommunications();
        tcp.setIpAddress("localhost");
        tcp.setPort(server.getListenerPort());

        /**
         * Create an Actuator for reading, and configure the GcodeDriver's command and regex for it.
         */
        Machine machine = Configuration.get().getMachine();
        Actuator actuator = new ReferenceActuator();
        machine.addActuator(actuator);
        gcodeDriver.setCommand(actuator, CommandType.ACTUATOR_READ_COMMAND, "READ A1");
        gcodeDriver.setCommand(actuator, CommandType.ACTUATOR_READ_REGEX, "read:a1:(?<Value>-?\\d+)");

        /**
         * Configure the machine to use the new GcodeDriver and initialize the machine.
         */
        ReferenceMachine referenceMachine = (ReferenceMachine) machine;
        referenceMachine.setDriver(gcodeDriver);
        machine.setEnabled(true);
        machine.home();

        /**
         * Attempt to read the actuator we configured.
         */
        actuator.read();
        
        server.shutdown();
    }

    private Job createSimpleJob() {
        Job job = new Job();

        Board board = new Board();
        board.setName("test");

        board.addPlacement(createPlacement("R1", "R-0805-10K", 10, 10, 0, 45, Side.Top));
        board.addPlacement(createPlacement("R2", "R-0805-10K", 20, 20, 0, 90, Side.Top));

        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
        boardLocation.setSide(Side.Top);

        job.addBoardLocation(boardLocation);

        return job;
    }

    public static Placement createPlacement(String id, String partId, double x, double y, double z,
            double rotation, Side side) {
        Placement placement = new Placement(id);
        placement.setPart(Configuration.get().getPart(partId));
        placement.setLocation(new Location(LengthUnit.Millimeters, x, y, z, rotation));
        placement.setSide(side);
        return placement;
    }
}