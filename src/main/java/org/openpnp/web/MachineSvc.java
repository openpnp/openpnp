package org.openpnp.web;

import java.util.ArrayList;
import java.util.List;

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
        private List<HeadDto> heads;
        private List<CameraDto> cameras;
        private List<FeederDto> feeders;
        
        public MachineDto() {
            
        }
        
        public MachineDto(Machine machine) {
            enabled = machine.isEnabled();
            heads = new ArrayList<>();
            for (Head head : machine.getHeads()) {
                heads.add(new HeadDto(head));
            }
            cameras = new ArrayList<>();
            for (Camera camera : machine.getCameras()) {
                cameras.add(new CameraDto(camera));
            }
            feeders = new ArrayList<>();
            for (Feeder feeder : machine.getFeeders()) {
                feeders.add(new FeederDto(feeder));
            }
        }
    }
    
    @Getter
    @Setter
    public static class HeadDto {
        private String id;
        private String name;
        private List<CameraDto> cameras;
        
        public HeadDto() {
            
        }
        
        public HeadDto(Head head) {
            id = head.getId();
            name = head.getName();
            cameras = new ArrayList<>();
            for (Camera camera : head.getCameras()) {
                cameras.add(new CameraDto(camera));
            }
        }
    }

    @Getter
    @Setter
    public static class CameraDto {
        private String id;
        private String name;
        
        public CameraDto() {
            
        }
        
        public CameraDto(Camera camera) {
            id = camera.getId();
            name = camera.getName();
        }
    }
    
    @Getter
    @Setter
    public static class FeederDto {
        private String id;
        private String name;
        
        public FeederDto() {
            
        }
        
        public FeederDto(Feeder feeder) {
            this.id = feeder.getId();
            this.name = feeder.getName();
        }
    }
}
