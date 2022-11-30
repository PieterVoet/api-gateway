package com.predic8.membrane.core.openapi;

import com.predic8.membrane.core.openapi.util.*;
import org.junit.*;

import static com.predic8.membrane.core.openapi.util.Utils.*;
import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void getTypeNameFromSchemaRef() {
        assertEquals("Customer", Utils.getComponentLocalNameFromRef("#/components/schemas/Customer"));
    }

    @Test
    public void uuidInvalid() {
        assertFalse(isValidUUID(""));
        assertFalse(isValidUUID("9A991F7-0502-4E5E-83A2-F55B38E78192"));
        assertFalse(isValidUUID("9A991F71-0502-4E5E-83A2-F55B38E7819"));
    }

    @Test
    public void uuidValid() {
        assertTrue(isValidUUID("9A991F71-0502-4E5E-83A2-F55B38E78192"));
    }

    @Test
    public void emailInvalid() {
        assertFalse(isValidEMail("foo"));
        assertFalse(isValidEMail("foo.bar"));
        assertFalse(isValidEMail("foo@bar@baz"));
    }

    @Test
    public void emailValid() {
        assertTrue(isValidEMail("nobody@predic8.de"));
    }

    @Test
    public void uriValid() {
        assertTrue(isValidUri("http://www.ics.uci.edu/pub/ietf/uri/#Related"));
        assertTrue(isValidUri("urn:bar"));

        // From https://www.rfc-editor.org/rfc/rfc3986#section-1.1.2
        String[] samples = {
                "ftp://ftp.is.co.za/rfc/rfc1808.txt",
                "http://www.ietf.org/rfc/rfc2396.txt",
                "ldap://[2001:db8::7]/c=GB?objectClass?one",
                "mailto:John.Doe@example.com",
                "news:comp.infosystems.www.servers.unix",
                "tel:+1-816-555-1212",
                "telnet://192.0.2.16:80/",
                "urn:oasis:names:specification:docbook:dtd:xml:4.1.2"};

        for (int i = 0; i < samples.length; i++) {
            assertTrue(isValidUri(samples[i]));
        }
      }

    @Test
    public void uriInvalid() {
        assertFalse(isValidUri("http"));
    }

    @Test
    public void dateValid() {
        assertTrue(isValidDate("2022-11-19"));
        assertTrue(isValidDate("2022-12-31"));
    }

    @Test
    public void dateInvalid() {
        assertFalse(isValidDate("2022-02-31"));
        assertFalse(isValidDate("2022-02-29"));
        assertFalse(isValidDate("2022-1-29"));
        assertFalse(isValidDate("2022-14-29"));
    }

    @Test
    public void dateTimeValid() {
        assertTrue(isValidDateTime("2009-01-01T12:00:00+01:00"));
        assertTrue(isValidDateTime("2007-08-31T16:47+00:00"));
        assertTrue(isValidDateTime("2008-02-01T09:00:22"));
        assertTrue(isValidDateTime("2008-02-01T09:00"));
    }

    @Test
    public void dateTimeInvalid() {
        assertFalse(isValidDateTime("2008-02-01"));
    }
}