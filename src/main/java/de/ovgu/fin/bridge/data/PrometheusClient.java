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
    private String brId;
    private String sourceIsdAs;
    @SuppressWarnings("unused")
    private String targetIsdAs;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @SuppressWarnings("unused")
    public String getBrId() {
        return brId;
    }

    public void setBrIdAndSource(String brId) {
        // delete possible leading br info
        brId = brId.replace("br", "");
        // Convert file format to normal format (see https://github.com/scionproto/scion/wiki/ISD-and-AS-numbering for
        // information about it)
        brId = brId.replace("_", ":");
        setBrId(brId);

        // SourceIsdAs is the br id till the second dash '-'
        int i = brId.lastIndexOf('-');
        if (i < 0)
            throw new RuntimeException("BrId '" + brId + "' is invalid! ");

        this.sourceIsdAs = brId.substring(0, i);
    }

    public void setBrId(String brId) {
        this.brId = brId;
    }

    @Override
    public String toString() {
        return "PrometheusClient{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", brId='" + brId + '\'' +
                ", sourceIsdAs='" + sourceIsdAs + '\'' +
                ", targetIsdAs='" + targetIsdAs + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrometheusClient that = (PrometheusClient) o;
        return port == that.port &&
                Objects.equals(ip, that.ip) &&
                Objects.equals(brId, that.brId) &&
                Objects.equals(sourceIsdAs, that.sourceIsdAs) &&
                Objects.equals(targetIsdAs, that.targetIsdAs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, brId, sourceIsdAs, targetIsdAs);
    }
}
