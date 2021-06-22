package org.db.example;

import org.db.BayeuxParameters;
import org.db.EmpConnector;
import org.db.LoginHelper;
import org.db.TopicSubscription;
import org.eclipse.jetty.util.ajax.JSON;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.cometd.bayeux.Channel.*;

public class DevLoginExample {
    // More than one thread can be used in the thread pool which leads to parallel processing of events which may be acceptable by the application
    // The main purpose of asynchronous event processing is to make sure that client is able to perform /meta/connect requests which keeps the session alive on the server side
    private final ExecutorService workerThreadPool = Executors.newFixedThreadPool(1);

    public static void main(String[] argv) throws Throwable {
        DevLoginExample devLoginExample = new DevLoginExample();
        devLoginExample.processEvents(argv);
    }

    public void processEvents(String[] argv) throws Throwable {
        if (argv.length < 4 || argv.length > 5) {
            System.err.println("Usage: DevLoginExample url username password topic [replayFrom]");
            System.exit(1);
        }

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(new URL(argv[0]), argv[1], argv[2]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

        EmpConnector connector = new EmpConnector(params);
        LoggingListener loggingListener = new LoggingListener(true, true);

        connector.addListener(META_HANDSHAKE, loggingListener)
                .addListener(META_CONNECT, loggingListener)
                .addListener(META_DISCONNECT, loggingListener)
                .addListener(META_SUBSCRIBE, loggingListener)
                .addListener(META_UNSUBSCRIBE, loggingListener);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        long replayFrom = EmpConnector.REPLAY_FROM_TIP;
        if (argv.length == 5) {
            replayFrom = Long.parseLong(argv[4]);
        }
        TopicSubscription subscription;
        try {
            subscription = connector.subscribe(argv[3], replayFrom, getConsumer()).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            System.err.println(e.getCause().toString());
            System.exit(1);
            throw e.getCause();
        } catch (TimeoutException e) {
            System.err.println("Timed out subscribing");
            System.exit(1);
            throw e.getCause();
        }

        System.out.printf("Subscribed: %s%n", subscription);
    }

    public Consumer<Map<String, Object>> getConsumer() {
        return event -> workerThreadPool.submit(() -> System.out.printf("Received:\n%s, \nEvent processed by threadName:%s, threadId: %s%n", JSON.toString(event), Thread.currentThread().getName(), Thread.currentThread().getId()));
    }
}
