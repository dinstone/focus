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
package com.dinstone.focus.server.transport;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.codec.photon.PhotonProtocolCodec;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.focus.server.binding.ImplementBinding;
import com.dinstone.photon.Connection;
import com.dinstone.photon.MessageProcessor;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;

public final class FocusProcessor extends MessageProcessor {
    private final ImplementBinding implementBinding;
    private final ExecutorSelector executorSelector;
    private ProtocolCodec errorCodec;

    public FocusProcessor(ImplementBinding implementBinding, ExecutorSelector executorSelector) {
        this.implementBinding = implementBinding;
        this.executorSelector = executorSelector;
        this.errorCodec = protocolCodec();
    }

    private PhotonProtocolCodec protocolCodec() {
        return new PhotonProtocolCodec(null, null, 0);
    }

    @Override
    public void process(Connection connection, Request request) {
        Executor executor = null;
        if (executorSelector != null) {
            Headers headers = request.headers();
            String g = headers.get(Call.GROUP_KEY);
            String s = headers.get(Call.SERVICE_KEY);
            String m = headers.get(Call.METHOD_KEY);
            executor = executorSelector.select(g, s, m);
        }
        if (executor != null) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    invoke(connection, request);
                }
            });
        } else {
            invoke(connection, request);
        }
    }

    private void invoke(Connection connection, Request request) {
        InvokeException exception = null;
        try {
            // check request timeout
            if (request.isTimeout()) {
                throw new InvokeException(308, "request timeout");
            }

            Headers headers = request.headers();
            // check service
            String group = headers.get(Call.GROUP_KEY);
            String service = headers.get(Call.SERVICE_KEY);
            ServiceConfig config = implementBinding.lookup(service, group);
            if (config == null) {
                throw new NoSuchMethodException("unkown service: " + service + "[" + group + "]");
            }

            // check method
            String methodName = headers.get(Call.METHOD_KEY);
            MethodConfig methodConfig = config.getMethodConfig(methodName);
            if (methodConfig == null) {
                throw new NoSuchMethodException("unkown method: " + service + "[" + group + "]." + methodName);
            }

            // decode call from request
            ProtocolCodec protocolCodec = config.getProtocolCodec();
            Call call = protocolCodec.decode(request, methodConfig.getParamType());

            // invoke call
            CompletableFuture<Reply> replyFuture = config.getHandler().invoke(call);
            replyFuture.whenComplete((reply, error) -> {
                if (error != null) {
                    errorHandle(connection, request, error);
                } else {
                    // encode reply to response
                    Response response = protocolCodec.encode(reply, methodConfig.getReturnType());
                    response.setMsgId(request.getMsgId());

                    // send response with reply
                    connection.sendMessage(response);
                }
            });

            return;
        } catch (InvokeException e) {
            exception = e;
        } catch (CodecException e) {
            exception = new InvokeException(201, e);
        } catch (IllegalArgumentException e) {
            exception = new InvokeException(202, e);
        } catch (IllegalAccessException e) {
            exception = new InvokeException(203, e);
        } catch (NoSuchMethodException e) {
            exception = new InvokeException(204, e);
        } catch (Throwable e) {
            exception = new InvokeException(309, e);
        }

        if (exception != null) {
            errorHandle(connection, request, exception);
        }
    }

    private void errorHandle(Connection connection, Request request, Throwable error) {
        // send response with exception
        Response response = errorCodec.encode(new Reply(error), error.getClass());
        response.setMsgId(request.getMsgId());
        connection.sendMessage(response);
    }
}