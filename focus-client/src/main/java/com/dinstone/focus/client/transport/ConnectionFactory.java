/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.Connector;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Status;
import com.dinstone.photon.processor.MessageProcessor;

import io.netty.channel.ChannelHandlerContext;

/**
 * connetcion factory.
 * 
 * @author guojinfei
 * 
 * @version 2.0.0.2015-11-3
 */
public class ConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactory.class);

    private ClientOptions clientOptions;

    private Connector connector;

    public ConnectionFactory(ClientOptions clientOptions) {
        this.clientOptions = clientOptions;

        this.connector = new Connector(clientOptions.getConnectOptions());
        this.connector.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(ChannelHandlerContext ctx, Notice msg) {
                LOG.warn("unsported message notice");
            }

            @Override
            public void process(ChannelHandlerContext ctx, Request msg) {
                LOG.warn("unsported message Request");
            }
        });
    }

    public Connection create(InetSocketAddress sa) throws Exception {
        return new ConnectionWrap(connector.connect(sa), clientOptions.getCodec());
    }

    public void destroy() {
        if (connector != null) {
            connector.destroy();
        }
    }

    public static class ConnectionWrap implements Connection {

        private static final AtomicInteger IDGENER = new AtomicInteger();

        private com.dinstone.photon.connection.Connection connection;

        private byte codecCode;

        public ConnectionWrap(com.dinstone.photon.connection.Connection connection, String codecCode) {
            this.connection = connection;
            this.codecCode = CodecManager.codec(codecCode);
        }

        @Override
        public Reply invoke(Call call) throws Exception {
            Request request = new Request();
            request.setMsgId(IDGENER.incrementAndGet());
            request.setCodec(codecCode);
            request.setTimeout(call.getTimeout());
            // encode call to request
            CodecManager.encode(request, call);

            // remote call
            Response response = connection.sync(request);

            // handle response by success
            if (response.getStatus() == Status.SUCCESS) {
                return CodecManager.decode(response);
            } else {
                throw ExceptionCodec.decode(response.getContent());
            }
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return connection.getRemoteAddress();
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return connection.getLocalAddress();
        }

        @Override
        public boolean isAlive() {
            return connection.isActive();
        }

        @Override
        public void destroy() {
            connection.destroy();
        }

    }
}