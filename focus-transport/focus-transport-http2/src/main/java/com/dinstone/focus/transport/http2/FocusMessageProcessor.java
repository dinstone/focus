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

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.CodecException;
import com.dinstone.focus.exception.InvokeException;
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

public final class FocusMessageProcessor {

    private final ImplementBinding implementBinding;
    private final ExecutorSelector executorSelector;

    public FocusMessageProcessor(ImplementBinding implementBinding, ExecutorSelector executorSelector) {
        this.implementBinding = implementBinding;
        this.executorSelector = executorSelector;
    }

    private void invoke(Channel channel, Http2HeadersFrame headersFrame, Http2DataFrame dataFrame) {
        InvokeException exception = null;
        try {
            Http2Headers headers = headersFrame.headers();
            // check service
            // TODO delete
            String group = null;
            String service = headers.get(Call.SERVICE_KEY).toString();
            ServiceConfig serviceConfig = implementBinding.lookup(service);
            if (serviceConfig == null) {
                throw new NoSuchMethodException("unkown service: " + service + "[" + group + "]");
            }

            // check method
            String methodName = headers.get(Call.METHOD_KEY).toString();
            MethodConfig methodConfig = serviceConfig.getMethodConfig(methodName);
            if (methodConfig == null) {
                throw new NoSuchMethodException("unkown method: " + service + "[" + group + "]." + methodName);
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
                            reply.attach().put(Serializer.TYPE_KEY, serializer.serializerType());
                        } catch (IOException e) {
                            throw new CodecException("serialize encode error: " + methodConfig.getMethodName(), e);
                        }

                        Compressor compressor = serviceConfig.getCompressor();
                        if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
                            try {
                                content = compressor.encode(content);
                                reply.attach().put(Compressor.TYPE_KEY, compressor.compressorType());
                            } catch (IOException e) {
                                throw new CodecException("compress encode error: " + methodConfig.getMethodName(), e);
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
            errorHandle(channel, exception);
        }
    }

    private void errorHandle(Channel channel, Throwable error) {
        InvokeException exception;
        if (error instanceof InvokeException) {
            exception = (InvokeException) error;
        } else {
            exception = new InvokeException(99, error);
        }
        // send response with exception
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText());
        headers.setInt(InvokeException.CODE_KEY, exception.getCode());

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
                    throw new CodecException("compress decode error: " + methodConfig.getMethodName(), e);
                }
            }

            try {
                Serializer serializer = serviceConfig.getSerializer();
                Class<?> contentType = methodConfig.getParamType();
                value = serializer.decode(content, contentType);
            } catch (IOException e) {
                throw new CodecException("serialize decode error: " + methodConfig.getMethodName(), e);
            }
        }

        Call call = new Call();
        call.setSource(headers.get(Call.SOURCE_KEY, "").toString());
        call.setTarget(headers.get(Call.TARGET_KEY, "").toString());
        call.setService(headers.get(Call.SERVICE_KEY).toString());
        call.setMethod(headers.get(Call.METHOD_KEY).toString());
        call.setTimeout(headers.getIntAndRemove(Call.TIMEOUT_KEY));
        headers.forEach(e -> call.attach().put(e.getKey().toString(), e.getValue().toString()));
        call.setParameter(value);
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