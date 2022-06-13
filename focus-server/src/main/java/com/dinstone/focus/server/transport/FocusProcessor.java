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
import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.photon.ExchangeException;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.connection.Connection;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.processor.MessageProcessor;

public final class FocusProcessor implements MessageProcessor {
    private final ImplementBinding binding;
    private ExecutorSelector selector;

    public FocusProcessor(ImplementBinding binding, ExecutorSelector selector) {
        this.binding = binding;
        this.selector = selector;
    }

    @Override
    public void process(Connection connection, Object msg) {
        if (msg instanceof Request) {
            Executor executor = null;
            Request request = (Request) msg;
            if (selector != null) {
                Headers headers = request.headers();
                String g = headers.get("rpc.call.group");
                String s = headers.get("rpc.call.service");
                String m = headers.get("rpc.call.method");
                executor = selector.select(g, s, m);
            }
            if (executor != null) {
                executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        invoke(binding, connection, request);
                    }
                });
            } else {
                invoke(binding, connection, request);
            }
        }
    }

    private void invoke(final ImplementBinding binding, Connection connection, Request request) {
        if (request.isTimeout()) {
            return;
        }

        ExchangeException exception = null;
        try {
            String codecId = request.headers().get("rpc.call.codec");
            ProtocolCodec codec = CodecManager.codec(codecId);
            // decode call from request
            Call call = codec.decode(request);

            ServiceConfig config = binding.lookup(call.getService(), call.getGroup());
            if (config == null) {
                throw new NoSuchMethodException("unkown service: " + call.getService() + "[" + call.getGroup() + "]");
            }
            if (!config.hasMethod(call.getMethod())) {
                throw new NoSuchMethodException(
                        "unkown method: " + call.getService() + "[" + call.getGroup() + "]" + call.getMethod());
            }

            // invoke call
            Reply reply = config.getHandler().invoke(call);

            // encode reply to response
            Response response = codec.encode(reply);
            response.setMsgId(request.getMsgId());
            // send response with reply
            connection.send(response);
            
            return;
        } catch (ExchangeException e) {
            exception = e;
        } catch (CodecException e) {
            exception = new ExchangeException(101, "codec excption", e);
        } catch (IllegalArgumentException e) {
            exception = new ExchangeException(102, "argument exception", e);
        } catch (IllegalAccessException e) {
            exception = new ExchangeException(103, "access exception", e);
        } catch (NoSuchMethodException e) {
            exception = new ExchangeException(104, "no method exception", e);
        } catch (Throwable e) {
            exception = new ExchangeException(109, "unkow exception", e);
        }

        if (exception != null) {
            Response response = new Response();
            response.setMsgId(request.getMsgId());
            response.setStatus(Status.FAILURE);
            response.setContent(ExceptionCodec.encode(exception));
            // send response with exception
            connection.send(response);
        }
    }
}