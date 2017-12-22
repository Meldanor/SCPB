package de.ovgu.fin.bridge;

import com.mashape.unirest.http.Unirest;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
class PrometheusProcess {

    private static final String CONFIG_FILE_PROMETHEUS_YML = "-config.file=prometheus.yml";
    private static final String WEB_ENABLE_REMOTE_SHUTDOWN = "-web.enable-remote-shutdown";
    private static final String PROMETHEUS_SHUTDOWN_URL = "http://localhost:9090/-/quit";

    private Process holder;

    private final String prometheusDir;
    private final Duration retentionTime;

    PrometheusProcess(String prometheusDir, Duration retentionTime) {
        this.prometheusDir = prometheusDir;
        this.retentionTime = retentionTime;
    }

    void start() throws Exception {
        if (isRunning())
            return;
        ProcessBuilder builder = new ProcessBuilder()
                .directory(new File(prometheusDir))
                .inheritIO()
                .command(prometheusDir + "/prometheus", CONFIG_FILE_PROMETHEUS_YML, WEB_ENABLE_REMOTE_SHUTDOWN, retentionTimeFlag());
        LOGGER.info("Prometheus flags: " + builder.command());
        this.holder = builder.start();
    }

    void stop() throws Exception {
        if (!isRunning())
            return;

        LocalTime start = LocalTime.now();
        LOGGER.info("Waiting for prometheus to stop (max 5 sec)...");
        sendShutdownToPrometheus();
        this.holder.waitFor(5, TimeUnit.SECONDS);
        LOGGER.info("Waited for prometheus shutdown: " + Duration.between(start, LocalTime.now()).getSeconds() + "s");
        this.holder = null;
    }

    private void sendShutdownToPrometheus() throws Exception {
        Unirest.post(PROMETHEUS_SHUTDOWN_URL).asString().getStatus();
    }

    private boolean isRunning() {
        return this.holder != null;
    }

    private String retentionTimeFlag() {
        long hours = retentionTime.toHours();
        long minutes = retentionTime.minusHours(hours).toMinutes();
        long seconds = retentionTime.minusHours(hours).minusMinutes(minutes).getSeconds();
        return "-storage.local.retention=" + hours + "h" + minutes + "m" + seconds + "s";
    }
}
