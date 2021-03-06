package org.db.example;

import org.cometd.bayeux.Channel;
import org.db.BayeuxParameters;
import org.db.EmpConnector;
import org.db.TopicSubscription;
import org.eclipse.jetty.util.ajax.JSON;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * An example of using the EMP connector using bearer tokens
 */
public class BearerTokenExample {
    // More than one thread can be used in the thread pool which leads to parallel processing of events which may be acceptable by the application
    // The main purpose of asynchronous event processing is to make sure that client is able to perform /meta/connect requests which keeps the session alive on the server side
    private static final ExecutorService workerThreadPool = Executors.newFixedThreadPool(1);

    public static void main(String[] argv) throws Exception {
        if (argv.length < 2 || argv.length > 4) {
            System.err.println("Usage: BearerTokenExample url token topic [replayFrom]");
            System.exit(1);
        }
        long replayFrom = EmpConnector.REPLAY_FROM_TIP;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[3]);
        }

        BayeuxParameters params = new BayeuxParameters() {

            @Override
            public String bearerToken() {
                return argv[1];
            }

            @Override
            public URL host() {
                try {
                    return new URL(argv[0]);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(String.format("Unable to create url: %s", argv[0]), e);
                }
            }
        };

        Consumer<Map<String, Object>> consumer = event -> workerThreadPool.submit(() -> System.out.printf("Received:\n%s, \nEvent processed by threadName:%s, threadId: %s%n", JSON.toString(event), Thread.currentThread().getName(), Thread.currentThread().getId()));
        EmpConnector connector = new EmpConnector(params);

        connector.addListener(Channel.META_CONNECT, new LoggingListener(true, true))
        .addListener(Channel.META_DISCONNECT, new LoggingListener(true, true))
        .addListener(Channel.META_HANDSHAKE, new LoggingListener(true, true));

        connector.start().get(5, TimeUnit.SECONDS);

        TopicSubscription subscription = connector.subscribe(argv[2], replayFrom, consumer).get(5, TimeUnit.SECONDS);

        System.out.printf("Subscribed: %s%n", subscription);
    }
}
