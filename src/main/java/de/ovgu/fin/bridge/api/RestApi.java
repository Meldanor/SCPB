package de.ovgu.fin.bridge.api;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.reflect.VisibilityFilter;
import de.ovgu.fin.bridge.data.PrometheusClientInfo;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;
import de.ovgu.fin.bridge.prometheus.ConfigurationUpdater;
import spark.Route;
import spark.Spark;

import java.util.Map;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 07.03.2018.
 */
public class RestApi {

    private static final GenericType<Map<String, Map<String, PrometheusClientInfo>>> REQUEST_TYPE =
            new GenericType<Map<String, Map<String, PrometheusClientInfo>>>() {
            };
    private static final String REGISTER_PROMETHEUS_CLIENT_POST = "/registerPrometheusClient/";

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

        Spark.post(REGISTER_PROMETHEUS_CLIENT_POST, registerPrometheusClient(genson));

        LOGGER.info("REST service started at port " + port);
    }

    private Route registerPrometheusClient(Genson genson) {
        LOGGER.debug("Register POST " + REGISTER_PROMETHEUS_CLIENT_POST);
        return (request, response) -> {

            // Workaround for POJO binding because of capital REMOVE, UPDATE and CREATE keys
            Map<String, Map<String, PrometheusClientInfo>> rawJson = genson.deserialize(request.bodyAsBytes(), REQUEST_TYPE);

            RegisterPrometheusRequest registerRequest = new RegisterPrometheusRequest(rawJson);
            configurationUpdater.registerPrometheusClient(registerRequest);
            response.status(200);
            return "";
        };
    }
}
