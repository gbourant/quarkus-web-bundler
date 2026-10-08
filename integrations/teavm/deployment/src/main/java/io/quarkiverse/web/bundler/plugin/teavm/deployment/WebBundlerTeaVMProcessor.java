package io.quarkiverse.web.bundler.plugin.teavm.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;

import io.quarkiverse.web.bundler.deployment.items.GeneratedBundleWebAssetBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;

public class WebBundlerTeaVMProcessor {
    private static final Logger LOGGER = Logger.getLogger(WebBundlerTeaVMProcessor.class);
    private static final String FEATURE = "web-bundler-teavm";
    static final String SCRIPT_NAME = "teavm.js";
    static final String MAIN_SCRIPT_NAME = "teavm-main.js";
    // TeaVM ends the ES module with e.g. "export { $rt_export_main as main, ... };" when the class has a main method
    private static final Pattern MAIN_EXPORT = Pattern.compile("^export \\{[^}]*\\bas main\\b", Pattern.MULTILINE);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void compile(TeaVMConfig config,
            ApplicationArchivesBuildItem applicationArchives,
            OutputTargetBuildItem outputTarget,
            LaunchModeBuildItem launchMode,
            BuildProducer<GeneratedBundleWebAssetBuildItem> generatedAssets) {
        if (config.mainClass().isEmpty()) {
            return;
        }
        final String mainClass = config.mainClass().get();
        // Not under target/web-bundler, it is deleted when the bundle is prepared
        final Path targetDir = outputTarget.getOutputDirectory()
                .resolve("teavm")
                .resolve(launchMode.getLaunchMode().getDefaultProfile());
        try (URLClassLoader classLoader = new URLClassLoader(toUrls(applicationArchives.getRootArchive()),
                WebBundlerTeaVMProcessor.class.getClassLoader())) {
            FileUtil.deleteDirectory(targetDir);
            final TeaVMTool tool = new TeaVMTool();
            tool.setLog(new JBossTeaVMToolLog());
            tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
            tool.setJsModuleType(JSModuleType.ES2015);
            tool.setMainClass(mainClass);
            tool.setTargetDirectory(targetDir.toFile());
            tool.setTargetFileName(SCRIPT_NAME);
            // esbuild takes care of the minification
            tool.setObfuscated(false);
            tool.setClassLoader(classLoader);
            final long start = System.currentTimeMillis();
            tool.generate();
            failOnSevereProblems(mainClass, tool.getProblemProvider().getSevereProblems());
            LOGGER.infof("TeaVM compiled '%s' to JavaScript in %sms", mainClass, System.currentTimeMillis() - start);

            final byte[] script = Files.readAllBytes(targetDir.resolve(SCRIPT_NAME));
            generatedAssets.produce(new GeneratedBundleWebAssetBuildItem(config.entryPoint(), SCRIPT_NAME, script));
            if (MAIN_EXPORT.matcher(new String(script, StandardCharsets.UTF_8)).find()) {
                // The ES module only exports main, this calls it when auto-imported
                final String mainScript = "import { main } from \"./%s\";\nmain([]);\n".formatted(SCRIPT_NAME);
                generatedAssets.produce(new GeneratedBundleWebAssetBuildItem(config.entryPoint(), MAIN_SCRIPT_NAME,
                        mainScript.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (TeaVMToolException e) {
            throw new IllegalStateException("TeaVM failed to compile '%s' to JavaScript".formatted(mainClass), e);
        }
    }

    private static void failOnSevereProblems(String mainClass, List<Problem> problems) {
        if (problems.isEmpty()) {
            return;
        }
        final String message = problems.stream()
                .map(p -> {
                    final DefaultProblemTextConsumer consumer = new DefaultProblemTextConsumer();
                    p.render(consumer);
                    String text = "  - " + consumer.getText();
                    if (p.getLocation() != null && p.getLocation().getMethod() != null) {
                        text += "\n    at " + p.getLocation().getMethod();
                    }
                    return text;
                })
                .collect(Collectors.joining("\n"));
        throw new IllegalStateException(
                "TeaVM failed to compile '%s' to JavaScript:\n%s".formatted(mainClass, message));
    }

    private static URL[] toUrls(ApplicationArchive archive) throws MalformedURLException {
        final List<URL> urls = new ArrayList<>();
        for (Path path : archive.getResolvedPaths()) {
            urls.add(path.toUri().toURL());
        }
        return urls.toArray(URL[]::new);
    }

    private static final class JBossTeaVMToolLog implements TeaVMToolLog {

        @Override
        public void info(String text) {
            LOGGER.debug(text);
        }

        @Override
        public void debug(String text) {
            LOGGER.debug(text);
        }

        @Override
        public void warning(String text) {
            LOGGER.warn(text);
        }

        @Override
        public void error(String text) {
            LOGGER.error(text);
        }

        @Override
        public void info(String text, Throwable e) {
            LOGGER.debug(text, e);
        }

        @Override
        public void debug(String text, Throwable e) {
            LOGGER.debug(text, e);
        }

        @Override
        public void warning(String text, Throwable e) {
            LOGGER.warn(text, e);
        }

        @Override
        public void error(String text, Throwable e) {
            LOGGER.error(text, e);
        }
    }
}
