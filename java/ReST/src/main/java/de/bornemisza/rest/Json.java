package de.bornemisza.rest;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.javalite.http.HttpException;

public class Json {
    
    public static String toJson(Object obj) throws HttpException {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        }
        catch (JsonProcessingException ex) {
            throw new HttpException("Problem marshalling JSON!", ex);
        }
    }

    public static <T extends Object> T fromJson(String json, Class<T> type) throws HttpException {
        try {
            return new ObjectMapper().readValue(json, type);
        }
        catch (IOException ex) {
            throw new HttpException("Problem unmarshalling JSON: " + json, ex);
        }
    }

}
