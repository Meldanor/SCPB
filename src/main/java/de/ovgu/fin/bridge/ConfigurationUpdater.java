package de.ovgu.fin.bridge;

import de.ovgu.fin.bridge.data.PrometheusClientInfo;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 06.11.2017.
 */
public class ConfigurationUpdater implements Runnable, Closeable {

    private static final String YAML_PREFIX = "      ";

    private final Path configFilePath;

    private Set<PrometheusClientInfo> clientInfo;
    private BlockingQueue<PrometheusClientInfo> newClientInfo = new LinkedBlockingQueue<>();
    private PrometheusProcess prometheusProcess;

    ConfigurationUpdater(String configFilePath, PrometheusProcess prometheusProcess) throws IOException {
        this.configFilePath = new File(configFilePath).toPath();
        this.prometheusProcess = prometheusProcess;
        this.clientInfo = new HashSet<>(parseKnownPortNumbers());

        // TreeSet => Sorted output
        LOGGER.info("Monitoring ports: " + new TreeSet<>(clientInfo));
    }

    private List<PrometheusClientInfo> parseKnownPortNumbers() throws IOException {

        return Files.lines(configFilePath)
                .filter(s -> s.contains("- targets:"))
                .map(this::parseKnownPortNumbers)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<PrometheusClientInfo> parseKnownPortNumbers(String yamlConfigEntry) {
        // Pattern = Extract IP address and port number
        Pattern ipPortPattern = Pattern.compile("((?:(?:2(?:[0-4][0-9]|5[0-5])|[0-1]?[0-9]?[0-9])\\.){3}(?:2(?:[0-4][0-9]|5[0-5])|[0-1]?[0-9]?[0-9])):(\\d+)");
        Matcher matcher = ipPortPattern.matcher(yamlConfigEntry);
        List<PrometheusClientInfo> ports = new ArrayList<>();
        while (matcher.find()) {
            String ipGroup = matcher.group(1);
            String portGroup = matcher.group(2);
            int port = Integer.parseInt(portGroup);
            ports.add(new PrometheusClientInfo(ipGroup, port));
        }

        return ports;
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
        try (BufferedWriter writer = Files.newBufferedWriter(configFilePath, StandardOpenOption.APPEND)) {
            for (PrometheusClientInfo info : copyInfo) {
                writer.newLine();
                writer.append(YAML_PREFIX)
                        .append("- targets: ['")
                        .append(info.getIp())
                        .append(":")
                        .append(Integer.toString(info.getPort()))
                        .append("']");
            }

        } catch (Exception e) {
            LOGGER.error("Can't write prometheus client info: " + copyInfo);
            e.printStackTrace();
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
