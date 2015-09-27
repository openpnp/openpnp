package org.openpnp.web.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;

@Data
public class HeadDto {
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