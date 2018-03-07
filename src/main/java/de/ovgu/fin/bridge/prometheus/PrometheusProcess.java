package de.ovgu.fin.bridge.prometheus;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
public class PrometheusProcess {

    private static final String CONFIG_FILE_PROMETHEUS_YML = "--config.file=prometheus.yml";
    private static final String WEB_ENABLE_REMOTE_SHUTDOWN = "--web.enable-lifecycle";
    private static final String PROMETHEUS_SHUTDOWN_URL = "http://localhost:9090/-/quit";
    private static final String PROMETHEUS_RELOAD_URL = "http://localhost:9090/-/reload";
    private static final String STORAGE_RETENTION_FLAG_PREFIX = "--storage.tsdb.retention=";

    private Process holder;

    private final String prometheusDir;
    private final Duration retentionTime;

    public PrometheusProcess(String prometheusDir, Duration retentionTime) {
        this.prometheusDir = prometheusDir;
        this.retentionTime = retentionTime;
    }

    public void start() throws Exception {
        if (isRunning())
            return;
        ProcessBuilder builder = new ProcessBuilder()
                .directory(new File(prometheusDir))
                .inheritIO()
                .command(prometheusDir + "/prometheus", CONFIG_FILE_PROMETHEUS_YML, WEB_ENABLE_REMOTE_SHUTDOWN, retentionTimeFlag());
        LOGGER.info("Prometheus flags: " + builder.command());
        this.holder = builder.start();
    }

    void reload() throws Exception {
        Unirest.post(PROMETHEUS_RELOAD_URL).asString().getStatus();
    }

    public void stop() throws Exception {
        if (!isRunning())
            return;

        LocalTime start = LocalTime.now();
        LOGGER.info("Waiting for prometheus to stop (max 5 sec)...");
        try {
            sendShutdownToPrometheus();
        } catch (UnirestException e) {
            if (e.getMessage().contains("Connection refused")) {
                LOGGER.warn("Prometheus was not running!");
            } else
                throw e;
        }
        this.holder.waitFor(5, TimeUnit.SECONDS);
        LOGGER.info("Waited for prometheus shutdown: " + Duration.between(start, LocalTime.now()).getSeconds() + "s");
        this.holder = null;
    }

    private void sendShutdownToPrometheus() throws Exception {
        Unirest.post(PROMETHEUS_SHUTDOWN_URL).asString().getStatus();
    }

    boolean isRunning() {
        return this.holder != null && this.holder.isAlive();
    }

    private String retentionTimeFlag() {
        return STORAGE_RETENTION_FLAG_PREFIX + retentionTime.toDays() + "d";
    }
}
