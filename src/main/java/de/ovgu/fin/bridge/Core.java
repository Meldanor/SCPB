package de.ovgu.fin.bridge;

import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 06.11.2017.
 */
public class Core {

    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            System.out.println("Usage: SpeedCamePrometheusBridge.jar [PATH_TO_PROMETHEUS_DIR] [AS 1-7 IPv4] (PORT)");
            return;
        }
        String path = args[0];
        String as17ipv4 = args[1];
        int port = 7536;
        if (args.length == 3) {
            port = Integer.parseInt(args[2]);
        }
        new Core(path, as17ipv4).start(port);
    }

    private ConfigurationUpdater configurationUpdater;
    private PrometheusProcess prometheusProcess;

    private Core(String prometheusDir, String as17ipv4) throws Exception {
        String configFilePath = new File(prometheusDir, "prometheus.yml").getPath();
        if (!new File(configFilePath).exists()) {
            throw new IOException("Prometheus config file at '" + configFilePath + "' not found!");
        }
        System.out.println("Config file of Prometheus: " + configFilePath);
        System.out.println("IPv4 of AS 1-7: " + as17ipv4);
        this.prometheusProcess = new PrometheusProcess(prometheusDir);
        this.configurationUpdater = new ConfigurationUpdater(configFilePath, as17ipv4, prometheusProcess);

    }

    private void start(int port) throws Exception {

        startUpdaterThread();
        startPrometheus();
        startRestService(port);

        // Write current port numbers at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configurationUpdater.close();
                prometheusProcess.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Shutdown-thread"));

        System.out.println("SpeedCam Prometheus Bridge started!");
    }

    private void startPrometheus() throws Exception {
        System.out.println("Starting prometheus ...");
        this.prometheusProcess.start();
        System.out.println("Prometheus started!");
    }

    private void startUpdaterThread() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this.configurationUpdater, 0, 30, TimeUnit.SECONDS);
        System.out.println("Started configuration update scheduler");
    }

    private void startRestService(int port) {
        Spark.port(port);
        registerRestResources();

        System.out.println("REST service started at port " + port);
    }

    private void registerRestResources() {
        Spark.put("/registerClient/:port", (request, response) -> {
            int newPort = Integer.parseInt(request.params("port"));
            if (configurationUpdater.registerNewPortNumber(newPort))
                response.status(200);
            else
                response.status(304);
            return "";
        });
    }
}
