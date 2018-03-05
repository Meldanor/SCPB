package de.ovgu.fin.bridge.data;

public class PrometheusClientInfoYamlConverter {

    public static PrometheusClientInfo deserialize(String yamlString) {
        String[] split = yamlString.split(":");
        return new PrometheusClientInfo(split[0], Integer.parseInt(split[1]));
    }

    public static String serialize(PrometheusClientInfo info) {
        return info.getIp() + ":" + info.getPort();
    }
}
