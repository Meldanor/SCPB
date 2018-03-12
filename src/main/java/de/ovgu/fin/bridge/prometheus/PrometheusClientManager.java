package de.ovgu.fin.bridge.prometheus;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import de.ovgu.fin.bridge.Core;
import de.ovgu.fin.bridge.data.PrometheusClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created on 12.03.2018.
 */
public class PrometheusClientManager {

    private static final GenericType<Set<PrometheusClient>> LIST_INFO_TYPE = new GenericType<Set<PrometheusClient>>() {
    };

    private final File clientFile;
    private final Genson genson;

    private Set<PrometheusClient> registeredClients;

    PrometheusClientManager() throws IOException {
        this.genson = Core.createSerializer();
        this.clientFile = new File("clients.json");

        this.registeredClients = loadRegisteredClients();
    }

    private Set<PrometheusClient> loadRegisteredClients() throws IOException {
        if (!clientFile.exists()) {
            Files.write(clientFile.toPath(), Collections.singleton("[]"));
            return new HashSet<>();
        }
        try (InputStream inputStream = Files.newInputStream(clientFile.toPath())) {
            return genson.deserialize(inputStream, LIST_INFO_TYPE);
        }
    }

    public void registerClients(Collection<PrometheusClient> clients) {
        registeredClients.addAll(clients);
        updateFile();
    }

    public void removeClients(Collection<PrometheusClient> clients) {
        registeredClients.removeAll(clients);
        updateFile();
    }

    private void updateFile() {
        try {
            saveRegisteredClients(registeredClients);
        } catch (IOException e) {
            Core.LOGGER.error("Write clients: ", e);
        }
    }

    private void saveRegisteredClients(Set<PrometheusClient> clientInfo) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(clientFile.toPath())) {
            genson.serialize(clientInfo, outputStream);
        }
    }

    public Set<PrometheusClient> getRegisteredClients() {
        return Collections.unmodifiableSet(registeredClients);
    }

}
