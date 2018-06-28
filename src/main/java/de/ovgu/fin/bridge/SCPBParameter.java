package de.ovgu.fin.bridge;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Created on 28.06.2018.
 */
public class SCPBParameter implements Callable<Void> {

    @CommandLine.Option(names = {"-pc", "--prometheus-config"}, description = "Path to prometheus 2.x configuration file prometheus.yml")
    private File prometheusConfiguration;

    @CommandLine.Option(names = {"-pu", "--prometheus-url"}, description = "URL for the prometheus 2.x web hook to reload configuration")
    private URL prometheusWebUrl;

    @CommandLine.Option(names = {"-p", "--port"}, description = "Port for the SCPB RESTful service to listen to")
    private int scpbPort = 7536;

    @Override
    public Void call() throws Exception {
        if ((prometheusConfiguration != null && prometheusWebUrl == null) ||
                (prometheusConfiguration == null && prometheusWebUrl != null))
            throw new Exception("Both PrometheusConfiguration and PrometheusUrl must be set to be working!");

        if (prometheusConfiguration != null && !prometheusConfiguration.exists())
            throw new IOException("Prometheus configuration does not exist at '" + prometheusConfiguration + "'!");
        return null;
    }

    public File getPrometheusConfiguration() {
        return prometheusConfiguration;
    }

    public URL getPrometheusWebUrl() {
        return prometheusWebUrl;
    }

    int getScpbPort() {
        return scpbPort;
    }

    public boolean withoutPrometheus() {
        return prometheusConfiguration == null && prometheusWebUrl == null;
    }
}
