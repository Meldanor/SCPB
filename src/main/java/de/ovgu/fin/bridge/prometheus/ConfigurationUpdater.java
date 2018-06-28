package de.ovgu.fin.bridge.prometheus;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.ovgu.fin.bridge.SCPBParameter;
import de.ovgu.fin.bridge.data.PrometheusClient;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
public class ConfigurationUpdater implements Runnable, Closeable {

    private PrometheusConfig prometheusConfig;
    private URL prometheusUrl;
    private final PrometheusClientManager prometheusClientManager;

    private final boolean withoutPrometheus;

    private BlockingQueue<RegisterPrometheusRequest> newRequests = new LinkedBlockingQueue<>();

    public ConfigurationUpdater(SCPBParameter parameter) throws IOException {
        this.withoutPrometheus = parameter.withoutPrometheus();
        if (!withoutPrometheus) {
            this.prometheusConfig = new PrometheusConfig(parameter.getPrometheusConfiguration().toPath());
            this.prometheusUrl = parameter.getPrometheusWebUrl();
        }
        this.prometheusClientManager = new PrometheusClientManager();

        LOGGER.info("Monitoring prometheus clients: " + prometheusClientManager.getRegisteredClients());
    }

    @Override
    public void run() {
        int size = newRequests.size();
        if (size <= 0)
            return;

        List<RegisterPrometheusRequest> copy = new ArrayList<>(size);
        newRequests.drainTo(copy);
        updateConfigurationFile(copy);
    }

    public void registerPrometheusClient(RegisterPrometheusRequest request) {
        newRequests.add(request);
    }

    public Collection<PrometheusClient> getRegisteredPrometheusClients() {
        return prometheusClientManager.getRegisteredClients();
    }

    private void updateConfigurationFile(List<RegisterPrometheusRequest> requests) {
        List<PrometheusClient> toRemoveClients = toRemoveClients(requests);
        if (!toRemoveClients.isEmpty()) {
            LOGGER.info("Remove prometheus clients: " + toRemoveClients);
            prometheusClientManager.removeClients(toRemoveClients);
        }

        List<PrometheusClient> toAddClients = toAddClients(requests);
        if (!toAddClients.isEmpty()) {
            LOGGER.info("Add prometheus clients: " + toAddClients);
            prometheusClientManager.registerClients(toAddClients);
        }

        if (!withoutPrometheus) {
            prometheusConfig.writeTargets(prometheusClientManager.getRegisteredClients());
            LOGGER.info("Reloading prometheus configuration...");
            try {
                reloadPrometheus();
            } catch (Exception e) {
                LOGGER.error("Can't reload prometheus configuration: " + requests, e);
            }
            LOGGER.info("Prometheus configuration updated and reloaded!");
        }
    }

    private List<PrometheusClient> toRemoveClients(List<RegisterPrometheusRequest> requests) {
        return requests.stream()
                .map(RegisterPrometheusRequest::getRemoveRequests)
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    private List<PrometheusClient> toAddClients(List<RegisterPrometheusRequest> requests) {
        return requests.stream()
                .map(RegisterPrometheusRequest::getCreateRequests)
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        if (!newRequests.isEmpty()) {
            LOGGER.info("Flush memory...!");
            updateConfigurationFile(new ArrayList<>(newRequests));
        }
    }

    private static final String PROMETHEUS_RELOAD_URL = "/-/reload";

    private void reloadPrometheus() throws IOException {
        URL url = new URL(prometheusUrl, PROMETHEUS_RELOAD_URL);

        try {
            HttpResponse<String> status = Unirest.post(url.toString()).asString();
            if (status.getStatus() != 200)
                throw new IOException("Status was not 200, but " + status.getStatus() + ". " +
                        "Message: " + status.getStatusText() + ". Body: " + status.getBody());
        } catch (UnirestException e) {
            throw new IOException(e);
        }
    }
}
