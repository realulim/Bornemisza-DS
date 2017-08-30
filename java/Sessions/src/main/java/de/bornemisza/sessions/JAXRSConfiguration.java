package de.bornemisza.sessions;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import de.bornemisza.sessions.endpoint.Sessions;

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

        classes.add(JacksonFeature.class);

        return classes;
    }

}
