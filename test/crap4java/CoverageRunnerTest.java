package crap4java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoverageRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void deletesStaleCoverageAndRunsMavenCoverageCommand() throws Exception {
        Path jacocoDir = tempDir.resolve("target/site/jacoco");
        Files.createDirectories(jacocoDir);
        Files.writeString(jacocoDir.resolve("old.xml"), "stale");
        Path exec = tempDir.resolve("target/jacoco.exec");
        Files.createDirectories(exec.getParent());
        Files.writeString(exec, "stale");

        RecordingExecutor executor = new RecordingExecutor(0);
        CoverageRunner runner = new CoverageRunner(executor);

        runner.generateCoverage(tempDir, BuildSystem.MAVEN);

        assertFalse(Files.exists(jacocoDir));
        assertFalse(Files.exists(exec));
        assertEquals(List.of(
                "mvn", "-q",
                "org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent",
                "test",
                "org.jacoco:jacoco-maven-plugin:0.8.12:report"
        ), executor.commands.get(0));
        assertEquals(tempDir, executor.directories.get(0));
    }

    @Test
    void failsWhenCoverageCommandFails() {
        RecordingExecutor executor = new RecordingExecutor(2);
        CoverageRunner runner = new CoverageRunner(executor);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> runner.generateCoverage(tempDir, BuildSystem.MAVEN));

        assertEquals("Coverage command failed with exit 2", ex.getMessage());
    }

    @Test
    void deletesStaleGradleCoverageAndUsesRootWrapper() throws Exception {
        Path wrapper = Files.createFile(tempDir.resolve("gradlew"));
        Path module = Files.createDirectories(tempDir.resolve("module"));
        Path report = Files.createDirectories(module.resolve("build/reports/jacoco/test"));
        Files.writeString(report.resolve("jacocoTestReport.xml"), "stale");
        Path exec = module.resolve("build/jacoco/test.exec");
        Files.createDirectories(exec.getParent());
        Files.writeString(exec, "stale");
        RecordingExecutor executor = new RecordingExecutor(0);

        new CoverageRunner(executor).generateCoverage(module, BuildSystem.GRADLE);

        assertFalse(Files.exists(report));
        assertFalse(Files.exists(exec));
        assertEquals(List.of(wrapper.toString(), "test", "jacocoTestReport"), executor.commands.get(0));
        assertEquals(module, executor.directories.get(0));
    }

    @Test
    void usesInstalledGradleWhenWrapperIsMissing() throws Exception {
        RecordingExecutor executor = new RecordingExecutor(0);

        new CoverageRunner(executor).generateCoverage(tempDir, BuildSystem.GRADLE);

        assertEquals(List.of("gradle", "test", "jacocoTestReport"), executor.commands.get(0));
    }

    private static final class RecordingExecutor implements CommandExecutor {
        private final int exitCode;
        private final List<List<String>> commands = new ArrayList<>();
        private final List<Path> directories = new ArrayList<>();

        private RecordingExecutor(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public int run(List<String> command, Path directory) {
            commands.add(command);
            directories.add(directory);
            return exitCode;
        }
    }
}
