// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2013-2020 VMware, Inc. or its affiliates. All rights reserved.
package com.rabbitmq.jms.client;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.GetResponse;
import com.rabbitmq.jms.util.TimeTracker;

/**
 * Receive messages from RMQ Queue with delay (timer).
 * <p>
 * The blocking method <code>get()</code> only returns with <code>null</code> when either the Receiver is closed,
 * or the timeout expires.
 * </p>
 */
class DelayedReceiver {

    private final Logger logger = LoggerFactory.getLogger(DelayedReceiver.class);

    private static final TimeTracker POLLING_INTERVAL = new TimeTracker(100, TimeUnit.MILLISECONDS); // one tenth of a second

    @SuppressWarnings("unused")
    private final int batchingSize;
    private final RMQMessageConsumer rmqMessageConsumer;

    private final Object responseLock = new Object();
    private boolean aborted = false; // @GuardedBy(responseLock)

    private TimeTracker pollingInterval = POLLING_INTERVAL;

    /**
     * @param batchingSize - the intended limit of messages that can be pre-fetched.
     * @param rmqMessageConsumer - the JMS MessageConsumer we are serving.
     */
    public DelayedReceiver(int batchingSize, RMQMessageConsumer rmqMessageConsumer) {
        this(batchingSize, rmqMessageConsumer, null);
    }

    /**
     * @param batchingSize - the intended limit of messages that can be pre-fetched.
     * @param rmqMessageConsumer - the JMS MessageConsumer we are serving.
     * @param pollingInt - the polling interval to user (can be null)
     * @since 2.10.0
     */
    public DelayedReceiver(int batchingSize, RMQMessageConsumer rmqMessageConsumer, Long pollingInt) {
        this.batchingSize = batchingSize;
        this.rmqMessageConsumer = rmqMessageConsumer;
        setPollingInterval(pollingInt);
    }

    /**
     * Get a message; if there isn't one, try again at intervals not exceeding the total time available. Aborts if closed while polling.
     * @param tt - keeps track of the time available
     * @return message gotten, or <code>null</code> if timeout or connection closed.
     */
    public GetResponse get(TimeTracker tt) {
        try {
            synchronized (this.responseLock) {
                GetResponse resp = this.rmqMessageConsumer.getFromRabbitQueue();
                if (resp != null) return resp;
                while (!this.aborted && !tt.timedOut()) {
                    resp = this.rmqMessageConsumer.getFromRabbitQueue();
                    if (resp != null)
                        break;
                    new TimeTracker(pollingInterval != null ? pollingInterval : POLLING_INTERVAL).timedWait(this.responseLock);
                }
                return resp;
            }

        } catch (InterruptedException e) {
            logger.warn("Get interrupted while buffer.poll-ing.", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void setPollingInterval(final Long pollingInt) {
        // Only update the polling interval if we're a positive long:
        if (pollingInt != null && pollingInt.longValue() > 1) {
            pollingInterval = new TimeTracker(pollingInt.longValue(), TimeUnit.MILLISECONDS);
        } else {
            logger.warn("Invalid or missing polling interval: " + pollingInt);
        }
    }

    private void abort() {
        synchronized(this.responseLock) {
            this.aborted = true;
            this.responseLock.notifyAll();
        }
    }

    public void close() {
        this.abort();
    }

}
