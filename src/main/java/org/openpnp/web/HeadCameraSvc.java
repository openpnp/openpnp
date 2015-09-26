package org.openpnp.web;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Getter;
import lombok.Setter;

import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.openpnp.util.VisionUtils;

@Path("/machine/heads/{headId}/cameras")
public class HeadCameraSvc {
    @POST
    @Path("/{cameraId}/jog")
    @Consumes(MediaType.APPLICATION_JSON)
    public void jogToPoint(
            @PathParam("headId") String headId, 
            @PathParam("cameraId") String cameraId,
            PointDto point) throws Exception {
        Machine machine = Configuration.get().getMachine();
        Camera camera = machine.getHead(headId).getCamera(cameraId);
        if (!machine.isEnabled()) {
            machine.setEnabled(true);
        }
        Location location = VisionUtils.getPixelLocation(camera, point.getX(), point.getY());
        camera.moveTo(location, 1.0);
    }
    
    @GET
    @Path("/{cameraId}/stream")
    @Produces("text/plain")
    public Response getStreamMjpeg(@PathParam("headId") String headId, @PathParam("cameraId") String cameraId) throws Exception {
        Camera camera = Configuration.get().getMachine().getHead(headId).getCamera(cameraId);
        return new MjpegCameraResponse(camera, 10).getResponse();
    }
    
    
    @Getter
    @Setter
    public static class PointDto {
        private int x;
        private int y;
    }
}
