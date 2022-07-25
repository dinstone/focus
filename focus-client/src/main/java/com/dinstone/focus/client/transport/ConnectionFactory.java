/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.photon.Connection;
import com.dinstone.photon.Connector;

public class ConnectionFactory {

    private final Connector connector;

    private final ClientOptions clientOptions;

    private final ConcurrentMap<InetSocketAddress, ConnectionPool> connectionPoolMap;

    public ConnectionFactory(ClientOptions clientOptions) {
        if (clientOptions == null) {
            throw new IllegalArgumentException("clientOptions is null");
        }
        this.clientOptions = clientOptions;

        this.connector = new Connector(clientOptions.getConnectOptions());
        this.connectionPoolMap = new ConcurrentHashMap<InetSocketAddress, ConnectionPool>();
    }

    public Connection create(InetSocketAddress socketAddress) throws Exception {
        ConnectionPool connectionPool = connectionPoolMap.get(socketAddress);
        if (connectionPool == null) {
            connectionPoolMap.putIfAbsent(socketAddress, new ConnectionPool(socketAddress));
            connectionPool = connectionPoolMap.get(socketAddress);
        }
        return connectionPool.getConnection();
    }

    public void destroy() {
        for (ConnectionPool connectionPool : connectionPoolMap.values()) {
            if (connectionPool != null) {
                connectionPool.destroy();
            }
        }
        connectionPoolMap.clear();
        connector.destroy();
    }

    class ConnectionPool {

        private final InetSocketAddress socketAddress;

        private Connection[] connections;

        private int count;

        public ConnectionPool(InetSocketAddress socketAddress) {
            this.socketAddress = socketAddress;
            this.connections = new Connection[clientOptions.getConnectPoolSize()];
        }

        public synchronized Connection getConnection() throws Exception {
            int index = count++ % connections.length;
            Connection connection = connections[index];
            if (connection == null || !connection.isActive()) {
                if (connection != null) {
                    connection.destroy();
                }

                connection = connector.connect(socketAddress);
                connections[index] = connection;
            }
            return connection;
        }

        public void destroy() {
            for (Connection connection : connections) {
                if (connection != null) {
                    connection.destroy();
                }
            }
        }

    }

}
