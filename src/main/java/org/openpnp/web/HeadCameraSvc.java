package org.openpnp.web;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Getter;
import lombok.Setter;

import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;

@Path("/machine/heads/{headId}/cameras")
public class HeadCameraSvc {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CameraDto> get(@PathParam("headId") String headId) {
        List<CameraDto> res = new ArrayList<>();
        for (Camera camera : Configuration.get().getMachine().getHead(headId).getCameras()) {
            res.add(new CameraDto(camera));
        }
        return res;
    }
    
    @GET
    @Path("/{cameraId}")
    @Produces(MediaType.APPLICATION_JSON)
    public CameraDto get(@PathParam("headId") String headId, @PathParam("cameraId") String cameraId) {
        return new CameraDto(Configuration.get().getMachine().getHead(headId).getCamera(cameraId));
    }
    
    @GET
    @Path("/{cameraId}/stream.html")
    @Produces("text/html")
    public String getStreamHtml(@PathParam("headId") String headId, @PathParam("cameraId") String cameraId) throws Exception {
        return "<html><body><img src=\"stream.mjpeg\"></body></html>";
    }

    @GET
    @Path("/{cameraId}/stream.mjpeg")
    @Produces("text/plain")
    public Response getStreamMjpeg(@PathParam("headId") String headId, @PathParam("cameraId") String cameraId) throws Exception {
        Camera camera = Configuration.get().getMachine().getHead(headId).getCamera(cameraId);
        return new MjpegCameraResponse(camera, 10).getResponse();
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
}
