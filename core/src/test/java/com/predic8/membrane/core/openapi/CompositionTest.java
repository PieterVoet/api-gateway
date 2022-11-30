package com.predic8.membrane.core.openapi;

import com.predic8.membrane.core.openapi.model.*;
import com.predic8.membrane.core.openapi.validators.*;
import org.junit.*;

import java.io.*;
import java.util.*;

import static com.predic8.membrane.core.openapi.util.JsonUtil.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CompositionTest {

    OpenAPIValidator validator;

    @Before
    public void setUp() {
        validator = new OpenAPIValidator(getResourceAsStream("/openapi/composition.yml"));
    }

    @Test
    public void allOfValid() {

        Map m = new HashMap();
        m.put("firstname","Otto");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void allOfTooLongInvalid() {

        Map m = new HashMap();
        m.put("firstname","123456");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(2,errors.size());

        ValidationError enumError = errors.stream().filter(e -> e.getMessage().contains("axLength")).findAny().get();
        assertEquals("/firstname", enumError.getValidationContext().getJSONpointer());

        ValidationError allOf = errors.stream().filter(e -> e.getMessage().contains("allOf")).findAny().get();
        assertEquals("/firstname", allOf.getValidationContext().getJSONpointer());
        assertTrue(allOf.getMessage().contains("subschemas"));

    }

    @Test
    public void allOfTooShortInvalid() {

        Map m = new HashMap();
        m.put("firstname","12");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(2,errors.size());

        ValidationError enumError = errors.stream().filter(e -> e.getMessage().contains("minLength")).findAny().get();
        assertEquals("/firstname", enumError.getValidationContext().getJSONpointer());

        ValidationError allOf = errors.stream().filter(e -> e.getMessage().contains("allOf")).findAny().get();
        assertEquals("/firstname", allOf.getValidationContext().getJSONpointer());
        assertTrue(allOf.getMessage().contains("subschemas"));
    }

    @Test
    public void anyOfeMailValid() {

        Map m = new HashMap();
        m.put("contact","membrane@predic8.de");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void anyOfTelValid() {

        Map m = new HashMap();
        m.put("contact","123");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void anyOfInvalid() {

        Map m = new HashMap();
        m.put("contact","Bonn");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
        System.out.println("errors = " + errors);
        assertEquals(1,errors.size());
        ValidationError e = errors.get(0);
        assertEquals("/contact", e.getValidationContext().getJSONpointer());
        assertTrue(e.getMessage().contains("anyOf"));
    }

    @Test
    public void oneOfNoneInvalid() {

        Map m = new HashMap();
        m.put("multiple",7);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
        System.out.println("errors = " + errors);
        assertEquals(1,errors.size());
        ValidationError e = errors.get(0);
        assertEquals("/multiple", e.getValidationContext().getJSONpointer());
        assertTrue(e.getMessage().contains("neOf"));
    }

    @Test
    public void oneOfTwoInvalid() {

        Map m = new HashMap();
        m.put("multiple",15);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
        System.out.println("errors = " + errors);
        assertEquals(1,errors.size());
        ValidationError e = errors.get(0);
        assertEquals("/multiple", e.getValidationContext().getJSONpointer());
        assertTrue(e.getMessage().contains("neOf"));
    }

    @Test
    public void oneOfValid() {

        Map m = new HashMap();
        m.put("multiple",21);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void factoredOutInvalid() {

        Map m = new HashMap();
        m.put("factored-out",15);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
        System.out.println("errors = " + errors);
        assertEquals(1,errors.size());
        ValidationError e = errors.get(0);
        assertEquals("/factored-out", e.getValidationContext().getJSONpointer());
        assertTrue(e.getMessage().contains("neOf"));
        assertTrue(e.getMessage().contains("2 subschemas"));
    }

    @Test
    public void factoredOutValid() {

        Map m = new HashMap();
        m.put("factored-out",21);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void notStringValid() {

        Map m = new HashMap();
        m.put("not-string",true);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void notStringInvalid() {

        Map m = new HashMap();
        m.put("not-string","abc");

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(m)));
//        System.out.println("errors = " + errors);
        assertEquals(1,errors.size());
        ValidationError e = errors.get(0);
        assertEquals("/not-string", e.getValidationContext().getJSONpointer());
        assertTrue(e.getMessage().contains("not"));
    }

    @Test
    public void inheritanceValid() {

        Map address = new HashMap();
        address.put("street","Koblenzer Str. 65");
        address.put("city","Bonn");
        address.put("country","IN"); // DE is not valid

        Map inheritance = new HashMap();
        inheritance.put("inheritance",address);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(inheritance)));
//        System.out.println("errors = " + errors);
        assertEquals(0,errors.size());
    }

    @Test
    public void inheritanceWrongPlaceInvalid() {

        Map address = new HashMap();
        address.put("street","Koblenzer Str. 65");
        address.put("city","Bonn");
        address.put("country","DE"); // DE is not valid

        Map inheritance = new HashMap();
        inheritance.put("inheritance",address);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(inheritance)));
//        System.out.println("errors = " + errors);
        assertEquals(2,errors.size());
        ValidationError enumError = errors.stream().filter(e -> e.getMessage().contains("enum")).findAny().get();
        assertEquals("/inheritance/country", enumError.getValidationContext().getJSONpointer());
        assertTrue(enumError.getMessage().contains("does not contain a value from the enum"));

        ValidationError allOf = errors.stream().filter(e -> e.getMessage().contains("allOf")).findAny().get();
        assertEquals("/inheritance", allOf.getValidationContext().getJSONpointer());
        assertTrue(allOf.getMessage().contains("subschemas of allOf"));
    }

    @Test
    public void inheritanceInvalid() {

        Map address = new HashMap();
        address.put("street","Koblenzer Str. 65");
        address.put("city","Bonn");

        Map inheritance = new HashMap();
        inheritance.put("inheritance",address);

        ValidationErrors errors = validator.validate(Request.post().path("/composition").body(mapToJson(inheritance)));
//        System.out.println("errors = " + errors);
        assertEquals(2,errors.size());

        ValidationError enumError = errors.stream().filter(e -> e.getMessage().toLowerCase().contains("required")).findAny().get();
        assertEquals("/inheritance/country", enumError.getValidationContext().getJSONpointer());

        ValidationError allOf = errors.stream().filter(e -> e.getMessage().contains("allOf")).findAny().get();
        assertEquals("/inheritance", allOf.getValidationContext().getJSONpointer());
        assertTrue(allOf.getMessage().contains("subschemas"));

    }


    private InputStream getResourceAsStream(String fileName) {
        return this.getClass().getResourceAsStream(fileName);
    }

}