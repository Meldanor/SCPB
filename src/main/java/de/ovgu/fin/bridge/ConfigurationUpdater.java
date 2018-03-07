package de.ovgu.fin.bridge;

import de.ovgu.fin.bridge.data.PrometheusClientInfo;
import de.ovgu.fin.bridge.data.PrometheusClientInfoYamlConverter;
import org.yaml.snakeyaml.Yaml;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
public class ConfigurationUpdater implements Runnable, Closeable {

    private final Path configFilePath;

    private Set<PrometheusClientInfo> clientInfo;
    private BlockingQueue<PrometheusClientInfo> newClientInfo = new LinkedBlockingQueue<>();
    private PrometheusProcess prometheusProcess;

    ConfigurationUpdater(String configFilePath, PrometheusProcess prometheusProcess) throws IOException {
        this.configFilePath = new File(configFilePath).toPath();
        this.prometheusProcess = prometheusProcess;
        this.clientInfo = new HashSet<>(parseKnownPortNumbers());

        // TreeSet => Sorted output
        LOGGER.info("Monitoring ports: " + clientInfo);
    }

    private List<PrometheusClientInfo> parseKnownPortNumbers() throws IOException {
        Map<String, Object> root = loadConfig();

        return getTargetList(root)
                .stream()
                .map(PrometheusClientInfoYamlConverter::deserialize)
                .collect(Collectors.toList());
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
        int size = newClientInfo.size();
        if (size <= 0)
            return;

        List<PrometheusClientInfo> copy = new ArrayList<>(size);
        newClientInfo.drainTo(copy);
        updateConfigurationFile(copy);
    }

    boolean registerPrometheusClient(PrometheusClientInfo clientInfo) {
        // Port number is already known
        if (!this.clientInfo.add(clientInfo))
            return false;

        newClientInfo.add(clientInfo);
        return true;
    }

    private void updateConfigurationFile(List<PrometheusClientInfo> copyInfo) {
        LOGGER.info("Adding prometheus client info: " + copyInfo);
        try {
            Map<String, Object> root = loadConfig();
            List<String> targetList = getTargetList(root);
            copyInfo.stream()
                    .map(PrometheusClientInfoYamlConverter::serialize)
                    .forEach(targetList::add);
            Yaml yaml = new Yaml();
            try (Writer writer = Files.newBufferedWriter(configFilePath)) {
                yaml.dump(root, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Can't write prometheus client info: " + copyInfo, e);
        }
        LOGGER.info("Reloading prometheus configuration...");
        try {
            prometheusProcess.reload();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Can't stop prometheus: " + copyInfo, e);
        }
        LOGGER.info("Prometheus configuration reloaded!");
    }

    @Override
    public void close() {
        if (!newClientInfo.isEmpty()) {
            LOGGER.info("Flush memory...!");
            updateConfigurationFile(new ArrayList<>(newClientInfo));
        }
    }
}
