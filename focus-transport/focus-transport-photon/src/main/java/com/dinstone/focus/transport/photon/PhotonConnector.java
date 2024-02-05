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
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.ExceptionUtil;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.transport.Connector;
import com.dinstone.photon.Connection;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import io.netty.util.CharsetUtil;

public class PhotonConnector implements Connector {

    private final PhotonConnectionFactory commonConnectionFactory;

    private final PhotonConnectionFactory secureConnectionFactory;

    public PhotonConnector(PhotonConnectOptions connectOptions) {
        if (connectOptions == null) {
            throw new IllegalArgumentException("connectOptions is null");
        }

        PhotonConnectOptions commonConnectOptions = new PhotonConnectOptions(connectOptions);
        commonConnectOptions.setEnableSsl(false);
        commonConnectionFactory = new PhotonConnectionFactory(commonConnectOptions);

        PhotonConnectOptions secureConnectOptions = new PhotonConnectOptions(connectOptions);
        secureConnectOptions.setEnableSsl(true);
        secureConnectionFactory = new PhotonConnectionFactory(secureConnectOptions);
    }

    @Override
    public CompletableFuture<Object> send(Invocation invocation, ServiceInstance instance) throws Exception {
        // create connection
        Connection connection;
        if (instance.isEnableSsl()) {
            connection = secureConnectionFactory.create(instance.getInstanceAddress());
        } else {
            connection = commonConnectionFactory.create(instance.getInstanceAddress());
        }

        invocation.context().setRemoteAddress(connection.getRemoteAddress());
        invocation.context().setLocalAddress(connection.getLocalAddress());

        ServiceConfig serviceConfig = invocation.getServiceConfig();
        MethodConfig methodConfig = invocation.getMethodConfig();

        // codec invocation to request
        Request request = encode(invocation, serviceConfig, methodConfig);

        // process request
        return connection.sendRequest(request).thenApplyAsync((response) -> {
            // process response
            return decode(response, serviceConfig, methodConfig);
        });
    }

    private Object decode(Response response, ServiceConfig serviceConfig, MethodConfig methodConfig) {
        Headers headers = response.headers();
        if (response.getStatus() == Status.SUCCESS) {
            byte[] content = response.getContent();
            if (content == null || content.length == 0) {
                return null;
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
                    Class<?> contentType = methodConfig.getReturnType();
                    return serializer.decode(content, contentType);
                } catch (IOException e) {
                    throw new ServiceException(ErrorCode.CODEC_ERROR,
                            "serialize decode error: " + methodConfig.getMethodName(), e);
                }
            }
        } else {
            String message = null;
            byte[] encoded = response.getContent();
            if (encoded != null && encoded.length > 0) {
                message = new String(encoded, CharsetUtil.UTF_8);
            }

            String codeKey = InvokeException.CODE_KEY;
            int value = ErrorCode.UNKNOWN_ERROR.value();
            int errorCode = headers.getInt(codeKey, value);
            throw ExceptionUtil.invokeException(errorCode, message);
        }
    }

    private Request encode(Invocation invocation, ServiceConfig serviceConfig, MethodConfig methodConfig) {
        byte[] content = null;
        if (invocation.getParameter() != null) {
            try {
                Serializer serializer = serviceConfig.getSerializer();
                content = serializer.encode(invocation.getParameter(), methodConfig.getParamType());
                invocation.attributes().put(Serializer.TYPE_KEY, serializer.type());
            } catch (IOException e) {
                throw new ServiceException(ErrorCode.CODEC_ERROR,
                        "serialize encode error: " + methodConfig.getMethodName(), e);
            }

            Compressor compressor = serviceConfig.getCompressor();
            if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
                try {
                    content = compressor.encode(content);
                    invocation.attributes().put(Compressor.TYPE_KEY, compressor.type());
                } catch (IOException e) {
                    throw new ServiceException(ErrorCode.CODEC_ERROR,
                            "compress encode error: " + methodConfig.getMethodName(), e);
                }
            }
        }

        Request request = new Request();
        Headers headers = request.headers();
        headers.add(Invocation.CONSUMER_KEY, invocation.getConsumer());
        headers.add(Invocation.PROVIDER_KEY, invocation.getProvider());
        headers.add(Invocation.SERVICE_KEY, invocation.getService());
        headers.add(Invocation.METHOD_KEY, invocation.getMethod());
        headers.setAll(invocation.attributes().entrySet());
        request.setTimeout(invocation.getTimeout());
        request.setContent(content);
        return request;
    }

    @Override
    public void destroy() {
        commonConnectionFactory.destroy();
    }

}
