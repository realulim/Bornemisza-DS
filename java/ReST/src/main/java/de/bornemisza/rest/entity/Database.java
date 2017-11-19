package de.bornemisza.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    public String toString() {
        return "Database{" + "docCount=" + docCount + ", diskSize=" + diskSize + ", dbName=" + dbName + '}';
    }

}
