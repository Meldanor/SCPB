package de.ovgu.fin.bridge;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.reflect.VisibilityFilter;
import de.ovgu.fin.bridge.api.RestApi;
import de.ovgu.fin.bridge.prometheus.ConfigurationUpdater;
import de.ovgu.fin.bridge.speedcam.PathServerRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 06.11.2017.
 */
public class Core {

    public static final Logger LOGGER = LoggerFactory.getLogger("SCPB");


    public static void main(String[] args) throws Exception {
        if (args.length <= 1) {
            LOGGER.error("Usage: SCBP.jar [PATH_TO_PROMETHEUS_DIR] [PROMETHEUS_URL] (PORT)");
            return;
        }
        String path = args[0];
        URL prometheusUrl = new URL(args[1]);
        int port = 7536;
        if (args.length >= 3) {
            port = Integer.parseInt(args[2]);
        }

        LOGGER.info("Starting SCPB!");

        new Core(path, prometheusUrl).start(port);
    }

    private ConfigurationUpdater configurationUpdater;
    private PathServerRequestProxy pathServerRequestProxy;

    private final ScheduledExecutorService scheduler;

    private Core(String prometheusDir, URL prometheusUrl) throws Exception {
        String configFilePath = new File(prometheusDir, "prometheus.yml").getPath();
        if (!new File(configFilePath).exists()) {
            throw new IOException("Prometheus config file at '" + configFilePath + "' not found!");
        }
        scheduler = Executors.newScheduledThreadPool(2);


        LOGGER.info("Config file of Prometheus: " + configFilePath);
        this.configurationUpdater = new ConfigurationUpdater(configFilePath, prometheusUrl);
        this.pathServerRequestProxy = new PathServerRequestProxy();
    }

    private void start(int port) {

        startUpdaterThread();

        new RestApi(configurationUpdater, pathServerRequestProxy).registerApi(port);

        // Write current port numbers at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configurationUpdater.close();
                scheduler.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Shutdown-thread"));

        LOGGER.info("SpeedCam Prometheus Bridge started!");
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

    public static Genson createSerializer() {
        return new GensonBuilder()
                .useFields(true)
                .setFieldFilter(VisibilityFilter.ALL)
                .create();
    }
}
