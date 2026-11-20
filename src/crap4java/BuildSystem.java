package crap4java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

enum BuildSystem {
    MAVEN("pom.xml",
            List.of("target/site/jacoco", "target/jacoco.exec"),
            "target/site/jacoco/jacoco.xml"),
    GRADLE("build.gradle",
            List.of("build/reports/jacoco", "build/jacoco/test.exec"),
            "build/reports/jacoco/test/jacocoTestReport.xml");

    final Path markerFile;
    final List<String> staleCoveragePaths;
    final String jacocoXmlPath;

    BuildSystem(String markerFile, List<String> staleCoveragePaths, String jacocoXmlPath) {
        this.markerFile = Path.of(markerFile);
        this.staleCoveragePaths = staleCoveragePaths;
        this.jacocoXmlPath = jacocoXmlPath;
    }

    Path jacocoXml(Path moduleRoot) {
        return moduleRoot.resolve(jacocoXmlPath);
    }

    boolean matches(Path moduleRoot) {
        return Files.exists(moduleRoot.resolve(markerFile))
                || (this == GRADLE && Files.exists(moduleRoot.resolve("build.gradle.kts")));
    }

    static BuildSystem forModule(Path moduleRoot) {
        for (BuildSystem system : values()) {
            if (system.matches(moduleRoot)) {
                return system;
            }
        }
        return MAVEN;
    }
}

/* mutate4java-manifest
version=1
moduleHash=PLACEHOLDER
*/
