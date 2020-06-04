/* Copyright (c) 2013-2020 VMware, Inc. or its affiliates. All rights reserved. */
package com.rabbitmq.integration.tests;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import com.rabbitmq.jms.admin.RMQConnectionFactory;

import java.security.NoSuchAlgorithmException;

/**
 * Connection factory for use in integration tests.
 */
public class RabbitAPIConnectionFactory extends AbstractTestConnectionFactory {

    private static final int RABBIT_PORT = 5672; // 5672 default; 5673 Tracer.
    private static final int RABBIT_TLS_PORT = 5671;
    private final boolean testssl;
    private int qbrMax;

    public RabbitAPIConnectionFactory() { this(false); }

    public RabbitAPIConnectionFactory(boolean testssl) { this(testssl, 0); }

    public RabbitAPIConnectionFactory(boolean testssl, int qbrMax) { this.testssl = testssl;
                                                                     this.qbrMax = qbrMax;
                                                                   }

    @Override
    public ConnectionFactory getConnectionFactory() {
        RMQConnectionFactory rmqCF = new RMQConnectionFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                if (!testssl) {
                    this.setPort(RABBIT_PORT);
                } else {
                    this.setPort(RABBIT_TLS_PORT);
                }
                return super.createConnection(userName, password);
            }
        };
        if (testssl) {
            try {
                rmqCF.useSslProtocol();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        rmqCF.setQueueBrowserReadMax(qbrMax);
        return rmqCF;
    }

}
