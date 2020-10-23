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
package com.dinstone.focus.server.transport;

import java.util.concurrent.Executor;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.processor.RpcProcessor;
import com.dinstone.photon.Acceptor;
import com.dinstone.photon.handler.MessageContext;
import com.dinstone.photon.message.Message;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.processor.MessageProcessor;

public class AcceptorFactory {

    private ServerOptions serverOption;

    public AcceptorFactory(ServerOptions serverOption) {
        this.serverOption = serverOption;
    }

    public Acceptor create(final InvokeHandler invoker) {
        Acceptor acceptor = new Acceptor(serverOption.getAcceptOptions());
        final RpcProcessor rpcProcessor = new RpcProcessor(invoker);
        acceptor.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(final MessageContext context, final Message message) {
                if (message instanceof Request) {
                    Executor executor = context.getDefaultExecutor();
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            rpcProcessor.process(context, (Request) message);
                        }
                    });
                } else if (message instanceof Notice) {
                    Executor executor = context.getDefaultExecutor();
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            rpcProcessor.process(context, (Notice) message);
                        }
                    });
                }
            }
        });
        return acceptor;
    }

}
