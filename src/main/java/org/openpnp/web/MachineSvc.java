package org.openpnp.web;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Getter;
import lombok.Setter;

import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;

// https://github.com/jasonray/jersey-starterkit/wiki/Serializing-a-POJO-to-xml-or-json-using-JAXB
@Path("/machine")
public class MachineSvc {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MachineDto get() {
        return new MachineDto(Configuration.get().getMachine());
    }
    
    @POST
    public void post() throws Exception {
        Configuration.get().getMachine().setEnabled(true);
    }
    
    @Getter
    @Setter
    public static class MachineDto {
        private boolean enabled;
        private Map<String, String> heads;
        private Map<String, String> cameras;
        private Map<String, String> feeders;
        
        public MachineDto() {
            
        }
        
        public MachineDto(Machine machine) {
            enabled = machine.isEnabled();
            heads = new IdToNameMapGen<Head>().getMap(machine.getHeads());
            cameras = new IdToNameMapGen<Camera>().getMap(machine.getCameras());
            feeders = new IdToNameMapGen<Feeder>().getMap(machine.getFeeders());
        }
    }
}
