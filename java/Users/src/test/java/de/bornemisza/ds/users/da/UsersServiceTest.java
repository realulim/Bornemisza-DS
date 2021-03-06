package de.bornemisza.ds.users.da;


import de.bornemisza.ds.users.da.UsersService;
import de.bornemisza.ds.users.da.CouchUsersPoolAsAdmin;
import de.bornemisza.ds.users.da.CouchPool;
import de.bornemisza.ds.users.da.CouchUsersPool;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.MediaType;

import org.javalite.http.Delete;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.HttpException;
import org.javalite.http.Put;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import de.bornemisza.rest.HttpConnection;
import de.bornemisza.rest.HttpHeaders;
import de.bornemisza.rest.entity.EmailAddress;
import de.bornemisza.rest.entity.User;
import de.bornemisza.rest.exception.BusinessException;
import de.bornemisza.rest.exception.DocumentNotFoundException;
import de.bornemisza.rest.exception.TechnicalException;
import de.bornemisza.rest.exception.UnauthorizedException;
import de.bornemisza.rest.exception.UpdateConflictException;
import de.bornemisza.rest.security.Auth;
import de.bornemisza.rest.security.BasicAuthCredentials;
import de.bornemisza.rest.security.DoubleSubmitToken;
import de.bornemisza.ds.users.boundary.UsersType;

public class UsersServiceTest {

    private UsersService CUT;
    private User user;
    private final String userAsJson = "{\"_id\":\"org.couchdb.user:Fazil Ongudar\",\"_rev\":\"34-80e1f26cf8e78a926be87928eb08ed72\",\"type\":\"user\",\"name\":\"Fazil Ongudar\",\"email\":\"fazil.changed@restmail.net\",\"roles\":[\"customer\",\"user\"],\"password_scheme\":\"pbkdf2\",\"iterations\":10,\"derived_key\":\"cae17cd81a9a0cbf5605ff8770847b3507a5cde8\",\"salt\":\"5168306a0c38fa04decb1362bb9d98e5\"}";
    private final Auth basicAuth = new Auth(new BasicAuthCredentials("flori", "floripassword"));
    private final Auth cookieAuth = new Auth(new DoubleSubmitToken("foo", "bar"));
    private Http http;
    private Get get;
    private Put put;
    private Delete delete;
    private ArgumentCaptor<String> revCaptor;

    public UsersServiceTest() {
    }

    @Before
    public void setUp() throws AddressException {
        CouchUsersPoolAsAdmin adminPool = mock(CouchUsersPoolAsAdmin.class);
        CouchUsersPool usersPool = mock(CouchUsersPool.class);
        CouchPool couchPool = mock(CouchPool.class);

        user = new User();
        user.setName("Fazil Ongudar");
        user.setEmail(new EmailAddress("fazil.changed@restmail.net"));

        http = mock(Http.class);
        when(http.getBaseUrl()).thenReturn("https://host.domain.com/myurl");
        when(http.getHostName()).thenReturn("host.domain.com");

        get = mock(Get.class);
        when(get.header(anyString(), anyString())).thenReturn(get);
        when(get.basic(anyString(), anyString())).thenReturn(get);
        when(http.get(anyString())).thenReturn(get);

        put = mock(Put.class);
        when(put.header(anyString(), anyString())).thenReturn(put);
        when(put.basic(anyString(), anyString())).thenReturn(put);
        when(http.put(anyString(), anyString())).thenReturn(put);

        delete = mock(Delete.class);
        revCaptor = ArgumentCaptor.forClass(String.class);
        when(delete.header(eq(HttpHeaders.IF_MATCH), revCaptor.capture())).thenReturn(delete);
        when(delete.basic(anyString(), anyString())).thenReturn(delete);
        when(http.delete(anyString())).thenReturn(delete);

        when(couchPool.getConnection()).thenReturn(new HttpConnection("myDb", http));
        when(usersPool.getConnection()).thenReturn(new HttpConnection("myDb", http));
        when(adminPool.getConnection()).thenReturn(new HttpConnection("myDb", http));
        when(adminPool.getUserName()).thenReturn("admin");
        when(adminPool.getPassword()).thenReturn("admin-pw".toCharArray());
        CUT = new UsersService(adminPool, usersPool, couchPool, 50);
    }

