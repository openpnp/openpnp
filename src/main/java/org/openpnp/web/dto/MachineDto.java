package org.openpnp.web.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;

@Data
public class MachineDto {
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