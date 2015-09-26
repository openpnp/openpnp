package org.openpnp.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Getter;
import lombok.Setter;

import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;

@Path("/machine/heads")
public class HeadSvc {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<HeadDto> get() {
        List<HeadDto> res = new ArrayList<>();
        for (Head head : Configuration.get().getMachine().getHeads()) {
            res.add(new HeadDto(head));
        }
        return res;
    }
    
    @GET
    @Path("/{headId}")
    @Produces(MediaType.APPLICATION_JSON)
    public HeadDto get(@PathParam("headId") String headId) {
        return new HeadDto(Configuration.get().getMachine().getHead(headId));
    }
    
    @Getter
    @Setter
    public static class HeadDto {
        private String id;
        private String name;
        private Map<String, String> cameras;
        
        public HeadDto() {
            
        }
        
        public HeadDto(Head head) {
            id = head.getId();
            name = head.getName();
            cameras = new IdToNameMapGen<Camera>().getMap(head.getCameras());
        }
    }
}
