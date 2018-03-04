package de.ovgu.fin.bridge.data;

import java.util.Map;

public class RegisterPrometheusRequest {

    private Map<String, PrometheusClientInfo> create;
    private Map<String, PrometheusClientInfo> update;
    private Map<String, PrometheusClientInfo> remove;

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
