package de.ovgu.fin.bridge;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created on 06.11.2017.
 */
public class ConfigurationUpdater implements Runnable, Closeable {

    private static final String YAML_PREFIX = "      ";

    private final Path configFilePath;
    private final String as17ipv4;

    private Set<Integer> portNumbers = new HashSet<>();
    private BlockingQueue<Integer> newPortNumbers = new LinkedBlockingQueue<>();

    ConfigurationUpdater(String configFilePath, String as17ipv4) {
        this.configFilePath = new File(configFilePath).toPath();
        this.as17ipv4 = as17ipv4;
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
        System.out.println("Adding port numbers: " + copyPortNumbers);
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
            System.err.println("Can't write port numbers: " + copyPortNumbers);
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        updateConfigurationFile(new ArrayList<>(newPortNumbers));
    }
}
