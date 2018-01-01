package de.bornemisza.sessions.consumer;

import java.io.Serializable;
import java.util.Objects;

import de.bornemisza.rest.entity.Uuid;
import de.bornemisza.rest.security.Auth;

public class StoreUuidRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Auth auth;
    private final String databaseName;
    private final Uuid uuidDocument;

    public StoreUuidRequest(Auth auth, String databaseName, Uuid uuidDocument) {
        this.auth = auth;
        this.databaseName = databaseName;
        this.uuidDocument = uuidDocument;
    }

    public Auth getAuth() {
        return auth;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public Uuid getUuidDocument() {
        return uuidDocument;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.auth);
        hash = 67 * hash + Objects.hashCode(this.databaseName);
        hash = 67 * hash + Objects.hashCode(this.uuidDocument);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StoreUuidRequest other = (StoreUuidRequest) obj;
        if (!Objects.equals(this.databaseName, other.databaseName)) {
            return false;
        }
        if (!Objects.equals(this.auth, other.auth)) {
            return false;
        }
        return Objects.equals(this.uuidDocument, other.uuidDocument);
    }

    @Override
    public String toString() {
        return "SaveUuidRequest{" + "auth=" + auth + ", databaseName=" + databaseName + ", uuidDocument=" + uuidDocument + '}';
    }

}
