package de.ovgu.fin.bridge;

import spark.Spark;

import java.io.File;
import java.io.IOException;

/**
 * Created on 06.11.2017.
 */
public class Core {

    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            System.out.println("Usage: SpeedCamePrometheusBridge.jar [PATH_TO_PROMETHEUS_CONFIG] (PORT)");
            return;
        }
        String path = args[0];
        int port = 7536;
        if (args.length == 2) {
            port = Integer.parseInt(args[1]);
        }
        new Core(path).start(port);
    }

    private final String configFilePath;

    private Core(String configFilePath) throws Exception {
        if (!new File(configFilePath).exists())
            throw new IOException("Prometheus config file at '" + configFilePath + "' not found!");
        this.configFilePath = configFilePath;

    }

    private void start(int port) {
        System.out.println("Config file of Prometheus: " + configFilePath);

        startRestService(port);

        System.out.println("SpeedCam Prometheus Bridge started!");
    }

    private void startRestService(int port) {
        Spark.port(port);
        registerRestResources();

        System.out.println("REST service started at port " + port);
    }

    private void registerRestResources() {
        Spark.put("/registerClient/:port", (request, response) -> {
            response.status(200);
            return "";
        });
    }
}
