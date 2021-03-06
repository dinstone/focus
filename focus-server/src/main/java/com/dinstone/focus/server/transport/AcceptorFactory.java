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
package com.dinstone.focus.server.transport;

import java.util.concurrent.Executor;

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeContext;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.photon.Acceptor;
import com.dinstone.photon.ExchangeException;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.processor.MessageProcessor;
import com.dinstone.photon.processor.ProcessContext;
import com.dinstone.photon.util.ExceptionUtil;

public class AcceptorFactory {

    private ServerOptions serverOption;

    public AcceptorFactory(ServerOptions serverOption) {
        this.serverOption = serverOption;
    }

    public Acceptor create(final ImplementBinding binding) {
        Acceptor acceptor = new Acceptor(serverOption.getAcceptOptions());
        acceptor.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(ProcessContext ctx, Object msg) {
                if (msg instanceof Request) {
                    Executor executor = null;
                    Request request = (Request) msg;
                    ExecutorSelector selector = serverOption.getExecutorSelector();
                    if (selector != null) {
                        String g = request.getHeaders().get("rpc.call.group");
                        String s = request.getHeaders().get("rpc.call.service");
                        String m = request.getHeaders().get("rpc.call.method");
                        executor = selector.select(g, s, m);
                    }
                    if (executor != null) {
                        executor.execute(new Runnable() {

                            @Override
                            public void run() {
                                invoke(binding, ctx, request);
                            }
                        });
                    } else {
                        invoke(binding, ctx, request);
                    }
                }
            }

            private void invoke(final ImplementBinding binding, ProcessContext context, Request request) {
                if (context.isTimeout()) {
                    return;
                }

                ProtocolCodec codec = CodecManager.codec(request.getCodec());

                ExchangeException exception = null;
                try {
                    // decode call from request
                    Call call = codec.decode(request);

                    ServiceConfig config = binding.lookup(call.getService(), call.getGroup());
                    if (config == null) {
                        throw new NoSuchMethodException(
                                "unkown service: " + call.getService() + "[" + call.getGroup() + "]");
                    }

                    InvokeContext.getContext().put("connection.remote", context.remoteAddress());

                    // invoke call
                    Reply reply = config.getHandler().invoke(call);

                    // encode reply to response
                    Response response = codec.encode(reply);
                    response.setMsgId(request.getMsgId());
                    response.setStatus(Status.SUCCESS);
                    response.setCodec(codec.codecId());

                    // send response with reply
                    context.send(response);
                    return;
                } catch (NoSuchMethodException e) {
                    String message = ExceptionUtil.getMessage(e);
                    exception = new ExchangeException(104, message, e);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    String message = ExceptionUtil.getMessage(e);
                    exception = new ExchangeException(103, message, e);
                } catch (Throwable e) {
                    String message = ExceptionUtil.getMessage(e);
                    exception = new ExchangeException(109, message, e);
                }

                if (exception != null) {
                    Response response = new Response();
                    response.setMsgId(request.getMsgId());
                    response.setStatus(Status.FAILURE);
                    response.setContent(ExceptionCodec.encode(exception));
                    // send response with exception
                    context.send(response);
                }
            }
        });
        return acceptor;
    }

}
