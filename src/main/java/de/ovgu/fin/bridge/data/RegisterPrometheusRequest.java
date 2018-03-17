package de.ovgu.fin.bridge.data;

import java.util.Collections;
import java.util.Map;

public class RegisterPrometheusRequest {

    private Map<String, PrometheusClient> create = Collections.emptyMap();
    private Map<String, PrometheusClient> update = Collections.emptyMap();
    private Map<String, PrometheusClient> remove = Collections.emptyMap();

    public RegisterPrometheusRequest(Map<String, Map<String, PrometheusClient>> rawJsonObject) {
        for (Map.Entry<String, Map<String, PrometheusClient>> entry : rawJsonObject.entrySet()) {
            String key = entry.getKey().toLowerCase();
            switch (key) {
                case "create":
                    this.create = entry.getValue();
                    break;
                case "update":
                    this.update = entry.getValue();
                    break;
                case "remove":
                    this.remove = entry.getValue();
                    break;
            }

            for (Map.Entry<String, PrometheusClient> clientEntry : entry.getValue().entrySet()) {
                clientEntry.getValue().setBrIdAndSource(clientEntry.getKey());
            }
        }
    }

    public Map<String, PrometheusClient> getCreateRequests() {
        return create;
    }

    // TODO: Need to be implemented - if it is useful
    @SuppressWarnings("unused")
    public Map<String, PrometheusClient> getUpdateRequests() {
        return update;
    }

    public Map<String, PrometheusClient> getRemoveRequests() {
        return remove;
    }

    @Override
    public String toString() {
        return "RegisterPrometheusRequest{" +
                "create=" + create +
                ", update=" + update +
                ", remove=" + remove +
                '}';
    }
}
