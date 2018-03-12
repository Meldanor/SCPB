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
public class PrometheusConfig {

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
        Map<String, Object> scrape_configs = (Map<String, Object>) ((List) root.get("scrape_configs")).get(0);
        Map<String, Object> static_configs = (Map<String, Object>) ((List) (scrape_configs.get("static_configs"))).get(0);
        return (List<String>) static_configs.get("targets");
    }

    public void writeTargets(Collection<PrometheusClient> registeredClients) {
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
