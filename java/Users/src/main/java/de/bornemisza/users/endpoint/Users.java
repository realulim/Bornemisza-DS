package de.bornemisza.users.endpoint;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.bornemisza.users.boundary.UsersFacade;
import de.bornemisza.users.entity.User;

@Path("/")
public class Users {

    @Inject
    UsersFacade facade;

    public Users() { }

    // Constructor for Unit Tests
    public Users(UsersFacade facade) {
        this.facade = facade;
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@PathParam("name") String userName) {
        User user = null;
        try {
            user = facade.getUser(userName);
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        if (user == null) throw new WebApplicationException(Status.NOT_FOUND);
        else return user;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public User createUser(User user) {
        if (user == null) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity("No User to create!").build());
        }
        User createdUser = null;
        try {
            createdUser = facade.createUser(user);
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        if (createdUser == null) {
            throw new WebApplicationException(
                    Response.status(Status.CONFLICT).entity("User already exists!").build());
        }
        return createdUser;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public User updateUser(User user) {
        if (user == null) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity("No User to update!").build());
        }
        else {
            User updatedUser = null;
            try {
                updatedUser = facade.updateUser(user);
            }
            catch (RuntimeException ex) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            if (updatedUser == null) {
                throw new WebApplicationException(
                    Response.status(Status.CONFLICT).entity("Newer Revision exists!").build());
            }
            return updatedUser;
        }
    }

    @DELETE
    @Path("{name}/{rev}")
    public void deleteUser(@PathParam("name") String name, @PathParam("rev") String revision) {
        if (name == null || revision == null || facade.getUser(name) == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        else {
            boolean success;
            try {
                success = facade.deleteUser(name, revision);
            }
            catch (RuntimeException ex) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
            if (! success) {
                throw new WebApplicationException(Status.CONFLICT);
            }
        }
    }

}
