package de.bornemisza.ds.rest.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Uuid extends Document implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public Uuid() {
        // JAXB needs this
        super();
        this.type = "uuid";
    }

    @JsonProperty(value = "values")
    private List<String> values;

    @JsonProperty(value = "appcolor")
    private String appColor;

    @JsonProperty(value = "dbcolor")
    private String dbColor;

    @JsonProperty(value = "date")
    private LocalDate date;

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String getAppColor() {
        return appColor;
    }

    public void setAppColor(String appColor) {
        this.appColor = appColor;
    }

    public String getDbColor() {
        return dbColor;
    }

    public void setDbColor(String color) {
        this.dbColor = color;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.values);
        hash = 29 * hash + Objects.hashCode(this.appColor);
        hash = 29 * hash + Objects.hashCode(this.dbColor);
        hash = 29 * hash + Objects.hashCode(this.date);
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
        final Uuid other = (Uuid) obj;
        if (!Objects.equals(this.appColor, other.appColor)) {
            return false;
        }
        if (!Objects.equals(this.dbColor, other.dbColor)) {
            return false;
        }
        if (!Objects.equals(this.values, other.values)) {
            return false;
        }
        return Objects.equals(this.date, other.date);
    }

    @Override
    public String toString() {
        return "Uuid{" + "values=" + values + ", appColor=" + appColor + ", dbColor=" + dbColor + ", date=" + date + '}';
    }

}
