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

public class WebBundlerTeaVMTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.web-bundler.teavm.main-class", HelloTeaVM.class.getName())
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloTeaVM.class));

    @Inject
    Bundle bundle;

    @Test
    public void test() {
        // No index file: the TeaVM module is auto-imported (exports re-exported) and main is called
        RestAssured.given()
                .basePath("")
                .get(bundle.script("app"))
                .then()
                .statusCode(200)
                .body(containsString("Hello from TeaVM, "))
                .body(containsString("TeaVM main started"))
                .body(containsString(" as greet"));
    }
}
