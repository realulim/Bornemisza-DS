/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package de.bornemisza.rest;

/**
 * Constants enumerating the HTTP headers. All headers defined in RFC1945 (HTTP/1.0), RFC2616 (HTTP/1.1), and RFC2518
 * (WebDAV) are listed.
 *
 * @since 4.1
 */
public final class HttpHeaders {

    private HttpHeaders() {
    }

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.3.2">Section 5.3.2 of RFC 7231</a>
     */
    public static final String ACCEPT = "Accept";
    
    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.3.3">Section 5.3.3 of RFC 7231</a>
     */
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    
    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.3.4">Section 5.3.4 of RFC 7231</a>
     */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    
    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.3.5">Section 5.3.5 of RFC 7231</a>
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    
    /**
     * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.3">Section 5.3.5 of RFC 7233</a>
     */
    public static final String ACCEPT_RANGES = "Accept-Ranges";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    /**
     * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
     */
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7234#section-5.1">Section 5.1 of RFC 7234</a>
     */
    public static final String AGE = "Age";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.4.1">Section 7.4.1 of RFC 7231</a>
     */
    public static final String ALLOW = "Allow";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7235#section-4.2">Section 4.2 of RFC 7235</a>
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7234#section-5.2">Section 5.2 of RFC 7234</a>
     */
    public static final String CACHE_CONTROL = "Cache-Control";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-6.1">Section 6.1 of RFC 7230</a>
     */
    public static final String CONNECTION = "Connection";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.2.2">Section 3.1.2.2 of RFC 7231</a>
     */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc6266">RFC 6266</a>
     */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.3.2">Section 3.1.3.2 of RFC 7231</a>
     */
    public static final String CONTENT_LANGUAGE = "Content-Language";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-3.3.2">Section 3.3.2 of RFC 7230</a>
     */
    public static final String CONTENT_LENGTH = "Content-Length";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.4.2">Section 3.1.4.2 of RFC 7231</a>
     */
    public static final String CONTENT_LOCATION = "Content-Location";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7233#section-4.2">Section 4.2 of RFC 7233</a>
     */
    public static final String CONTENT_RANGE = "Content-Range";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc2109#section-4.3.4">Section 4.3.4 of RFC 2109</a>
     */
    public static final String COOKIE = "Cookie";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
     */
    public static final String DATE = "Date";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
     */
    public static final String ETAG = "ETag";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.1.1">Section 5.1.1 of RFC 7231</a>
     */
    public static final String EXPECT = "Expect";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7234#section-5.3">Section 5.3 of RFC 7234</a>
     */
    public static final String EXPIRES = "Expires";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.5.1">Section 5.5.1 of RFC 7231</a>
     */
    public static final String FROM = "From";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-5.4">Section 5.4 of RFC 7230</a>
     */
    public static final String HOST = "Host";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7232#section-3.1">Section 3.1 of RFC 7232</a>
     */
    public static final String IF_MATCH = "If-Match";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7232#section-3.3">Section 3.3 of RFC 7232</a>
     */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7232#section-3.2">Section 3.2 of RFC 7232</a>
     */
    public static final String IF_NONE_MATCH = "If-None-Match";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7233#section-3.2">Section 3.2 of RFC 7233</a>
     */
    public static final String IF_RANGE = "If-Range";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7232#section-3.4">Section 3.4 of RFC 7232</a>
     */
    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7232#section-2.2">Section 2.2 of RFC 7232</a>
     */
    public static final String LAST_MODIFIED = "Last-Modified";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
     */
    public static final String LINK = "Link";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.2">Section 7.1.2 of RFC 7231</a>
     */
    public static final String LOCATION = "Location";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.1.2">Section 5.1.2 of RFC 7231</a>
     */
    public static final String MAX_FORWARDS = "Max-Forwards";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc6454">RFC 6454</a>
     */
    public static final String ORIGIN = "Origin";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7234#section-5.4">Section 5.4 of RFC 7234</a>
     */
    public static final String PRAGMA = "Pragma";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7235#section-4.3">Section 4.3 of RFC 7235</a>
     */
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7235#section-4.4">Section 4.4 of RFC 7235</a>
     */
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7233#section-3.1">Section 3.1 of RFC 7233</a>
     */
    public static final String RANGE = "Range";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.5.2">Section 5.5.2 of RFC 7231</a>
     */
    public static final String REFERER = "Referer";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.3">Section 7.1.3 of RFC 7231</a>
     */
    public static final String RETRY_AFTER = "Retry-After";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
     */
    public static final String SERVER = "Server";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc2109#section-4.2.2">Section 4.2.2 of RFC 2109</a>
     */
    public static final String SET_COOKIE = "Set-Cookie";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc2965">RFC 2965</a>
     */
    public static final String SET_COOKIE2 = "Set-Cookie2";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-4.3">Section 4.3 of RFC 7230</a>
     */
    public static final String TE = "TE";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-4.4">Section 4.4 of RFC 7230</a>
     */
    public static final String TRAILER = "Trailer";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-3.3.1">Section 3.3.1 of RFC 7230</a>
     */
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-6.7">Section 6.7 of RFC 7230</a>
     */
    public static final String UPGRADE = "Upgrade";
    
    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.5.3">Section 5.5.3 of RFC 7231</a>
     */
    public static final String USER_AGENT = "User-Agent";
    
    /**
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.4">Section 7.1.4 of RFC 7231</a>
     */
    public static final String VARY = "Vary";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7230#section-5.7.1">Section 5.7.1 of RFC 7230</a>
     */
    public static final String VIA = "Via";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7234#section-5.5">Section 5.5 of RFC 7234</a>
     */
    public static final String WARNING = "Warning";

    /**
     * @see <a href="http://tools.ietf.org/html/rfc7235#section-4.1">Section 4.1 of RFC 7235</a>
     */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    // Custom Headers
    public static final String CTOKEN = "C-Token";

}
