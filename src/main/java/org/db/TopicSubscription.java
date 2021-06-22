package org.db;

/**
 * A subscription to a topic
 */
public interface TopicSubscription {

    /**
     * Cancel the subscription
     */
    void cancel();

    /**
     * @return the current replayFrom event id of the subscription
     */
    long getReplayFrom();

    /**
     * @return the topic subscribed to
     */
    String getTopic();

}
