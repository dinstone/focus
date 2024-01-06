/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.transport.ExecutorSelector;

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
        InvokeException exception = null;
        try {
            Http2Headers headers = headersFrame.headers();
            // check service
            String service = headers.get(Call.SERVICE_KEY).toString();
            ServiceConfig serviceConfig = serviceFinder.apply(service);
            if (serviceConfig == null) {
                throw new ServiceException(ErrorCode.SERVICE_ERROR, "unkown service: " + service);
            }

            // check method
            String methodName = headers.get(Call.METHOD_KEY).toString();
            MethodConfig methodConfig = serviceConfig.lookup(methodName);
            if (methodConfig == null) {
                throw new ServiceException(ErrorCode.METHOD_ERROR, "unkown method: " + service + "/" + methodName);
            }

            // decode call from request
            ByteBuf dataBuf = dataFrame == null ? null : dataFrame.content();
            Call call = decode(headers, dataBuf, serviceConfig, methodConfig);

            // invoke call
            CompletableFuture<Reply> replyFuture = serviceConfig.getHandler().handle(call);
            replyFuture.whenComplete((reply, error) -> {
                if (error != null) {
                    errorHandle(channel, error);
                } else {
                    byte[] content = null;
                    if (reply.getData() != null) {
                        try {
                            Serializer serializer = serviceConfig.getSerializer();
                            content = serializer.encode(reply.getData(), methodConfig.getReturnType());
                            reply.attach().put(Serializer.TYPE_KEY, serializer.type());
                        } catch (IOException e) {
                            throw new ServiceException(ErrorCode.CODEC_ERROR,
                                    "serialize encode error: " + methodConfig.getMethodName(), e);
                        }

                        Compressor compressor = serviceConfig.getCompressor();
                        if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
                            try {
                                content = compressor.encode(content);
                                reply.attach().put(Compressor.TYPE_KEY, compressor.type());
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

    private Call decode(Http2Headers headers, ByteBuf bbc, ServiceConfig serviceConfig, MethodConfig methodConfig) {
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

        String service = headers.get(Call.SERVICE_KEY).toString();
        String method = headers.get(Call.METHOD_KEY).toString();
        Call call = new Call(service, method, value);
        call.setConsumer(headers.get(Call.CONSUMER_KEY, "").toString());
        call.setProvider(headers.get(Call.PROVIDER_KEY, "").toString());
        call.setTimeout(headers.getIntAndRemove(Call.TIMEOUT_KEY));
        headers.forEach(e -> call.attach().put(e.getKey().toString(), e.getValue().toString()));
        return call;
    }

    public void process(Channel channel, Http2HeadersFrame headersFrame, Http2DataFrame dataFrame) {
        Executor executor = null;
        if (executorSelector != null) {
            Http2Headers headers = headersFrame.headers();
            String s = headers.get(Call.SERVICE_KEY).toString();
            String m = headers.get(Call.METHOD_KEY).toString();
            executor = executorSelector.select(s, m);
        }
        if (executor != null) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    invoke(channel, headersFrame, dataFrame);
                }
            });
        } else {
            invoke(channel, headersFrame, dataFrame);
        }
    }
}