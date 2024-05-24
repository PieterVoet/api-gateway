package com.predic8.membrane.core;

import com.predic8.membrane.core.interceptor.*;
import com.predic8.membrane.core.interceptor.misc.*;
import com.predic8.membrane.core.openapi.serviceproxy.*;
import com.predic8.membrane.core.rules.*;
import io.restassured.response.*;
import org.jetbrains.annotations.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class OpenApiRewriteIntegrationTest {

    private static final Router r = new HttpRouter();

    @BeforeEach
    public void setUp() throws Exception {
        r.getRuleManager().addProxyAndOpenPortIfNew(getApiProxy());
        r.getRuleManager().addProxyAndOpenPortIfNew(getTargetProxy());
        r.init();
    }

    @NotNull
    private static Rule getTargetProxy() throws Exception {
        Rule targetProxy = new ServiceProxy(new ServiceProxyKey("localhost", "GET", ".*", 3000), null, 8000);
        targetProxy.getInterceptors().add(new ReturnInterceptor());
        targetProxy.init(r);
        return targetProxy;
    }

    @NotNull
    private static APIProxy getApiProxy() throws Exception {
        APIProxy proxy = new APIProxy();
        OpenAPISpec spec = getSpec();
        Rewrite rw = new Rewrite();
        rw.setUrl("/bar");
        spec.setRewrite(rw);
        spec.setValidateRequests(OpenAPISpec.YesNoOpenAPIOption.YES);
        proxy.setSpecs(singletonList(spec));
        proxy.setKey(new OpenAPIProxyServiceKey(null, "*", 2000));
        proxy.getInterceptors().add(new LogInterceptor());
        proxy.init(r);
        return proxy;
    }

    @Test
    void rewriteUrlInOpenAPIDocument() {
        Response r = given()
                .when()
                .get("http://localhost:2000/api-docs/rewriting-test-v1-0");

        assertTrue(r.asString().contains("http://localhost:2000/bar"));

        r.then().statusCode(200);
    }


    /**
     * The path /api/v2/foo in the OpenAPI should be rewritten to /bar/foo
     */
    @Test
    void rewriteURLInOpenAPI() {
        // @formatter:off
        given()
        .when()
            .get("http://localhost:2000/bar/foo")
        .then()
            .statusCode(200);
        // @formatter:on
    }


    @AfterAll
    public static void tearDown() throws Exception {
        r.shutdown();
    }

    @NotNull
    private static OpenAPISpec getSpec() {
        OpenAPISpec s = new OpenAPISpec();
        s.location = "src/test/resources/openapi/specs/rewrite-integration-test.yml";
        return s;
    }
}