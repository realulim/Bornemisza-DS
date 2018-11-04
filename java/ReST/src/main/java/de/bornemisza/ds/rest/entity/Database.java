package de.bornemisza.ds.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Database {

    public Database() {
        // JAXB needs this
    }

    @JsonProperty(value = "doc_count")
    private int docCount;
    
    @JsonProperty(value = "disk_size")
    private int diskSize;
    
    @JsonProperty(value = "db_name")
    private String dbName;

    public int getDocCount() {
        return docCount;
    }

    public int getDiskSize() {
        return diskSize;
    }

    public String getDbName() {
        return dbName;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + this.docCount;
        hash = 59 * hash + this.diskSize;
        hash = 59 * hash + Objects.hashCode(this.dbName);
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
        final Database other = (Database) obj;
        if (this.docCount != other.docCount) {
            return false;
        }
        if (this.diskSize != other.diskSize) {
            return false;
        }
        return Objects.equals(this.dbName, other.dbName);
    }

    @Override
    public String toString() {
        return "Database{" + "docCount=" + docCount + ", diskSize=" + diskSize + ", dbName=" + dbName + '}';
    }

}
