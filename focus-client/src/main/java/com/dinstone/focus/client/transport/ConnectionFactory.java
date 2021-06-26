/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus.client.transport;

import java.net.InetSocketAddress;

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.ConnectOptions;
import com.dinstone.photon.Connector;
import com.dinstone.photon.connection.Connection;
import com.dinstone.photon.processor.MessageProcessor;
import com.dinstone.photon.processor.ProcessContext;

/**
 * connetcion factory.
 * 
 * @author guojinfei
 * 
 * @version 2.0.0.2015-11-3
 */
public class ConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactory.class);

    private Connector connector;

    public ConnectionFactory(ConnectOptions connectOptions) {
        this.connector = new Connector(connectOptions);
        this.connector.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(ProcessContext ctx, Object msg) {
                LOG.warn("unsupported message {}", msg);
            }
        });
    }

    public Connection create(InetSocketAddress sa) throws Exception {
        return connector.connect(sa);
    }

    public void destroy() {
        if (connector != null) {
            connector.destroy();
        }
    }

}