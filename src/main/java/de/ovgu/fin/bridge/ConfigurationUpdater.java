package de.ovgu.fin.bridge;

import de.ovgu.fin.bridge.data.PrometheusClientInfo;
import de.ovgu.fin.bridge.data.PrometheusClientInfoYamlConverter;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;
import org.yaml.snakeyaml.Yaml;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
public class ConfigurationUpdater implements Runnable, Closeable {

    private final Path configFilePath;

    private BlockingQueue<RegisterPrometheusRequest> newRequests = new LinkedBlockingQueue<>();
    private PrometheusProcess prometheusProcess;

    ConfigurationUpdater(String configFilePath, PrometheusProcess prometheusProcess) throws IOException {
        this.configFilePath = new File(configFilePath).toPath();
        this.prometheusProcess = prometheusProcess;

        LOGGER.info("Monitoring prometheus clients: " + loadMonitoringClients());
    }

    private List<String> loadMonitoringClients() throws IOException {
        return getTargetList(loadConfig());
    }

    private Map<String, Object> loadConfig() throws IOException {
        Yaml yaml = new Yaml();
        return yaml.load(Files.newInputStream(configFilePath));
    }

    @SuppressWarnings("unchecked")
    private List<String> getTargetList(Map<String, Object> root) {
        Map<String, Object> scrape_configs = (Map<String, Object>) ((List) root.get("scrape_configs")).get(0);
        Map<String, Object> static_configs = (Map<String, Object>) ((List) (scrape_configs.get("static_configs"))).get(0);
        return (List<String>) static_configs.get("targets");
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

    void registerPrometheusClient(RegisterPrometheusRequest request) {
        newRequests.add(request);
    }

    private void updateConfigurationFile(List<RegisterPrometheusRequest> requests) {
        Map<String, Object> configRoot;
        try {
            configRoot = loadConfig();
        } catch (IOException e) {
            LOGGER.error("Can't read prometheus config file!", e);
            return;
        }

        Set<PrometheusClientInfo> registeredClients = getTargetList(configRoot).stream()
                .map(PrometheusClientInfoYamlConverter::deserialize)
                .collect(Collectors.toSet());

        List<PrometheusClientInfo> toRemoveClients = toRemoveClients(requests);
        if (!toRemoveClients.isEmpty()) {
            LOGGER.info("Remove prometheus clients: " + toRemoveClients);

            boolean hasRemoved = registeredClients.removeAll(toRemoveClients);
            if (!hasRemoved) {
                LOGGER.warn("Tried to remove prometheus clients, but none of them was existing!");
            }
        }

        List<PrometheusClientInfo> toAddClients = toAddClients(requests);
        if (!toAddClients.isEmpty()) {
            LOGGER.info("Add prometheus clients: " + toAddClients);
            registeredClients.addAll(toAddClients);
        }

        // Clear targetList because it has a reference to the root object and then add all registered clients to this
        List<String> targetList = getTargetList(configRoot);
        targetList.clear();
        registeredClients.stream()
                .map(PrometheusClientInfoYamlConverter::serialize)
                .forEach(targetList::add);

        LOGGER.info("Update prometheus config!");
        // Serialize the new target list
        try (Writer writer = Files.newBufferedWriter(configFilePath)) {
            Yaml yaml = new Yaml();
            yaml.dump(configRoot, writer);
        } catch (IOException e) {
            LOGGER.error("Can't write prometheus client info: " + registeredClients, e);
        }
        LOGGER.info("Reloading prometheus configuration...");
        try {
            prometheusProcess.reload();
        } catch (Exception e) {
            LOGGER.error("Can't reload prometheus configuration: " + requests, e);
        }
        LOGGER.info("Prometheus configuration updated and reloaded!");
    }

    private List<PrometheusClientInfo> toRemoveClients(List<RegisterPrometheusRequest> requests) {
        return requests.stream()
                .map(RegisterPrometheusRequest::getRemoveRequests)
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    private List<PrometheusClientInfo> toAddClients(List<RegisterPrometheusRequest> requests) {
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
