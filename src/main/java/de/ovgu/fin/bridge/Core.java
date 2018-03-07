package de.ovgu.fin.bridge;

import de.ovgu.fin.bridge.api.RestApi;
import de.ovgu.fin.bridge.prometheus.ConfigurationUpdater;
import de.ovgu.fin.bridge.prometheus.PrometheusHeartbeatCheck;
import de.ovgu.fin.bridge.prometheus.PrometheusProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 06.11.2017.
 */
public class Core {

    public static final Logger LOGGER = LoggerFactory.getLogger("SCPB");


    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            LOGGER.error("Usage: SCBP.jar [PATH_TO_PROMETHEUS_DIR] (PORT) (RETENTION_TIME)");
            return;
        }
        String path = args[0];
        int port = 7536;
        Duration retentionTime = Duration.ofDays(90);
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            retentionTime = Duration.parse("P" + args[2]);
        }

        LOGGER.info("Starting SCPB!");

        new Core(path, retentionTime).start(port);
    }

    private ConfigurationUpdater configurationUpdater;
    private PrometheusProcess prometheusProcess;
    private PrometheusHeartbeatCheck prometheusHeartbeatCheck;

    private final ScheduledExecutorService scheduler;

    private Core(String prometheusDir, Duration retentionTime) throws Exception {
        String configFilePath = new File(prometheusDir, "prometheus.yml").getPath();
        if (!new File(configFilePath).exists()) {
            throw new IOException("Prometheus config file at '" + configFilePath + "' not found!");
        }
        scheduler = Executors.newScheduledThreadPool(2);


        LOGGER.info("Config file of Prometheus: " + configFilePath);
        LOGGER.info("Storage retention time: " + retentionTime.getSeconds() + "s");
        this.prometheusProcess = new PrometheusProcess(prometheusDir, retentionTime);
        this.prometheusHeartbeatCheck = new PrometheusHeartbeatCheck(prometheusProcess);
        this.configurationUpdater = new ConfigurationUpdater(configFilePath, prometheusProcess);
    }

    private void start(int port) throws Exception {

        startUpdaterThread();
        startPrometheus();

        new RestApi(configurationUpdater).registerApi(port);

        // Write current port numbers at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configurationUpdater.close();
                prometheusProcess.stop();
                scheduler.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Shutdown-thread"));

        LOGGER.info("SpeedCam Prometheus Bridge started!");
    }

    private void startPrometheus() throws Exception {
        LOGGER.info("Starting prometheus ...");
        this.prometheusProcess.start();
        LOGGER.info("Prometheus started!");

        scheduler.scheduleAtFixedRate(runnableWithException(this.prometheusHeartbeatCheck), TimeUnit.MINUTES.toSeconds(1L), 5, TimeUnit.SECONDS);
        LOGGER.info("Prometheus heartbeat checker started!");
    }

    private void startUpdaterThread() {
        scheduler.scheduleAtFixedRate(runnableWithException(this.configurationUpdater), 0, 5, TimeUnit.SECONDS);
        LOGGER.info("Started configuration update scheduler with interval 5s");
    }

    /**
     * This methods encapsulates a runnable with a try-catch, because scheduled executor service suppresses
     * exceptions and errors. Without this method it is possible to miss errors.
     *
     * @param runnable The runnable to encapsulates.
     * @return The same runnable with a try-catch around.
     */
    private Runnable runnableWithException(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                LOGGER.error("Thread error!", e);
            }
        };
    }
}
