package io.quarkiverse.web.bundler.plugin.teavm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.web-bundler.teavm")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TeaVMConfig {

    /**
     * The fully qualified name of the Java class to compile to JavaScript with TeaVM.
     * <p>
     * Static methods annotated with {@code @org.teavm.jso.JSExport} are exported from the generated module.
     * If the class has a {@code main(String[])} method, it is exported as {@code main}.
     * </p>
     * <p>
     * When not set, nothing is compiled.
     * </p>
     */
    Optional<String> mainClass();

    /**
     * The Web Bundler entry point the generated script is added to.
     * <p>
     * The script is added as {@code teavm.js} in the entry point directory. When the entry point has no index file,
     * it is auto-imported and {@code main} is called on load. Otherwise import it from your index file:
     * {@code import { main, myExport } from "./teavm.js";}
     * </p>
     */
    @WithDefault("app")
    String entryPoint();

}
