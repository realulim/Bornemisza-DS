package de.bornemisza.ds.rest.entity;

import de.bornemisza.ds.rest.entity.result.RestResult;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Document extends RestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty(value = "_id")
    private String id;

    @JsonProperty(value = "_rev")
    private String revision;

    @JsonProperty(value = "_conflicts")
    private List<String> conflicts;

    @JsonProperty(value = "type")
    protected String type;

    public String getId() {
        return id;
    }

    public Document() {
        // JAXB needs this
        this.type = "document";
    }

    public void setId(String s) {
        if (s == null || s.equals("")) throw new IllegalArgumentException("id must have a value");
        if (id != null && id.equals(s)) {
            return;
        }
        if (id != null) {
            throw new IllegalStateException("cannot set id, id already set");
        }
        id = s;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @JsonIgnore
    public boolean isNew() {
        return revision == null;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }

    @JsonIgnore
    public boolean hasConflict() {
        return conflicts != null && !conflicts.isEmpty();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.id);
        hash = 79 * hash + Objects.hashCode(this.revision);
        hash = 79 * hash + Objects.hashCode(this.conflicts);
        hash = 79 * hash + Objects.hashCode(this.type);
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
        final Document other = (Document) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.revision, other.revision)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return Objects.equals(this.conflicts, other.conflicts);
    }

    @Override
    public String toString() {
        return super.toString() + " => " + "Document{" + "id=" + id + ", revision=" + revision + ", conflicts=" + conflicts + ", type=" + type + '}';
    }

}
