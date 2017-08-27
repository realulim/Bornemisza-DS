package de.bornemisza.users.endpoint;

import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.bornemisza.users.boundary.BusinessException;
import de.bornemisza.users.boundary.BusinessException.Type;
import de.bornemisza.users.boundary.UnauthorizedException;
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
    public User getUser(@PathParam("name") String userName,
                        @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(userName)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        User user;
        try {
            user = facade.getUser(userName, authHeader);
        }
        catch (UnauthorizedException ex) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (user == null) throw new WebApplicationException(Status.NOT_FOUND);
        else return user;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response userAccountCreationRequest(User user) {
        if (user == null || user.getEmail() == null) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity("No User or E-Mail missing!").build());
        }
        try {
            facade.addUser(user);
            return Response.accepted().build();
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
    }

    @GET
    @Path("confirmation/user/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public User confirmUser(@PathParam("uuid") String uuidStr) {
        validateUuid(uuidStr);
        Function confirmUserFunction = new Function<UsersFacade, User>() {
            @Override
            public User apply(UsersFacade facade) {
                return facade.confirmUser(uuidStr);
            }
        };
        String expiryMsg = "User Account Creation Request does not exist - maybe expired?";
        String conflictMsg = "User already exists!";
        return executeConfirmation(confirmUserFunction, expiryMsg, conflictMsg);
    }

    @PUT
    @Path("{name}/email/{newemail}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeEmailRequest(@PathParam("name") String userName, 
                           @PathParam("newemail") String email,
                           @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        InternetAddress newEmail;
        try {
            newEmail = isVoid(email) ? null : new InternetAddress(email);
        }
        catch (AddressException ae) {
            newEmail = null;
        }
        if (newEmail == null || isVoid(userName)) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity("UserName or E-Mail missing or unparseable!").build());
        }
        User user;
        try {
            user = facade.getUser(userName, authHeader);
            if (user == null) throw new WebApplicationException(Status.NOT_FOUND);
            else user.setEmail(newEmail);
        }
        catch (UnauthorizedException e) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        try {
            facade.changeEmail(user);
            return Response.accepted().build();
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
    }

    @GET
    @Path("confirmation/email/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public User confirmEmail(@PathParam("uuid") String uuidStr,
                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        validateUuid(uuidStr);
        Function confirmEmailFunction = new Function<UsersFacade, User>() {
            @Override
            public User apply(UsersFacade facade) {
                return facade.confirmEmail(uuidStr, authHeader);
            }
        };
        String expiryMsg = "Email Change Request does not exist - maybe expired?";
        String conflictMsg = "Newer Revision exists!";
        return executeConfirmation(confirmEmailFunction, expiryMsg, conflictMsg);
    }

    @PUT
    @Path("{name}/password/{newpassword}")
    @Produces(MediaType.APPLICATION_JSON)
    public User changePassword(@PathParam("name") String userName, 
                           @PathParam("newpassword") String password,
                           @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(userName) || isVoid(password)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        User user;
        try {
            user = facade.getUser(userName, authHeader);
            if (user == null) throw new WebApplicationException(Status.NOT_FOUND);
            else user.setPassword(password.toCharArray());
        }
        catch (UnauthorizedException e) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        try {
            user = facade.changePassword(user, user.getRevision(), authHeader);
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (user == null) {
            throw new WebApplicationException(
                Response.status(Status.CONFLICT).entity("Newer Revision exists!").build());
        }
        return user;
    }
    
    @DELETE
    @Path("{name}")
    public void deleteUser(@PathParam("name") String name, 
                           @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(name)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        else {
            User user;
            try {
                user = facade.getUser(name, authHeader);
                if (user == null) throw new WebApplicationException(Status.NOT_FOUND);
            }
            catch (UnauthorizedException e) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }
            boolean success;
            try {
                success = facade.deleteUser(name, user.getRevision(), authHeader);
            }
            catch (RuntimeException ex) {
                throw new WebApplicationException(
                        Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
            }
            if (! success) {
                throw new WebApplicationException(
                        Response.status(Status.CONFLICT).entity("Newer Revision exists!").build());
            }
        }
    }

    private boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

    private void validateUuid(String uuidStr) throws WebApplicationException {
        UUID uuid;
        try {
            uuid = isVoid(uuidStr) ? null : UUID.fromString(uuidStr);
        }
        catch (IllegalArgumentException iae) {
            uuid = null;
        }
        if (uuid == null) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity("UUID missing or unparseable!").build());
        }
    }

    private User executeConfirmation(Function<UsersFacade, User> function, String expiryMsg, String conflictMsg) {
        User confirmedUser = null;
        try {
            confirmedUser = function.apply(facade);
        }
        catch (BusinessException e) {
            Status status = e.getType() == Type.UUID_NOT_FOUND ? Status.NOT_FOUND : Status.INTERNAL_SERVER_ERROR;
            throw new WebApplicationException(
                    Response.status(status).entity(expiryMsg).build());
        }
        catch (RuntimeException ex) {
            throw new WebApplicationException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (confirmedUser == null) {
            throw new WebApplicationException(
                    Response.status(Status.CONFLICT).entity(conflictMsg).build());
        }
        return confirmedUser;
    }

}
