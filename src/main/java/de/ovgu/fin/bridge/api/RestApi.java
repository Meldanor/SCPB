package de.ovgu.fin.bridge.api;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.reflect.VisibilityFilter;
import de.ovgu.fin.bridge.data.PrometheusClientInfo;
import de.ovgu.fin.bridge.data.PrometheusClientInfoYamlConverter;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;
import de.ovgu.fin.bridge.prometheus.ConfigurationUpdater;
import spark.Route;
import spark.Spark;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 07.03.2018.
 */
public class RestApi {

    private static final GenericType<Map<String, Map<String, PrometheusClientInfo>>> REQUEST_TYPE =
            new GenericType<Map<String, Map<String, PrometheusClientInfo>>>() {
            };
    private static final String GET_PROMETHEUS_CLIENTS = "/prometheusClient";
    private static final String POST_PROMETHEUS_CLIENTS = "/prometheusClient";

    private final ConfigurationUpdater configurationUpdater;

    public RestApi(ConfigurationUpdater configurationUpdater) {
        this.configurationUpdater = configurationUpdater;
    }

    public void registerApi(int port) {
        Spark.port(port);
        final Genson genson = new GensonBuilder()
                .useFields(true)
                .setFieldFilter(VisibilityFilter.ALL)
                .create();

        Spark.post(POST_PROMETHEUS_CLIENTS, registerPrometheusClient(genson));
        Spark.get(GET_PROMETHEUS_CLIENTS, getRegisteredClients(genson));

        LOGGER.info("REST service started at port " + port);
    }

    private Route registerPrometheusClient(Genson genson) {
        LOGGER.debug("Register POST " + POST_PROMETHEUS_CLIENTS);
        return (request, response) -> {

            // Workaround for POJO binding because of capital REMOVE, UPDATE and CREATE keys
            Map<String, Map<String, PrometheusClientInfo>> rawJson = genson.deserialize(request.bodyAsBytes(), REQUEST_TYPE);

            if (rawJson == null || rawJson.isEmpty()) {
                response.status(400);
                return "Malformed JSON";
            }

            RegisterPrometheusRequest registerRequest = new RegisterPrometheusRequest(rawJson);
            configurationUpdater.registerPrometheusClient(registerRequest);
            response.status(200);
            return "";
        };
    }

    private Route getRegisteredClients(Genson genson) {
        LOGGER.debug("Register GET " + GET_PROMETHEUS_CLIENTS);

        return (request, response) -> {

            // Transform YAML to JSON
            List<PrometheusClientInfo> clients = configurationUpdater.getRegisteredClients()
                    .stream()
                    .map(PrometheusClientInfoYamlConverter::deserialize)
                    .collect(Collectors.toList());

            String jsonString = genson.serialize(clients);
            response.body(jsonString);
            response.header("Content-Type", "application/json");
            response.status(200);
            return jsonString;
        };
    }
}