    @Test
    public void init_technicalException() {
        String errMsg = "Connection refused";
        when(get.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.init();
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void init_businessException() {
        when(get.responseCode()).thenReturn(500);
        try {
            CUT.init();
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void init() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn("{\"db_name\":\"_users\",\"update_seq\":\"27-g1AAAAFTeJzLYWBg4MhgTmEQTM4vTc5ISXIwNNAz0TPQszDLAUoxJTIkyf___z8rkQGPoiQFIJlkT1idA0hdPFgdIz51CSB19QTNy2MBkgwNQAqodH5WoiRBtQsgavcTY-4BiNr7-N0KUfsAohbk3iwAlrhe6Q\",\"sizes\":{\"file\":144661,\"external\":5675,\"active\":10411},\"purge_seq\":0,\"other\":{\"data_size\":5675},\"doc_del_count\":1,\"doc_count\":3,\"disk_size\":144661,\"disk_format_version\":6,\"data_size\":10411,\"compact_running\":false,\"instance_start_time\":\"0\"}");
        CUT.init();
        verify(get).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }

    @Test
    public void existsUser_technicalException() {
        String errMsg = "Connection refused";
        when(get.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.existsUser("Harry");
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void existsUser_businessException() {
        when(get.responseCode()).thenReturn(500);
        try {
            CUT.existsUser("Harry");
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void existsUser_no() {
        when(get.responseCode()).thenReturn(404);
        assertFalse(CUT.existsUser("Harry"));
    }

    @Test
    public void existsUser_yes() {
        when(get.responseCode()).thenReturn(200);
        assertTrue(CUT.existsUser("Harry"));
    }

    @Test
    public void existsEmail_technicalException() {
        String errMsg = "Connection refused";
        when(get.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.existsEmail(user.getEmail());
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void existsEmail_businessException() {
        when(get.responseCode()).thenReturn(500);
        try {
            CUT.existsEmail(user.getEmail());
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void existsEmail() throws AddressException {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn("{\"total_rows\":1,\"offset\":0,\"rows\":[\n" +
"{\"id\":\"org.couchdb.user:Harry Potter\",\"key\":\"harry@potter.com\",\"value\":\"org.couchdb.user:Harry Potter\"}\n" +
"]}");
        assertTrue(CUT.existsEmail(new InternetAddress("harry@potter.com")));
        assertFalse(CUT.existsEmail(new InternetAddress("polga@grumvory.sk")));
    }

    @Test
    public void createUser_technicalException() {
        String errMsg = "Connection refused";
        when(put.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.createUser(user);
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void createUser_businessException(){
        when(put.responseCode()).thenReturn(500);
        try {
            CUT.createUser(user);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void createUser_updateConflict() {
        when(put.responseCode()).thenReturn(409);
        String msg = "Newer Revision exists";
        when(put.responseMessage()).thenReturn(msg);
        try {
            CUT.createUser(user);
            fail();
        }
        catch (UpdateConflictException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void createUser_userDatabaseDoesNotBecomeAvailable() {
        when(put.responseCode()).thenReturn(201);
        when(get.responseCode()).thenReturn(200).thenReturn(404);
        when(get.text()).thenReturn(userAsJson);
        try {
            CUT.createUser(user);
        }
        catch (TechnicalException ex) {
            assertTrue(ex.toString().contains("was not created"));
        }
    }

    @Test
    public void createUser() {
        when(put.responseCode()).thenReturn(201);
        when(get.responseCode()).thenReturn(200).thenReturn(404).thenReturn(404).thenThrow(new HttpException("meh")).thenReturn(404).thenReturn(200);
        when(get.text()).thenReturn(userAsJson);

        User createdUser = CUT.createUser(user);
        assertNull(createdUser.getPassword());
        assertEquals(user.getName(), createdUser.getName());
        assertEquals(user.getEmail(), createdUser.getEmail());
    }

    @Test
    public void updateUser_technicalException() {
        String errMsg = "Connection refused";
        when(put.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.updateUser(basicAuth, user);
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void updateUser_businessException(){
        when(put.responseCode()).thenReturn(500);
        try {
            CUT.updateUser(basicAuth, user);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void updateUser_unauthorized() {
        when(put.responseCode()).thenReturn(401);
        String msg = "User unknown";
        when(put.responseMessage()).thenReturn(msg);
        try {
            CUT.updateUser(basicAuth, user);
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void updateUser_updateConflict() {
        when(put.responseCode()).thenReturn(409);
        String msg = "Newer Revision exists";
        when(put.responseMessage()).thenReturn(msg);
        try {
            CUT.updateUser(basicAuth, user);
            fail();
        }
        catch (UpdateConflictException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void updateUser() {
        when(put.responseCode()).thenReturn(201);
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(userAsJson);
        User updatedUser = CUT.updateUser(basicAuth, user);
        assertNull(updatedUser.getPassword());
        assertEquals(user.getName(), updatedUser.getName());
        assertEquals(user.getEmail(), updatedUser.getEmail());
    }

    @Test
    public void getUser_technicalException() {
        String errMsg = "Connection refused";
        when(get.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.getUser(basicAuth, user.getName());
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void getUser_businessException(){
        when(get.responseCode()).thenReturn(500);
        try {
            CUT.getUser(basicAuth, user.getName());
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void getUser_unauthorized() {
        when(get.responseCode()).thenReturn(401);
        String msg = "User unknown";
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getUser(basicAuth, user.getName());
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void getUser_notFound() {
        when(get.responseCode()).thenReturn(404);
        String msg = "User not found";
        when(get.responseMessage()).thenReturn(msg);
        try {
            CUT.getUser(basicAuth, user.getName());
            fail();
        }
        catch (DocumentNotFoundException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void getUser_usernamePasswordScheme() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(userAsJson);
        User readUser = CUT.getUser(basicAuth, user.getName());
        assertNull(readUser.getPassword());
        assertEquals(user.getName(), readUser.getName());
        assertEquals(user.getEmail(), readUser.getEmail());
        verify(get).basic(basicAuth.getUsername(), basicAuth.getPassword());
    }

    @Test
    public void getUser_cookieCsrfTokenScheme() {
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(userAsJson);
        User readUser = CUT.getUser(cookieAuth, user.getName());
        assertNull(readUser.getPassword());
        assertEquals(user.getName(), readUser.getName());
        assertEquals(user.getEmail(), readUser.getEmail());
        verify(get).header(HttpHeaders.COOKIE, cookieAuth.getCookie());
    }

    @Test
    public void changePassword_technicalException() {
        String errMsg = "Connection refused";
        when(put.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.changePassword(basicAuth, user);
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void changePassword_businessException(){
        when(put.responseCode()).thenReturn(500);
        try {
            CUT.changePassword(basicAuth, user);
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void changePassword_unauthorized() {
        when(put.responseCode()).thenReturn(401);
        String msg = "User unknown";
        when(put.responseMessage()).thenReturn(msg);
        try {
            CUT.changePassword(basicAuth, user);
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void changePassword_updateConflict() {
        when(put.responseCode()).thenReturn(409);
        String msg = "Newer Revision exists";
        when(put.responseMessage()).thenReturn(msg);
        try {
            CUT.changePassword(basicAuth, user);
            fail();
        }
        catch (UpdateConflictException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void changePassword() {
        when(put.responseCode()).thenReturn(201);
        when(get.responseCode()).thenReturn(200);
        when(get.text()).thenReturn(userAsJson);

        String oldPassword = basicAuth.getPassword();
        String newPassword = "newpassword";
        user.setPassword(newPassword.toCharArray());
        Auth myAuth = mock(Auth.class);
        when(myAuth.getUsername()).thenReturn(basicAuth.getUsername());
        when(myAuth.getPassword()).thenReturn(oldPassword).thenReturn(newPassword);
        ArgumentCaptor<String> userJsonCaptor = ArgumentCaptor.forClass(String.class);

        User updatedUser = CUT.changePassword(myAuth, user);
        assertNull(updatedUser.getPassword());
        verify(http).put(anyString(), userJsonCaptor.capture());
        assertTrue(userJsonCaptor.getValue().contains(String.valueOf(newPassword)));

        verify(myAuth).changePassword(newPassword);
        verify(get).basic(basicAuth.getUsername(), newPassword);
    }

    @Test
    public void deleteUser_technicalException() {
        String errMsg = "Connection refused";
        when(delete.responseCode()).thenThrow(new HttpException(errMsg));
        try {
            CUT.deleteUser(basicAuth, user.getName(), "rev123");
            fail();
        }
        catch (TechnicalException e) {
            assertTrue(e.getMessage().contains(errMsg));
        }
    }

    @Test
    public void deleteUser_businessException(){
        when(delete.responseCode()).thenReturn(201);
        try {
            CUT.deleteUser(basicAuth, user.getName(), "rev123");
            fail();
        }
        catch (BusinessException e) {
            assertEquals(UsersType.UNEXPECTED, e.getType());
        }
    }

    @Test
    public void deleteUser_unauthorized() {
        when(delete.responseCode()).thenReturn(401);
        String msg = "User unknown";
        when(delete.responseMessage()).thenReturn(msg);
        try {
            CUT.deleteUser(basicAuth, user.getName(), "rev123");
            fail();
        }
        catch (UnauthorizedException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void deleteUser_updateConflict() {
        when(delete.responseCode()).thenReturn(409);
        String msg = "Newer Revision exists";
        when(delete.responseMessage()).thenReturn(msg);
        try {
            CUT.deleteUser(basicAuth, user.getName(), "rev123");
            fail();
        }
        catch (UpdateConflictException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void deleteUser() {
        String rev = "rev123";
        when(delete.responseCode()).thenReturn(200);
        CUT.deleteUser(basicAuth, user.getName(), rev);
        assertEquals(rev, revCaptor.getValue());
    }

}
