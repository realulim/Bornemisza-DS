package de.bornemisza.users;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import de.bornemisza.users.endpoint.Users;

/**
 * Configures a JAX-RS endpoint.
 */
@ApplicationPath("/")
public class JAXRSConfiguration extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(Users.class);

        classes.add(JacksonFeature.class);

        return classes;
    }

    public static String COUCHDB_HOSTQUEUE = "CouchDBHostQueue";
    public static String COUCHDB_UTILISATION = "CouchDBUtilisation";
    public static String TOPIC_NEW_USER_ACCOUNT = "New User Account";
    public static String TOPIC_SEND_CONFIRMATION_MAIL = "Send Confirmation Mail";

}
