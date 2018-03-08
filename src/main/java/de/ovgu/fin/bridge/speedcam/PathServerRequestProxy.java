package de.ovgu.fin.bridge.speedcam;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class only for the speedCam test environment. Holds raw path requests, which are sent to SCPB to serve them to
 * a local client.
 * SCPB implements does this instead of the speedCam client is to multiplex an open port and keep the implementation
 * of speedCam more simple. Would need instead an own web server or other API.
 */
public class PathServerRequestProxy {

    private BlockingQueue<String> rawPathRequests;

    public PathServerRequestProxy() {
        this.rawPathRequests = new LinkedBlockingQueue<>();
    }

    public void addPathRequests(List<String> toAddPathRequests) {
        this.rawPathRequests.addAll(toAddPathRequests);
    }

    public Set<String> getLatestPathRequests() {

        int size = rawPathRequests.size();
        if (size == 0)
            return Collections.emptySet();

        Set<String> copy = new HashSet<>(size);
        rawPathRequests.drainTo(copy);

        return copy;
    }
}
