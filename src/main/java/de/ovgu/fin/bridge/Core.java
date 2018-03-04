package de.ovgu.fin.bridge;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.reflect.VisibilityFilter;
import de.ovgu.fin.bridge.data.PrometheusClientInfo;
import de.ovgu.fin.bridge.data.RegisterPrometheusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 06.11.2017.
 */
public class Core {

    static final Logger LOGGER = LoggerFactory.getLogger("SCPB");
    private static final GenericType<Map<String, Map<String, PrometheusClientInfo>>> REQUEST_TYPE =
            new GenericType<Map<String, Map<String, PrometheusClientInfo>>>() {
            };


    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            LOGGER.error("Usage: SpeedCamePrometheusBridge.jar [PATH_TO_PROMETHEUS_DIR] (PORT) (RETENTION_TIME)");
            return;
        }
        String path = args[0];
        int port = 7536;
        Duration retentionTime = Duration.ofDays(90);
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            retentionTime = Duration.parse("P" + args[3]);
        }

        LOGGER.info("Starting SCPB!");

        new Core(path, retentionTime).start(port);
    }

    private ConfigurationUpdater configurationUpdater;
    private PrometheusProcess prometheusProcess;
    private PrometheusHeartbeatCheck prometheusHeartbeatCheck;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private Core(String prometheusDir, Duration retentionTime) throws Exception {
        String configFilePath = new File(prometheusDir, "prometheus.yml").getPath();
        if (!new File(configFilePath).exists()) {
            throw new IOException("Prometheus config file at '" + configFilePath + "' not found!");
        }

        LOGGER.info("Config file of Prometheus: " + configFilePath);
        LOGGER.info("Storage retention time: " + retentionTime.getSeconds() + "s");
        this.prometheusProcess = new PrometheusProcess(prometheusDir, retentionTime);
        this.prometheusHeartbeatCheck = new PrometheusHeartbeatCheck(prometheusProcess);
        this.configurationUpdater = new ConfigurationUpdater(configFilePath, prometheusProcess);

    }

    private void start(int port) throws Exception {

        startUpdaterThread();
        startPrometheus();
        startRestService(port);

        // Write current port numbers at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configurationUpdater.close();
                prometheusProcess.stop();
                scheduler.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Shutdown-thread"));

        LOGGER.info("SpeedCam Prometheus Bridge started!");
    }

    private void startPrometheus() throws Exception {
        LOGGER.info("Starting prometheus ...");
        this.prometheusProcess.start();
        LOGGER.info("Prometheus started!");

        scheduler.scheduleAtFixedRate(this.prometheusHeartbeatCheck, TimeUnit.MINUTES.toSeconds(1L), 5, TimeUnit.SECONDS);
        LOGGER.info("Prometheus heartbeat checker started!");
    }

    private void startUpdaterThread() {
        scheduler.scheduleAtFixedRate(this.configurationUpdater, 0, 5, TimeUnit.SECONDS);
        LOGGER.info("Started configuration update scheduler with interval 5s");
    }

    private void startRestService(int port) {
        Spark.port(port);
        registerRestResources();

        LOGGER.info("REST service started at port " + port);
    }

    @SuppressWarnings("unchecked")
    private void registerRestResources() {


        final Genson genson = new GensonBuilder()
                .useFields(true)
                .setFieldFilter(VisibilityFilter.ALL)
                .create();

        Spark.post("/registerPrometheusClient/", (request, response) -> {

            // Workaround for POJO binding because of capital REMOVE, UPDATE and CREATE keys
            Map<String, Map<String, PrometheusClientInfo>> rawJson = genson.deserialize(request.bodyAsBytes(), REQUEST_TYPE);

            RegisterPrometheusRequest registerRequest = new RegisterPrometheusRequest(rawJson);

            // TODO: Also save the Name of the border router
            for (Map.Entry<String, PrometheusClientInfo> entry : registerRequest.getCreateRequests().entrySet()) {
                configurationUpdater.registerPrometheusClient(entry.getValue());
            }

            System.out.println(registerRequest);
            response.status(200);
            return "";
        });
    }
}
