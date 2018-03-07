package de.ovgu.fin.bridge.prometheus;

import de.ovgu.fin.bridge.Core;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Created on 22.12.2017.
 */
public class PrometheusHeartbeatCheck implements Runnable {

    private static final Duration checkDuration = Duration.ofMinutes(1L);

    private final PrometheusProcess prometheusProcess;
    private LocalDateTime checkTime;

    public PrometheusHeartbeatCheck(PrometheusProcess prometheusProcess) {
        this.prometheusProcess = prometheusProcess;
    }

    @Override
    public void run() {

        if (prometheusProcess.isRunning()) {
            // if there was a checktime and it was passed , reset the checkTime
            if (checkTime != null && LocalDateTime.now().isAfter(checkTime)) {
                Core.LOGGER.warn("Prometheus server was down, but was normally restarted.");
                checkTime = null;
            }
            // Do nothing if prometheus server is alive
            return;
        }


        // If the prometheus server isn't alive, wait a few minutes before force restart
        if (checkTime == null) {
            checkTime = LocalDateTime.now().plus(checkDuration);
            Core.LOGGER.warn("Prometheus server down! Wait " + checkDuration + " (till '" + checkTime + "') before forced restart!");
            return;
        }

        // If the checktime has passed without anything, force restart
        if (LocalDateTime.now().isAfter(checkTime)) {

            Core.LOGGER.warn("Starting force restart of prometheus!");
            try {
                prometheusProcess.stop();
                prometheusProcess.start();
                checkTime = null;
                Core.LOGGER.warn("Forced restart was successful.");
            } catch (Exception e) {
                Core.LOGGER.error("Error while restarting prometheus", e);
            }
        }
    }
}
