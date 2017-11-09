package de.ovgu.fin.bridge;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
class PrometheusProcess {
    private Process holder;

    private final String prometheusDir;

    PrometheusProcess(String prometheusDir) {
        this.prometheusDir = prometheusDir;
    }

    void start() throws Exception {
        if (isRunning())
            return;
        ProcessBuilder builder = new ProcessBuilder()
                .directory(new File(prometheusDir))
                .command(prometheusDir + "/prometheus", "-config.file=prometheus.yml");

        this.holder = builder.start();
    }

    void stop() throws Exception {
        if (!isRunning())
            return;

        this.holder.destroy();
        LocalTime start = LocalTime.now();
        LOGGER.info("Waiting for prometheus to stop (max 5 sec)...");
        this.holder.waitFor(5, TimeUnit.SECONDS);
        LOGGER.info("Waited for prometheus shutdown: " + Duration.between(start, LocalTime.now()).getSeconds() + "s");
        this.holder = null;
    }

    private boolean isRunning() {
        return this.holder != null;
    }
}
