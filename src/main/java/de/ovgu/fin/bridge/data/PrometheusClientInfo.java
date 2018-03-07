package de.ovgu.fin.bridge.data;

import java.util.Objects;

public class PrometheusClientInfo {

    // Used for serialization process
    @SuppressWarnings("unused")
    public PrometheusClientInfo() {
    }

    PrometheusClientInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    private String ip;
    private int port;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "PrometheusClientInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrometheusClientInfo that = (PrometheusClientInfo) o;
        return port == that.port &&
                Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}