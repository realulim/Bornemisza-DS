package de.bornemisza.rest.entity;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;
    private String revision;
    private List<String> conflicts;

    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
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

    @JsonProperty("_rev")
    public String getRevision() {
        return revision;
    }

    @JsonProperty("_rev")
    public void setRevision(String s) {
        // no empty strings thanks
        if (s != null && s.length() == 0) {
            return;
        }
        this.revision = s;
    }

    @JsonIgnore
    public boolean isNew() {
        return revision == null;
    }

    @JsonProperty("_conflicts")
    void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }

    @JsonIgnore
    public List<String> getConflicts() {
        return conflicts;
    }

    public boolean hasConflict() {
        return conflicts != null && !conflicts.isEmpty();
    }

}
