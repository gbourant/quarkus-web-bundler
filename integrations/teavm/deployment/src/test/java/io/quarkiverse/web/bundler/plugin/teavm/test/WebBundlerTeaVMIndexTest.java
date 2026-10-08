package io.quarkiverse.web.bundler.plugin.teavm.test;

import static org.hamcrest.Matchers.containsString;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.web.bundler.runtime.Bundle;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WebBundlerTeaVMIndexTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.web-bundler.teavm.main-class", HelloTeaVM.class.getName())
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloTeaVM.class)
                    .addAsResource("web-index", "web"));

    @Inject
    Bundle bundle;

    @Test
    public void test() {
        // With an index file, the TeaVM module is imported manually
        RestAssured.given()
                .basePath("")
                .get(bundle.script("app"))
                .then()
                .statusCode(200)
                .body(containsString("Hello from TeaVM, "))
                .body(containsString("greeting from index"));
    }
}
