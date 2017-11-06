package de.ovgu.fin.bridge;

import java.io.File;

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
                .command(prometheusDir+"/prometheus", "-config.file=prometheus.yml");

        this.holder = builder.start();
    }

    void stop() throws Exception {
        if (!isRunning())
            return;

        this.holder.destroy();
        this.holder = null;
    }

    private boolean isRunning() {
        return this.holder != null;
    }
}
