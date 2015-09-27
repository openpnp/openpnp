package org.openpnp.web;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openpnp.model.Configuration;
import org.openpnp.web.dto.MachineDto;

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
}
