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
package com.dinstone.focus.transport.http2;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Context;
import com.dinstone.focus.invoke.DefaultInvocation;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.transport.ExecutorSelector;
import com.dinstone.focus.utils.ConstantUtil;
import com.dinstone.focus.utils.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.CharsetUtil;

public final class Http2MessageProcessor {

    private final Function<String, ServiceConfig> serviceFinder;
    private final ExecutorSelector executorSelector;

    public Http2MessageProcessor(Function<String, ServiceConfig> serviceFinder, ExecutorSelector executorSelector) {
        this.serviceFinder = serviceFinder;
        this.executorSelector = executorSelector;
    }

    private void invoke(Channel channel, Http2HeadersFrame headersFrame, Http2DataFrame dataFrame) {
        InvokeException exception;
        try {
            Http2Headers headers = headersFrame.headers();
            // check service
            String service = headers.get(Invocation.SERVICE_KEY).toString();
            ServiceConfig serviceConfig = serviceFinder.apply(service);
            if (serviceConfig == null) {
                throw new ServiceException(ErrorCode.SERVICE_ERROR, "unknown service: " + service);
            }

            // check method
            String methodName = headers.get(Invocation.METHOD_KEY).toString();
            MethodConfig methodConfig = serviceConfig.lookup(methodName);
            if (methodConfig == null) {
                throw new ServiceException(ErrorCode.METHOD_ERROR, "unknown method: " + service + "/" + methodName);
            }

            // decode invocation from request
            ByteBuf dataBuf = dataFrame == null ? null : dataFrame.content();
            Invocation invocation = decode(headers, dataBuf, serviceConfig, methodConfig);

            try (Context context = Context.create()) {
                // set link
                String link = NetworkUtil.link(channel.remoteAddress(), channel.localAddress());
                context.put(ConstantUtil.RPC_LINK, link);

                // invoke invocation
                serviceConfig.getHandler().handle(invocation).whenComplete((reply, error) -> {
                    if (error != null) {
                        errorHandle(channel, error);
                    } else {
                        byte[] content = null;
                        if (reply != null) {
                            try {
                                Serializer serializer = serviceConfig.getSerializer();
                                content = serializer.encode(reply, methodConfig.getReturnType());
                            } catch (IOException e) {
                                throw new ServiceException(ErrorCode.CODEC_ERROR,
                                        "serialize encode error: " + methodConfig.getMethodName(), e);
                            }

                            Compressor compressor = serviceConfig.getCompressor();
                            if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
                                try {
                                    content = compressor.encode(content);
                                } catch (IOException e) {
                                    throw new ServiceException(ErrorCode.CODEC_ERROR,
                                            "compress encode error: " + methodConfig.getMethodName(), e);
                                }
                            }
                        }

                        DefaultHttp2Headers h = new DefaultHttp2Headers();
                        h.status(HttpResponseStatus.OK.codeAsText());
                        if (content == null) {
                            channel.writeAndFlush(new DefaultHttp2HeadersFrame(h, true));
                        } else {
                            channel.write(new DefaultHttp2HeadersFrame(h, false));
                            ByteBuf bb = channel.alloc().ioBuffer(content.length).writeBytes(content);
                            channel.writeAndFlush(new DefaultHttp2DataFrame(bb, true));
                        }
                    }
                });
            }

            return;
        } catch (InvokeException e) {
            exception = e;
        } catch (Throwable e) {
            exception = new InvokeException(ErrorCode.INVOKE_ERROR, e);
        }

        errorHandle(channel, exception);
    }

    private void errorHandle(Channel channel, Throwable error) {
        InvokeException exception;
        if (error instanceof InvokeException) {
            exception = (InvokeException) error;
        } else {
            exception = new InvokeException(ErrorCode.INVOKE_ERROR, error);
        }
        // send response with exception
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText());
        headers.setInt(InvokeException.CODE_KEY, exception.getCode().value());

        String message = exception.getMessage();
        if (message == null) {
            channel.write(new DefaultHttp2HeadersFrame(headers, true));
        } else {
            ByteBuf ioBuffer = channel.alloc().ioBuffer();
            ByteBuf buf = ioBuffer.writeBytes(message.getBytes(CharsetUtil.UTF_8));
            channel.write(new DefaultHttp2HeadersFrame(headers, false));
            channel.writeAndFlush(new DefaultHttp2DataFrame(buf, true));
        }

    }

    private Invocation decode(Http2Headers headers, ByteBuf bbc, ServiceConfig serviceConfig,
            MethodConfig methodConfig) {
        Object value;
        if (bbc == null) {
            value = null;
        } else {
            byte[] content = new byte[bbc.readableBytes()];
            bbc.readBytes(content);

            CharSequence compressorType = headers.get(Compressor.TYPE_KEY);
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

        String service = headers.get(Invocation.SERVICE_KEY).toString();
        String method = headers.get(Invocation.METHOD_KEY).toString();
        DefaultInvocation invocation = new DefaultInvocation(service, method, value);
        invocation.setConsumer(headers.get(Invocation.CONSUMER_KEY, "").toString());
        invocation.setProvider(headers.get(Invocation.PROVIDER_KEY, "").toString());
        invocation.setTimeout(headers.getIntAndRemove(Invocation.TIMEOUT_KEY));
        invocation.setServiceConfig(serviceConfig);
        invocation.setMethodConfig(methodConfig);
        headers.forEach(e -> invocation.attributes().put(e.getKey().toString(), e.getValue().toString()));
        return invocation;
    }

    public void process(Channel channel, Http2HeadersFrame headersFrame, Http2DataFrame dataFrame) {
        Executor executor = null;
        if (executorSelector != null) {
            Http2Headers headers = headersFrame.headers();
            String s = headers.get(Invocation.SERVICE_KEY).toString();
            String m = headers.get(Invocation.METHOD_KEY).toString();
            executor = executorSelector.select(s, m);
        }
        if (executor != null) {
            executor.execute(() -> invoke(channel, headersFrame, dataFrame));
        } else {
            invoke(channel, headersFrame, dataFrame);
        }
    }
}