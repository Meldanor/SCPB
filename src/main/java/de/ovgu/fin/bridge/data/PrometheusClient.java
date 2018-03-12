package de.ovgu.fin.bridge.data;

import java.util.Objects;

public class PrometheusClient {

    // Used for serialization process
    @SuppressWarnings("unused")
    public PrometheusClient() {
    }

    @SuppressWarnings("unused")
    private String ip;

    @SuppressWarnings("unused")
    private int port;

    private String brInfo;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @SuppressWarnings("unused")
    public String getBrInfo() {
        return brInfo;
    }

    public void setBrInfo(String brInfo) {
        this.brInfo = brInfo;
    }

    @Override
    public String toString() {
        return "PrometheusClient{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", brInfo='" + brInfo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrometheusClient that = (PrometheusClient) o;
        return port == that.port &&
                Objects.equals(ip, that.ip) &&
                Objects.equals(brInfo, that.brInfo);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ip, port, brInfo);
    }
}
