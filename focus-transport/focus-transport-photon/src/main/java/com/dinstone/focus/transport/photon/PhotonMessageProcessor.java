/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.transport.photon;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.DefaultInvocation;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.transport.ExecutorSelector;
import com.dinstone.photon.Connection;
import com.dinstone.photon.Processor;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import io.netty.util.CharsetUtil;

public final class PhotonMessageProcessor extends Processor {
    private final Function<String, ServiceConfig> serviceFinder;
    private final ExecutorSelector executorSelector;

    public PhotonMessageProcessor(Function<String, ServiceConfig> serviceFinder, ExecutorSelector executorSelector) {
        this.serviceFinder = serviceFinder;
        this.executorSelector = executorSelector;
    }

    @Override
    public void process(Connection connection, Request request) {
        Executor executor = null;
        if (executorSelector != null) {
            Headers headers = request.headers();
            String s = headers.get(Invocation.SERVICE_KEY);
            String m = headers.get(Invocation.METHOD_KEY);
            executor = executorSelector.select(s, m);
        }
        if (executor != null) {
            executor.execute(() -> invoke(connection, request));
        } else {
            invoke(connection, request);
        }
    }

    private void invoke(Connection connection, Request request) {
        InvokeException exception;
        try {
            // check request timeout
            if (request.isTimeout()) {
                throw new InvokeException(ErrorCode.TIMEOUT_ERROR, "request timeout");
            }

            Headers headers = request.headers();
            // check service
            String service = headers.get(Invocation.SERVICE_KEY);
            ServiceConfig serviceConfig = serviceFinder.apply(service);
            if (serviceConfig == null) {
                throw new ServiceException(ErrorCode.SERVICE_ERROR, "unknown service: " + service);
            }

            // check method
            String methodName = headers.get(Invocation.METHOD_KEY);
            MethodConfig methodConfig = serviceConfig.lookup(methodName);
            if (methodConfig == null) {
                throw new ServiceException(ErrorCode.METHOD_ERROR, "unknown method: " + service + "/" + methodName);
            }

            // decode invocation from request
            DefaultInvocation invocation = decode(request, serviceConfig, methodConfig);
            invocation.context().setRemoteAddress(connection.getRemoteAddress());
            invocation.context().setLocalAddress(connection.getLocalAddress());

            // invoke invocation
            serviceConfig.getHandler().handle(invocation).whenComplete((reply, error) -> {
                if (error != null) {
                    errorHandle(connection, request, error);
                } else {
                    // encode reply to response
                    Response response = encode(reply, serviceConfig, methodConfig);
                    response.setSequence(request.getSequence());

                    // send response with reply
                    connection.sendResponse(response);
                }
            });

            return;
        } catch (InvokeException e) {
            exception = e;
        } catch (Throwable e) {
            exception = new InvokeException(ErrorCode.INVOKE_ERROR, e);
        }

        errorHandle(connection, request, exception);
    }

    private Response encode(Object reply, ServiceConfig serviceConfig, MethodConfig methodConfig) {
        Response response = new Response();
        response.setStatus(Status.SUCCESS);
        byte[] content = null;
        if (reply != null) {
            try {
                Serializer serializer = serviceConfig.getSerializer();
                content = serializer.encode(reply, methodConfig.getReturnType());
                response.headers().add(Serializer.TYPE_KEY, serializer.type());
            } catch (IOException e) {
                throw new ServiceException(ErrorCode.CODEC_ERROR,
                        "serialize encode error: " + methodConfig.getMethodName(), e);
            }

            Compressor compressor = serviceConfig.getCompressor();
            if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
                try {
                    content = compressor.encode(content);
                    response.headers().add(Compressor.TYPE_KEY, compressor.type());
                } catch (IOException e) {
                    throw new ServiceException(ErrorCode.CODEC_ERROR,
                            "compress encode error: " + methodConfig.getMethodName(), e);
                }
            }
        }
        response.setContent(content);

        return response;
    }

    private DefaultInvocation decode(Request request, ServiceConfig serviceConfig, MethodConfig methodConfig) {
        Object value;
        Headers headers = request.headers();
        byte[] content = request.getContent();
        if (content == null || content.length == 0) {
            value = null;
        } else {
            String compressorType = headers.get(Compressor.TYPE_KEY);
            Compressor compressor = serviceConfig.getCompressor();
            if (compressor != null && compressorType != null) {
                try {
                    content = compressor.decode(content);
                } catch (IOException e) {
                    throw new ServiceException(ErrorCode.CODEC_ERROR,
                            "compress decode error: " + methodConfig.getMethodName(), e);
                }
            }

            try {
                Serializer serializer = serviceConfig.getSerializer();
                Class<?> contentType = methodConfig.getParamType();
                value = serializer.decode(content, contentType);
            } catch (IOException e) {
                throw new ServiceException(ErrorCode.CODEC_ERROR,
                        "serialize decode error: " + methodConfig.getMethodName(), e);
            }
        }

        String service = headers.get(Invocation.SERVICE_KEY);
        String method = headers.get(Invocation.METHOD_KEY);
        DefaultInvocation invocation = new DefaultInvocation(service, method, value);
        invocation.setConsumer(headers.get(Invocation.CONSUMER_KEY));
        invocation.setProvider(headers.get(Invocation.PROVIDER_KEY));
        invocation.setTimeout(request.getTimeout());
        invocation.setServiceConfig(serviceConfig);
        invocation.setMethodConfig(methodConfig);
        headers.forEach(e -> invocation.attributes().put(e.getKey(), e.getValue()));
        return invocation;
    }

    private void errorHandle(Connection connection, Request request, Throwable error) {
        InvokeException exception;
        if (error instanceof InvokeException) {
            exception = (InvokeException) error;
        } else {
            exception = new InvokeException(ErrorCode.INVOKE_ERROR, error);
        }
        // send response with exception
        Response response = new Response();
        response.setStatus(Status.FAILURE);
        response.setSequence(request.getSequence());

        response.headers().setInt(InvokeException.CODE_KEY, exception.getCode().value());
        if (exception.getMessage() != null) {
            response.setContent(exception.getMessage().getBytes(CharsetUtil.UTF_8));
        }
        connection.sendResponse(response);
    }
}