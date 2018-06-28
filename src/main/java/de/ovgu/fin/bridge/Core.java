package de.ovgu.fin.bridge;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.reflect.VisibilityFilter;
import de.ovgu.fin.bridge.api.RestApi;
import de.ovgu.fin.bridge.prometheus.ConfigurationUpdater;
import de.ovgu.fin.bridge.speedcam.PathServerRequestProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 06.11.2017.
 */
public class Core {

    public static final Logger LOGGER = LoggerFactory.getLogger("SCPB");

    public static void main(String[] args) throws Exception {
        SCPBParameter parameter = new SCPBParameter();
        CommandLine.call(parameter, args);

        LOGGER.info("Starting SCPB!");

        new Core(parameter).start(parameter);
    }

    private ConfigurationUpdater configurationUpdater;
    private PathServerRequestProxy pathServerRequestProxy;

    private final ScheduledExecutorService scheduler;

    private Core(SCPBParameter parameter) throws Exception {
        scheduler = Executors.newScheduledThreadPool(2);
        this.configurationUpdater = new ConfigurationUpdater(parameter);
        this.pathServerRequestProxy = new PathServerRequestProxy();
    }

    private void start(SCPBParameter parameter) {
        startUpdaterThread();

        // Write current port numbers at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configurationUpdater.close();
                scheduler.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Shutdown-thread"));
        new RestApi(configurationUpdater, pathServerRequestProxy).registerApi(parameter.getScpbPort());

        LOGGER.info("SpeedCam Prometheus Bridge started!");
    }

    private void startUpdaterThread() {
        scheduler.scheduleAtFixedRate(runnableWithException(this.configurationUpdater), 0, 5, TimeUnit.SECONDS);
        LOGGER.info("Started configuration update scheduler with interval 5s");
    }

    /**
     * This methods encapsulates a runnable with a try-catch, because scheduled executor service suppresses
     * exceptions and errors. Without this method it is possible to miss errors.
     *
     * @param runnable The runnable to encapsulates.
     * @return The same runnable with a try-catch around.
     */
    private Runnable runnableWithException(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                LOGGER.error("Thread error!", e);
            }
        };
    }

    public static Genson createSerializer() {
        return new GensonBuilder()
                .useFields(true)
                .setFieldFilter(VisibilityFilter.ALL)
                .create();
    }
}
