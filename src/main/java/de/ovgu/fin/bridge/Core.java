package de.ovgu.fin.bridge;

import java.io.File;
import java.io.IOException;

/**
 * Created on 06.11.2017.
 */
public class Core {

    public static void main(String[] args) throws Exception {
        if (args.length <= 0)
            System.out.println("Usage: SpeedCamePrometheusBridge.jar [PATH_TO_PROMETHEUS_CONFIG]");

        String path = args[0];
        new Core(path).start();
    }

    private final String configFilePath;

    private Core(String configFilePath) throws Exception {
        if (!new File(configFilePath).exists())
            throw new IOException("Prometheus config file at '" + configFilePath + "' not found!");
        this.configFilePath = configFilePath;

    }

    private void start() {
        System.out.println("Config file of Prometheus: " + configFilePath);
        System.out.println("SpeedCam Prometheus Bridge started!");
    }

}
