package com.sibilante.houseseats.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sibilante.houseseats.model.Show;
import com.sibilante.houseseats.service.IgnoreShowInterface;
import com.sibilante.houseseats.service.IgnoreShowService;

@Path("/ignore/shows")
@Produces(MediaType.APPLICATION_JSON)
public class IgnoreShowResource {
	
	private IgnoreShowInterface ignoreShow = new IgnoreShowService();
	
	@GET
    public Response getShows(){
		List<Show> shows = ignoreShow.findAll();
		GenericEntity<List<Show>> genericEntity = new GenericEntity<List<Show>>(shows) {};
        return Response.ok(genericEntity).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Show addShow(Show show){
        ignoreShow.create(show);
        return show;
    }
    
    @DELETE
    @Path("/{id}")
    public void deleteShow(@PathParam("id")long id) {
    	ignoreShow.delete(id);
    }
    
    //TODO authentication using https://cloud.google.com/appengine/docs/standard/java/users/
}
