package de.bornemisza.rest.entity.result;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyValueViewResult extends RestResult implements Serializable {

    private static final long serialVersionUID = 1L;

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
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.totalRows;
        hash = 17 * hash + this.offset;
        hash = 17 * hash + Objects.hashCode(this.rows);
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
        final KeyValueViewResult other = (KeyValueViewResult) obj;
        if (this.totalRows != other.totalRows) {
            return false;
        }
        if (this.offset != other.offset) {
            return false;
        }
        return Objects.equals(this.rows, other.rows);
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
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.id);
            hash = 71 * hash + Objects.hashCode(this.key);
            hash = 71 * hash + Objects.hashCode(this.value);
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
            final Row other = (Row) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Row{" + "id=" + id + ", key=" + key + ", value=" + value + '}';
        }

    }

}
