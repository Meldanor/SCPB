package de.ovgu.fin.bridge;

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
    private final String as17ipv4;

    private Set<Integer> portNumbers;
    private BlockingQueue<Integer> newPortNumbers = new LinkedBlockingQueue<>();
    private PrometheusProcess prometheusProcess;

    ConfigurationUpdater(String configFilePath, String as17ipv4, PrometheusProcess prometheusProcess) throws IOException {
        this.configFilePath = new File(configFilePath).toPath();
        this.as17ipv4 = as17ipv4;
        this.prometheusProcess = prometheusProcess;
        this.portNumbers = new HashSet<>(parseKnownPortNumbers());

        // TreeSet => Sorted output
        LOGGER.info("Monitoring ports: " + new TreeSet<>(portNumbers));
    }

    private List<Integer> parseKnownPortNumbers() throws IOException {

        return Files.lines(configFilePath)
                .filter(s -> s.contains("- targets:"))
                .map(this::parseKnownPortNumbers)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<Integer> parseKnownPortNumbers(String yamlConfigEntry) {
        Pattern portPattern = Pattern.compile("(\\d+)'");
        Matcher matcher = portPattern.matcher(yamlConfigEntry);
        List<Integer> ports = new ArrayList<>();
        while (matcher.find()) {
            String portMatch = matcher.group(1);
            int port = Integer.parseInt(portMatch);
            ports.add(port);
        }

        return ports;
    }

    @Override
    public void run() {
        int size = newPortNumbers.size();
        if (size <= 0)
            return;

        List<Integer> copyPortNumbers = new ArrayList<>(size);
        newPortNumbers.drainTo(copyPortNumbers);
        updateConfigurationFile(copyPortNumbers);
    }

    boolean registerNewPortNumber(int portNumber) {
        // Port number is already known
        if (!portNumbers.add(portNumber))
            return false;

        newPortNumbers.add(portNumber);
        return true;
    }

    private void updateConfigurationFile(List<Integer> copyPortNumbers) {
        LOGGER.info("Stopping prometheus...");
        try {
            prometheusProcess.stop();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Can't stop prometheus: " + copyPortNumbers, e);
        }
        LOGGER.info("Adding port numbers: " + copyPortNumbers);
        try (BufferedWriter writer = Files.newBufferedWriter(configFilePath, StandardOpenOption.APPEND)) {
            for (Integer portNumber : copyPortNumbers) {
                writer.newLine();
                writer.append(YAML_PREFIX)
                        .append("- targets: ['")
                        .append(as17ipv4)
                        .append(":")
                        .append(Integer.toString(portNumber))
                        .append("']");
            }

        } catch (Exception e) {
            LOGGER.error("Can't write port numbers: " + copyPortNumbers);
            e.printStackTrace();
        }
        LOGGER.info("Restarting prometheus...");
        try {
            prometheusProcess.start();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Can't stop prometheus: " + copyPortNumbers, e);
        }
        LOGGER.info("Prometheus restarted!");
    }

    @Override
    public void close() throws IOException {
        if (!newPortNumbers.isEmpty()) {
            LOGGER.info("Flush memory...!");
            updateConfigurationFile(new ArrayList<>(newPortNumbers));
        }
    }
}
