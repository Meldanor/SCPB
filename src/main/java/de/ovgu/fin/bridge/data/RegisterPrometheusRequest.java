package de.ovgu.fin.bridge.data;

import java.util.Collections;
import java.util.Map;

public class RegisterPrometheusRequest {

    private Map<String, PrometheusClientInfo> create = Collections.emptyMap();
    private Map<String, PrometheusClientInfo> update = Collections.emptyMap();
    private Map<String, PrometheusClientInfo> remove = Collections.emptyMap();

    public RegisterPrometheusRequest(Map<String, Map<String, PrometheusClientInfo>> rawJsonObject) {
        for (Map.Entry<String, Map<String, PrometheusClientInfo>> entry : rawJsonObject.entrySet()) {
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
        }
    }

    public Map<String, PrometheusClientInfo> getCreateRequests() {
        return create;
    }

    // TODO: Need to be implemented - if it is useful
    @SuppressWarnings("unused")
    public Map<String, PrometheusClientInfo> getUpdateRequests() {
        return update;
    }

    public Map<String, PrometheusClientInfo> getRemoveRequests() {
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
