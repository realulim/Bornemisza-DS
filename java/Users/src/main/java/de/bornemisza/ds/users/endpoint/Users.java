package de.bornemisza.ds.users.endpoint;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.EmailAddress;
import de.bornemisza.rest.entity.User;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.ds.users.boundary.UsersFacade;
import de.bornemisza.ds.users.boundary.UsersType;
import static de.bornemisza.ds.users.boundary.UsersType.EMAIL_ALREADY_EXISTS;
import static de.bornemisza.ds.users.boundary.UsersType.USER_ALREADY_EXISTS;

@Stateless
@Path("/")
public class Users {

    @Inject
    UsersFacade facade;

    public Users() {
    }

    // Constructor for Unit Tests
    public Users(UsersFacade facade) {
        this.facade = facade;
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("name") String userName,
                        @HeaderParam(HttpHeaders.COOKIE) String cookie,
                        @HeaderParam(HttpHeaders.CTOKEN) String ctoken) {
        if (isVoid(userName) || hasIllegalCharacters(userName)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        User user;
        try {
            user = facade.getUser(userName, new DoubleSubmitToken(cookie, ctoken));
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        if (user == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        else {
            user.setStatus(Status.OK);
            return user.toResponse();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userAccountCreationRequest(User user) {
        if (user == null || user.getEmail() == null) {
            return Response.status(Status.BAD_REQUEST).entity("No User or E-Mail missing!").build();
        }
        else if (hasIllegalCharacters(user.getName())) {
            return Response.status(Status.BAD_REQUEST).entity("Illegal Characters in User Name!").build();
        }
        Consumer userAccountCreationRequestConsumer = new Consumer<UsersFacade>() {
            @Override
            public void accept(UsersFacade facade) {
                facade.addUser(user);
            }
        };
        return executeRequest(userAccountCreationRequestConsumer);
    }

    @GET
    @Path("confirmation/user/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmUser(@PathParam("uuid") String uuidStr) {
        if (! validateUuid(uuidStr)) {
            return Response.status(Status.BAD_REQUEST).entity("UUID missing or unparseable!").build();
        }
        Function confirmUserFunction = new Function<UsersFacade, Response>() {
            @Override
            public Response apply(UsersFacade facade) {
                User user = facade.confirmUser(uuidStr);
                if (user == null) return Response.status(Status.CONFLICT).build();
                else return Response.ok().entity(user).build();
            }
        };
        String expiryMsg = "User Account Creation Request does not exist - maybe expired?";
        String conflictMsg = "User already exists!";
        return executeConfirmation(confirmUserFunction, expiryMsg, conflictMsg);
    }

    @PUT
    @Path("{name}/email")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response changeEmailRequest(@PathParam("name") String userName,
                                       String emailStr,
                                       @HeaderParam(HttpHeaders.COOKIE) String cookie,
                                       @HeaderParam(HttpHeaders.CTOKEN) String ctoken) {
        if (isVoid(userName)) return Response.status(Status.BAD_REQUEST).build();
        EmailAddress email = validateEmail(emailStr);
        if (email == null) {
            return Response.status(Status.BAD_REQUEST).entity("E-Mail missing or unparseable!").build();
        }
        User user;
        try {
            user = facade.getUser(userName, new DoubleSubmitToken(cookie, ctoken));
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        if (user == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        user.setEmail(email);
        Consumer changeEmailRequestConsumer = new Consumer<UsersFacade>() {
            @Override
            public void accept(UsersFacade facade) {
                facade.changeEmail(user);
            }
        };
        return executeRequest(changeEmailRequestConsumer);
    }

    @GET
    @Path("confirmation/email/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmEmail(@PathParam("uuid") String uuidStr,
                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (! validateUuid(uuidStr)) {
            return Response.status(Status.BAD_REQUEST).entity("UUID missing or unparseable!").build();
        }
        Function confirmEmailFunction = new Function<UsersFacade, Response>() {
            @Override
            public Response apply(UsersFacade facade) {
                User user = facade.confirmEmail(uuidStr, authHeader);
                if (user == null) return Response.status(Status.CONFLICT).build();
                else return Response.ok().entity(user).build();
            }
        };
        String expiryMsg = "E-Mail Change Request does not exist - maybe expired?";
        String conflictMsg = "Newer Revision exists!";
        return executeConfirmation(confirmEmailFunction, expiryMsg, conflictMsg);
    }

    @PUT
    @Path("{name}/password/{newpassword}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(@PathParam("name") String userName,
                               @PathParam("newpassword") String password,
                               @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(userName) || isVoid(password)) return Response.status(Status.BAD_REQUEST).build();
        User user;
        try {
            user = facade.getUser(userName, authHeader);
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        if (user == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        user.setPassword(password.toCharArray());
        try {
            user = facade.changePassword(user, user.getRevision(), authHeader);
        }
        catch (RuntimeException ex) {
           return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        if (user == null) {
            return Response.status(Status.CONFLICT).entity("Newer Revision exists!").build();
        }
        return Response.ok().entity(user).build();
    }

    @DELETE
    @Path("{name}")
    public Response deleteUser(@PathParam("name") String userName,
                           @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(userName)) return Response.status(Status.NOT_FOUND).build();
        User user;
        try {
            user = facade.getUser(userName, authHeader);
        }
        catch (UnauthorizedException ex) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        if (user == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        boolean success;
        try {
            success = facade.deleteUser(userName, user.getRevision(), authHeader);
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        if (!success) {
            return Response.status(Status.CONFLICT).entity("Newer Revision exists!").build();
        }
        return Response.noContent().build();
    }

    private boolean isVoid(String value) {
        if (value == null) {
            return true;
        }
        else if (value.length() == 0) {
            return true;
        }
        else {
            return value.equals("null");
        }
    }

    private boolean hasIllegalCharacters(String value) {
        if (isVoid(value)) {
            return false;
        }
        String[] array = value.split("[<>\"&']", 2);
        return array.length > 1;
    }

    private boolean validateUuid(String uuidStr) {
        UUID uuid;
        try {
            uuid = isVoid(uuidStr) ? null : UUID.fromString(uuidStr);
        }
        catch (IllegalArgumentException iae) {
            uuid = null;
        }
        if (uuid == null) return false;
        else return true;
    }

    private EmailAddress validateEmail(String emailStr) {
        try {
            return isVoid(emailStr) ? null : new EmailAddress(emailStr);
        }
        catch (AddressException ae) {
            return null;
        }
    }

    private Response executeRequest(Consumer userAccountCreationRequestConsumer) {
        try {
            userAccountCreationRequestConsumer.accept(facade);
        }
        catch (BusinessException be) {
            Response.Status status = Status.INTERNAL_SERVER_ERROR; // default
            if (be.getType() instanceof UsersType) {
                UsersType type = (UsersType) be.getType();
                if (type == USER_ALREADY_EXISTS || type == EMAIL_ALREADY_EXISTS) {
                    status = Status.CONFLICT;
                }
            }
            return Response.status(status).entity(be.getMessage()).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
        return Response.accepted().build();
    }

    private Response executeConfirmation(Function<UsersFacade, Response> function, String expiryMsg, String conflictMsg) {
        try {
            return function.apply(facade);
        }
        catch (BusinessException e) {
            Status status = ((e.getType() instanceof UsersType && (UsersType) e.getType() == UsersType.UUID_NOT_FOUND) ? Status.NOT_FOUND : Status.INTERNAL_SERVER_ERROR);
            return Response.status(status).entity(expiryMsg).build();
        }
        catch (RuntimeException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

}
