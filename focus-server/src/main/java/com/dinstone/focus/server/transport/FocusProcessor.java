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

import java.util.concurrent.Executor;

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.photon.PhotonProtocolCodec;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodInfo;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.photon.Connection;
import com.dinstone.photon.handler.DefaultMessageProcessor;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;

public final class FocusProcessor extends DefaultMessageProcessor {
    private final ImplementBinding implementBinding;
    private final PhotonProtocolCodec protocolCodec;
    private final ExecutorSelector selector;

    public FocusProcessor(ImplementBinding implementBinding, PhotonProtocolCodec protocolCodec,
            ExecutorSelector selector) {
        this.implementBinding = implementBinding;
        this.protocolCodec = protocolCodec;
        this.selector = selector;
    }

    @Override
    public void process(Connection connection, Request msg) {
        if (msg instanceof Request) {
            Executor executor = null;
            Request request = (Request) msg;
            if (selector != null) {
                Headers headers = request.headers();
                String g = headers.get(Call.GROUP_KEY);
                String s = headers.get(Call.SERVICE_KEY);
                String m = headers.get(Call.METHOD_KEY);
                executor = selector.select(g, s, m);
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
            MethodInfo methodInfo = config.getMethodInfo(methodName);
            if (methodInfo == null) {
                throw new NoSuchMethodException("unkown method: " + service + "[" + group + "]." + methodName);
            }

            // decode call from request
            Call call = protocolCodec.decode(request, methodInfo.getParamType());

            // invoke call
            Reply reply = config.getHandler().invoke(call);

            // init reply attach
            String svalue = headers.get(Serializer.SERIALIZER_KEY);
            String cvalue = headers.get(Compressor.COMPRESSOR_KEY);
            reply.attach().put(Serializer.SERIALIZER_KEY, svalue);
            reply.attach().put(Compressor.COMPRESSOR_KEY, cvalue);

            // encode reply to response
            Response response = protocolCodec.encode(reply, methodInfo.getReturnType());
            response.setMsgId(request.getMsgId());

            // send response with reply
            connection.send(response);

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
            // send response with exception
            Response response = protocolCodec.encode(new Reply(exception), exception.getClass());
            response.setMsgId(request.getMsgId());
            connection.send(response);
        }
    }
}