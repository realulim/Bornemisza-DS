package de.bornemisza.rest;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import static java.net.URLEncoder.encode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.javalite.http.Delete;
import org.javalite.http.Get;
import org.javalite.http.HttpException;
import org.javalite.http.Multipart;
import org.javalite.http.Patch;
import org.javalite.http.Post;
import org.javalite.http.Put;

/**
 * This class has been adapted from https://github.com/javalite/activejdbc/blob/master/javalite-common/src/main/java/org/javalite/http/Http.java
 * 
 * The original code is Copyright 2009-2016 Igor Polevoy and is licensed as http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Changes have been made to make all methods non-static and remove the private constructor to facilitate unit testing
 * and dependency injection. Additionally there is now the notion of a BaseURI, to which all other URIs are now relative to.
 */
public class Http {

    /**
     * Connection timeout in milliseconds. Set this value to what you like to
     * override default.
     */
    public static final int CONNECTION_TIMEOUT = 5000;

    /**
     * Read timeout in milliseconds. Set this value to what you like to override
     * default.
     */
    public static final int READ_TIMEOUT = 5000;

    private ObjectMapper jsonMapper = new ObjectMapper();

    private final String baseUrl;
    private final String hostname;

    /**
     * @param baseUrl not null
     */
    public Http(URL baseUrl) {
        String url = baseUrl.toString();
        if (! url.endsWith("/")) {
            url = url + "/";
        }
        this.baseUrl = url;
        this.hostname = baseUrl.getHost();
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getHostName() {
        return this.hostname;
    }

    /**
     * Executes a POST request.
     *
     * @param uri url of resource.
     * @param content content to be posted.
     * @return {@link Post} object.
     */
    public Post post(String uri, String content) {
        return post(uri, content.getBytes(), CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a POST request. Often used to post form parameters:
     *
     * <pre>
     *     Http.post("http://example.com/create").param("name1", "val1");
     * </pre>
     *
     * @param uri url of resource.
     * @return {@link Post} object.
     */
    public Post post(String uri) {
        return post(uri, null, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a POST request.
     *
     * @param uri url of resource.
     * @param content content to be posted.
     * @return {@link Post} object.
     */
    public Post post(String uri, byte[] content) {
        return post(uri, content, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a POST request.
     *
     * @param url url of resource.
     * @param content content to be posted.
     * @param connectTimeout connection timeout in milliseconds.
     * @param readTimeout read timeout in milliseconds.
     * @return {@link Post} object.
     */
    public Post post(String url, byte[] content, int connectTimeout, int readTimeout) {

        try {
            if (! url.startsWith("http")) url = baseUrl + url;
            return new Post(url, content, connectTimeout, readTimeout);
        } 
        catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }

    /**
     * Executes a POST request. Often used to post form parameters:
     *
     * <pre>
     *     Http.post("http://example.com/create").param("name1", "val1");
     * </pre>
     *
     * @param url url of resource.
     * @param connectTimeout connection timeout in milliseconds.
     * @param readTimeout read timeout in milliseconds.
     * @return {@link Post} object.
     */
    public Post post(String url, int connectTimeout, int readTimeout) {

        try {
            if (! url.startsWith("http")) url = baseUrl + url;
            return new Post(url, null, connectTimeout, readTimeout);
        } 
        catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }

    /**
     * Executes a GET request.
     *
     * @param url url of the resource.
     * @return {@link Get} object.
     */
    public Get get(String url) {
        return get(url, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a GET request
     *
     * @param url url of resource.
     * @param connectTimeout connection timeout in milliseconds.
     * @param readTimeout read timeout in milliseconds.
     * @return {@link Get} object.
     */
    public Get get(String url, int connectTimeout, int readTimeout) {

        try {
            if (! url.startsWith("http")) url = baseUrl + url;
            return new Get(url, connectTimeout, readTimeout);
        } 
        catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }

    /**
     * Executes a PUT request.
     *
     * @param uri url of resource.
     * @param content content to be put.
     * @return {@link Put} object.
     */
    public Put put(String uri, String content) {
        return put(uri, content.getBytes());
    }

    /**
     * Executes a PUT request.
     *
     * @param uri uri of resource.
     * @param content content to be put.
     * @return {@link Put} object.
     */
    public Put put(String uri, byte[] content) {
        return put(uri, content, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a PUT request.
     *
     * @param url url of resource.
     * @param content content to be "put"
     * @param connectTimeout connection timeout in milliseconds.
     * @param readTimeout read timeout in milliseconds.
     * @return {@link Put} object.
     */
    public Put put(String url, byte[] content, int connectTimeout, int readTimeout) {

        try {
            if (! url.startsWith("http")) url = baseUrl + url;
            return new Put(url, content, connectTimeout, readTimeout);
        } 
        catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }

    /**
     * Create multipart request
     *
     * @param url URL to send to
     * @return new Multipart request
     */
    public Multipart multipart(String url) {
        if (! url.startsWith("http")) url = baseUrl + url;
        return new Multipart(url, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Create multipart request
     *
     * @param url URL to send to
     * @param connectTimeout connect timeout
     * @param readTimeout read timeout
     * @return new Multipart request
     */
    public Multipart multipart(String url, int connectTimeout, int readTimeout) {
        if (! url.startsWith("http")) url = baseUrl + url;
        return new Multipart(url, connectTimeout, connectTimeout);
    }

    /**
     * Executes a DELETE request.
     *
     * @param url url of resource to delete
     * @return {@link Delete}
     */
    public Delete delete(String url) {
        if (! url.startsWith("http")) url = baseUrl + url;
        return delete(url, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a DELETE request.
     *
     * @param url url of resource to delete
     * @param connectTimeout connection timeout in milliseconds.
     * @param readTimeout read timeout in milliseconds.
     * @return {@link Delete}
     */
    public Delete delete(String url, int connectTimeout, int readTimeout) {
        try {
            if (! url.startsWith("http")) url = baseUrl + url;
            return new Delete(url, connectTimeout, readTimeout);
        } 
        catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }

    /**
     * Executes a PATCH request.
     *
     * @param uri url of resource.
     * @param content content to be posted.
     * @return {@link Patch} object.
     */
    public Patch patch(String uri, String content) {
        return patch(uri, content.getBytes(), CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a PATCH request.
     *
     * @param uri url of resource.
     * @param content content to be posted.
     * @return {@link Patch} object.
     */
    public Patch patch(String uri, byte[] content) {
        return patch(uri, content, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Executes a PATCH request.
     *
     * @param url url of resource.
     * @param content content to be posted.
     * @param connectTimeout connection timeout in milliseconds.
     * @param readTimeout read timeout in milliseconds.
     * @return {@link Patch} object.
     */
    public Patch patch(String url, byte[] content, int connectTimeout, int readTimeout) {

        try {
            if (! url.startsWith("http")) url = baseUrl + url;
            return new Patch(url, content, connectTimeout, readTimeout);
        } 
        catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }

    /**
     * Converts a map to URL- encoded content. This is a convenience method
     * which can be used in combination with
     * {@link #post(String, byte[])}, {@link #put(String, String)} and others.
     * It makes it easy to convert parameters to submit a string:
     *
     * <pre>
     *     key=value&key1=value1;
     * </pre>
     *
     *
     *
     * @param params map with keys and values to be posted. This map is used to
     * build content to be posted, such that keys are names of parameters, and
     * values are values of those posted parameters. This method will also
     * URL-encode keys and content using UTF-8 encoding.
     * <p>
     * String representations of both keys and values are used.
     * </p>
     * @return {@link Post} object.
     */
    public String map2Content(Map params) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            Set keySet = params.keySet();
            Object[] keys = keySet.toArray();

            for (int i = 0; i < keys.length; i++) {
                stringBuilder.append(encode(keys[i].toString(), "UTF-8")).append("=").append(encode(params.get(keys[i]).toString(), "UTF-8"));
                if (i < (keys.length - 1)) {
                    stringBuilder.append("&");
                }
            }
        } 
        catch (UnsupportedEncodingException | RuntimeException e) {
            throw new HttpException("failed to generate content from map", e);
        }
        return stringBuilder.toString();
    }

    public String urlEncode(String toEncode) {
        try {
            return encode(toEncode, "utf-8");
        }
        catch (UnsupportedEncodingException ex) {
            throw new HttpException("failed to urlencode", ex);
        }
    }

    public String toJson(Object obj) throws HttpException {
        String json;
        try {
            json = jsonMapper.writeValueAsString(obj);
        }
        catch (JsonProcessingException ex) {
            throw new HttpException("Problem marshalling JSON!", ex);
        }
        return json;
    }

}
