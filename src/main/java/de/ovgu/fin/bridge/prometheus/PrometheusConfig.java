package de.ovgu.fin.bridge.prometheus;

import de.ovgu.fin.bridge.data.PrometheusClient;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 12.03.2018.
 */
class PrometheusConfig {

    private final Path configFilePath;

    PrometheusConfig(Path configFilePath) {
        this.configFilePath = configFilePath;
    }

    private Map<String, Object> loadConfig() throws IOException {
        Yaml yaml = new Yaml();
        return yaml.load(Files.newInputStream(configFilePath));
    }

    @SuppressWarnings("unchecked")
    private List<String> getTargetList(Map<String, Object> root) {

        // Traverse the configuration structure and check if the element is there
        List scrape_configs1 = (List) root.get("scrape_configs");
        if (scrape_configs1 == null)
            throw new RuntimeException("Invalid prometheus.yml file! Missing 'scrape_configs'");
        Map<String, Object> scrape_configs = (Map<String, Object>) scrape_configs1.get(0);
        if (scrape_configs == null)
            throw new RuntimeException("Invalid prometheus.yml file! Missing 'scrape_configs'");
        List staticConfigs = (List) (scrape_configs.get("static_configs"));
        if (staticConfigs == null)
            throw new RuntimeException("Invalid prometheus.yml file! Missing 'static_configs'");
        Map<String, Object> static_configs = (Map<String, Object>) staticConfigs.get(0);
        if (static_configs == null)
            throw new RuntimeException("Invalid prometheus.yml file! Missing 'static_configs'");
        List<String> targets = (List<String>) static_configs.get("targets");
        if (targets == null)
            throw new RuntimeException("Invalid prometheus.yml file! Missing 'targets'");
        return targets;
    }

    void writeTargets(Collection<PrometheusClient> registeredClients) {
        Map<String, Object> configRoot;
        try {
            configRoot = loadConfig();
        } catch (IOException e) {
            LOGGER.error("Can't read prometheus config file!", e);
            return;
        }

        List<String> targetList = getTargetList(configRoot);
        targetList.clear();

        targetList.addAll(
                registeredClients.stream()
                        .map(this::toYamlString)
                        .collect(Collectors.toList())
        );

        LOGGER.info("Update prometheus config!");
        // Serialize the new target list
        try (Writer writer = Files.newBufferedWriter(configFilePath)) {
            Yaml yaml = new Yaml();
            yaml.dump(configRoot, writer);
        } catch (IOException e) {
            LOGGER.error("Can't write prometheus client info: " + registeredClients, e);
        }
    }

    private String toYamlString(PrometheusClient info) {
        return info.getIp() + ":" + info.getPort();
    }
}
