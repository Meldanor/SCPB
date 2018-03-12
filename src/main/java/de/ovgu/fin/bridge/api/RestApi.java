package de.ovgu.fin.bridge.api;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import de.ovgu.fin.bridge.Core;
import de.ovgu.fin.bridge.data.PrometheusClient;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;
import de.ovgu.fin.bridge.prometheus.ConfigurationUpdater;
import de.ovgu.fin.bridge.speedcam.PathServerRequestProxy;
import spark.Route;
import spark.Spark;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.ovgu.fin.bridge.Core.LOGGER;

/**
 * Created on 07.03.2018.
 */
public class RestApi {

    private static final GenericType<Map<String, Map<String, PrometheusClient>>> PROMETHEUS_CLIENT_REQUEST_TYPE =
            new GenericType<Map<String, Map<String, PrometheusClient>>>() {
            };

    private static final GenericType<List<String>> PATH_SERVER_REQUEST_TYPE = new GenericType<List<String>>() {
    };


    private static final String GET_PROMETHEUS_CLIENTS = "/prometheusClient";
    private static final String POST_PROMETHEUS_CLIENTS = "/prometheusClient";

    private static final String GET_PATH_SERVER_REQUESTS = "/pathServerRequests";
    private static final String POST_PATH_SERVER_REQUESTS = "/pathServerRequests";

    private final ConfigurationUpdater configurationUpdater;
    private final PathServerRequestProxy pathServerRequestProxy;

    public RestApi(ConfigurationUpdater configurationUpdater, PathServerRequestProxy pathServerRequestProxy) {
        this.configurationUpdater = configurationUpdater;
        this.pathServerRequestProxy = pathServerRequestProxy;
    }

    public void registerApi(int port) {
        Spark.port(port);
        final Genson genson = Core.createSerializer();

        Spark.get(GET_PROMETHEUS_CLIENTS, getRegisteredClients(genson));
        Spark.post(POST_PROMETHEUS_CLIENTS, registerPrometheusClient(genson));

        Spark.get(GET_PATH_SERVER_REQUESTS, getPathServerRequests(genson));
        Spark.post(POST_PATH_SERVER_REQUESTS, addPathServerRequests(genson));

        // enable possible gzip
        Spark.after(((request, response) -> response.header("Content-Encoding", "gzip")));

        LOGGER.info("REST service started at port " + port);
    }

    private Route registerPrometheusClient(Genson genson) {
        LOGGER.debug("Register POST " + POST_PROMETHEUS_CLIENTS);

        return (request, response) -> {
            // Workaround for POJO binding because of capital REMOVE, UPDATE and CREATE keys
            Map<String, Map<String, PrometheusClient>> rawJson = genson.deserialize(request.bodyAsBytes(), PROMETHEUS_CLIENT_REQUEST_TYPE);

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
            Collection<PrometheusClient> clients = configurationUpdater.getRegisteredPrometheusClients();

            String jsonString = genson.serialize(clients);
            response.body(jsonString);
            response.header("Content-Type", "application/json");
            response.status(200);
            return jsonString;
        };
    }

    // See PathServerRequestProxy.java for an explanation
    private Route getPathServerRequests(Genson genson) {
        LOGGER.debug("Register GET " + GET_PATH_SERVER_REQUESTS);

        return (request, response) -> {
            Set<String> pathRequests = pathServerRequestProxy.getLatestPathRequests();

            String jsonString = genson.serialize(pathRequests);
            response.body(jsonString);
            response.header("Content-Type", "application/json");
            response.status(200);
            return jsonString;
        };
    }

    private Route addPathServerRequests(Genson genson) {
        LOGGER.debug("Register GET " + POST_PATH_SERVER_REQUESTS);

        return (request, response) -> {
            List<String> pathRequests = genson.deserialize(request.bodyAsBytes(), PATH_SERVER_REQUEST_TYPE);
            if (pathRequests == null || pathRequests.isEmpty())
                return "";

            pathServerRequestProxy.addPathRequests(pathRequests);
            return "";
        };
    }
}
