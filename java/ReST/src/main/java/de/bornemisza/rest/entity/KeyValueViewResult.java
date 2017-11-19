package de.bornemisza.rest.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyValueViewResult {

    public KeyValueViewResult() {
        // JAXB needs this
    }

    @JsonProperty(value = "total_rows")
    private int totalRows;

    @JsonProperty(value = "offset")
    private int offset;

    @JsonProperty(value = "rows")
    private List<Row> rows;

    public int getTotalRows() {
        return totalRows;
    }

    public int getOffset() {
        return offset;
    }

    public List<Row> getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return "KeyValueViewResult{" + "totalRows=" + totalRows + ", offset=" + offset + ", rows=" + rows + '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Row {

        public Row() {
        }

        @JsonProperty("id")
        private String id;

        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;

        public String getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Row{" + "id=" + id + ", key=" + key + ", value=" + value + '}';
        }

    }

}
