package de.ovgu.fin.bridge.prometheus;

import de.ovgu.fin.bridge.data.PrometheusClient;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
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

    private final PrometheusConfig prometheusConfig;
    private final PrometheusClientManager prometheusClientManager;

    private BlockingQueue<RegisterPrometheusRequest> newRequests = new LinkedBlockingQueue<>();
    private PrometheusProcess prometheusProcess;

    public ConfigurationUpdater(String configFilePath, PrometheusProcess prometheusProcess) throws IOException {
        this.prometheusConfig = new PrometheusConfig(new File(configFilePath).toPath());
        this.prometheusClientManager = new PrometheusClientManager();
        this.prometheusProcess = prometheusProcess;

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

        prometheusConfig.writeTargets(prometheusClientManager.getRegisteredClients());

        LOGGER.info("Reloading prometheus configuration...");
        try {
            prometheusProcess.reload();
        } catch (Exception e) {
            LOGGER.error("Can't reload prometheus configuration: " + requests, e);
        }
        LOGGER.info("Prometheus configuration updated and reloaded!");
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
}
