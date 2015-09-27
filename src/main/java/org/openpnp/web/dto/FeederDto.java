package org.openpnp.web.dto;

import lombok.Data;

import org.openpnp.spi.Feeder;

@Data
public class FeederDto {
    private String id;
    private String name;
    
    public FeederDto() {
        
    }
    
    public FeederDto(Feeder feeder) {
        this.id = feeder.getId();
        this.name = feeder.getName();
    }
}