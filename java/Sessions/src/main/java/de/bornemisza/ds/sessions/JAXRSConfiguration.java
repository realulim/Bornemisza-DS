package de.bornemisza.ds.sessions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import de.bornemisza.ds.sessions.endpoint.Sessions;
import de.bornemisza.ds.sessions.endpoint.Uuids;

/**
 * Configures a JAX-RS endpoint.
 */
@ApplicationPath("/")
public class JAXRSConfiguration extends Application {

    public JAXRSConfiguration() { }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(Sessions.class);
        classes.add(Uuids.class);

        classes.add(JacksonFeature.class);

        return classes;
    }

    public static final List<String> COLORS = Arrays.asList(new String[] { "LightSeaGreen", "Crimson", "Gold", "RoyalBlue", "LightSalmon"});
    public static final String DEFAULT_COLOR = "Black";
    public static String MY_COLOR = DEFAULT_COLOR;
    public static String QUEUE_UUID_STORE = "UuidWriteQueue";

}
