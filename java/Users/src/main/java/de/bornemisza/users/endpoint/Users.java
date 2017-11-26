package de.bornemisza.users.endpoint;

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
import de.bornemisza.users.boundary.UsersFacade;
import de.bornemisza.users.boundary.UsersType;
import static de.bornemisza.users.boundary.UsersType.EMAIL_ALREADY_EXISTS;
import static de.bornemisza.users.boundary.UsersType.USER_ALREADY_EXISTS;

@Stateless
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
        if (isVoid(userName) || hasIllegalCharacters(userName)) {
            throw new RestException(Status.BAD_REQUEST);
        }
        User user;
        try {
            user = facade.getUser(userName, authHeader);
        }
        catch (UnauthorizedException ex) {
            throw new RestException(Status.UNAUTHORIZED);
        }
        catch (RuntimeException ex) {
            throw new RestException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (user == null) throw new RestException(Status.NOT_FOUND);
        else return user;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userAccountCreationRequest(User user) {
        if (user == null || user.getEmail() == null) {
            throw new RestException(
                    Response.status(Status.BAD_REQUEST).entity("No User or E-Mail missing!").build());
        }
        else if (hasIllegalCharacters(user.getName())) {
            throw new RestException(
                    Response.status(Status.BAD_REQUEST).entity("Illegal Characters in User Name!").build());
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
    @Path("{name}/email")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response changeEmailRequest(@PathParam("name") String userName,
            String emailStr,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(userName)) {
            throw new RestException(Status.BAD_REQUEST);
        }
        EmailAddress email = validateEmail(emailStr);
        User user = getUser(userName, authHeader);
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
    public User confirmEmail(@PathParam("uuid") String uuidStr,
                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        validateUuid(uuidStr);
        Function confirmEmailFunction = new Function<UsersFacade, User>() {
            @Override
            public User apply(UsersFacade facade) {
                return facade.confirmEmail(uuidStr, authHeader);
            }
        };
        String expiryMsg = "E-Mail Change Request does not exist - maybe expired?";
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
            throw new RestException(Status.BAD_REQUEST);
        }
        User user = getUser(userName, authHeader);
        user.setPassword(password.toCharArray());
        try {
            user = facade.changePassword(user, user.getRevision(), authHeader);
        }
        catch (RuntimeException ex) {
            throw new RestException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (user == null) {
            throw new RestException(
                Response.status(Status.CONFLICT).entity("Newer Revision exists!").build());
        }
        return user;
    }
    
    @DELETE
    @Path("{name}")
    public void deleteUser(@PathParam("name") String userName, 
                           @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (isVoid(userName)) {
            throw new RestException(Status.NOT_FOUND);
        }
        User user = getUser(userName, authHeader);
        boolean success;
        try {
            success = facade.deleteUser(userName, user.getRevision(), authHeader);
        }
        catch (RuntimeException ex) {
            throw new RestException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (! success) {
            throw new RestException(
                    Response.status(Status.CONFLICT).entity("Newer Revision exists!").build());
        }
    }

    private boolean isVoid(String value) {
        if (value == null) return true;
        else if (value.length() == 0) return true;
        else return value.equals("null");
    }

    private boolean hasIllegalCharacters(String value) {
        if (isVoid(value)) return false;
        String[] array = value.split("[<>\"&']", 2);
        return array.length > 1;
    }

    private void validateUuid(String uuidStr) throws RestException {
        UUID uuid;
        try {
            uuid = isVoid(uuidStr) ? null : UUID.fromString(uuidStr);
        }
        catch (IllegalArgumentException iae) {
            uuid = null;
        }
        if (uuid == null) {
            throw new RestException(
                    Response.status(Status.BAD_REQUEST).entity("UUID missing or unparseable!").build());
        }
    }

    private EmailAddress validateEmail(String emailStr) throws RestException {
        EmailAddress newEmail;
        try {
            newEmail = isVoid(emailStr) ? null : new EmailAddress(emailStr);
        }
        catch (AddressException ae) {
            newEmail = null;
        }
        if (newEmail == null) {
            throw new RestException(
                    Response.status(Status.BAD_REQUEST).entity("E-Mail missing or unparseable!").build());
        }
        return newEmail;
    }

    private Response executeRequest(Consumer userAccountCreationRequestConsumer) {
        try {
            userAccountCreationRequestConsumer.accept(facade);
        }
        catch (BusinessException be) {
            Response.Status status = Status.INTERNAL_SERVER_ERROR; // default
            if (be.getType() instanceof UsersType) {
                UsersType type = (UsersType)be.getType();
                if (type == USER_ALREADY_EXISTS || type == EMAIL_ALREADY_EXISTS) {
                    status = Status.CONFLICT;
                }
            }
            throw new RestException(
                    Response.status(status).entity(be.getMessage()).build());
        }
        catch (RuntimeException ex) {
            throw new RestException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        return Response.accepted().build();
    }

    private User executeConfirmation(Function<UsersFacade, User> function, String expiryMsg, String conflictMsg) {
        User confirmedUser = null;
        try {
            confirmedUser = function.apply(facade);
        }
        catch (BusinessException e) {
            Status status = ((e.getType() instanceof UsersType && (UsersType)e.getType() == UsersType.UUID_NOT_FOUND) ? Status.NOT_FOUND : Status.INTERNAL_SERVER_ERROR);
            throw new RestException(
                    Response.status(status).entity(expiryMsg).build());
        }
        catch (RuntimeException ex) {
            throw new RestException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        }
        if (confirmedUser == null) {
            throw new RestException(
                    Response.status(Status.CONFLICT).entity(conflictMsg).build());
        }
        return confirmedUser;
    }

}
