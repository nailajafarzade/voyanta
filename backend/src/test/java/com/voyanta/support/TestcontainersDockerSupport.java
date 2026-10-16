package com.voyanta.support;

/**
 * Docker Engine 29+ refuses clients older than API 1.44. Testcontainers 1.20.x still
 * defaults to 1.32, which surfaces as "Could not find a valid Docker environment"
 * even when Docker Desktop itself is healthy. On Windows we also pin the Linux
 * engine named pipe so Testcontainers does not attach to the Windows engine.
 */
final class TestcontainersDockerSupport {

    private static final String LINUX_NPIPE = "npipe:////./pipe/dockerDesktopLinuxEngine";
    private static final String FALLBACK_NPIPE = "npipe:////./pipe/docker_engine";

    private TestcontainersDockerSupport() {
    }

    static void install() {
        if (System.getProperty("api.version") == null || System.getProperty("api.version").isBlank()) {
            System.setProperty("api.version", "1.44");
        }
        if (System.getenv("DOCKER_API_VERSION") == null) {
            System.setProperty("DOCKER_API_VERSION", "1.44");
        }

        if (!isWindows()) {
            return;
        }

        String dockerHost = firstNonBlank(
                System.getenv("DOCKER_HOST"),
                System.getProperty("DOCKER_HOST"),
                System.getProperty("docker.host"),
                preferredWindowsPipe()
        );
        System.setProperty("DOCKER_HOST", dockerHost);
        System.setProperty("docker.host", dockerHost);
        System.setProperty("testcontainers.reuse.enable", "false");
    }

    private static String preferredWindowsPipe() {
        if (namedPipeExists("dockerDesktopLinuxEngine")) {
            return LINUX_NPIPE;
        }
        if (namedPipeExists("docker_engine_linux")) {
            return "npipe:////./pipe/docker_engine_linux";
        }
        return FALLBACK_NPIPE;
    }

    private static boolean namedPipeExists(String pipeName) {
        try {
            return new java.io.File("\\\\.\\pipe\\" + pipeName).exists();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return LINUX_NPIPE;
    }
}
