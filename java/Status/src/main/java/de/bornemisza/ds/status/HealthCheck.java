package de.bornemisza.ds.status;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Stateless
@Path("status")
public class HealthCheck {
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHealthReport() {
        return "Status: OK";
    }

}
