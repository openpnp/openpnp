package org.openpnp.web.dto;

import lombok.Data;

import org.openpnp.spi.Camera;

@Data
public class CameraDto {
    private String id;
    private String name;
    
    public CameraDto() {
        
    }
    
    public CameraDto(Camera camera) {
        id = camera.getId();
        name = camera.getName();
    }
}