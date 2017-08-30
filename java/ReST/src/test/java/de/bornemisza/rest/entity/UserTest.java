package de.bornemisza.rest.entity;

import org.junit.Test;
import static org.junit.Assert.*;

public class UserTest {
    
    public UserTest() {
    }
    
    @Test
    public void setNameAndId() {
        String name = "King Kong";
        User CUT = new User();
        CUT.setName(name);
        assertEquals(name, CUT.getName());
        assertEquals(User.USERNAME_PREFIX + name, CUT.getId());
    }

    @Test(expected=IllegalStateException.class)
    public void changeIdNotAllowed() {
        User CUT = new User();
        CUT.setName("King Kong");
        CUT.setId("someId");
    }

    @Test(expected=IllegalStateException.class)
    public void changeNameNotAllowed() {
        User CUT = new User();
        CUT.setName("King Kong");
        CUT.setName("something else");
    }

}
