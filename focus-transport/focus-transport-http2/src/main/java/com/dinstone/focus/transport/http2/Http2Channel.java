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
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.ExceptionUtil;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

public class Http2Channel {

    private static final AttributeKey<Http2HeadersFrame> HEADER_KEY = AttributeKey.newInstance("header.key");

    private static final String PATH = "/focus";

    private final Channel channel;

    public Http2Channel(Channel channel) {
        this.channel = channel;
    }

    public boolean isActive() {
        return channel.isActive();
    }

    public void destroy() {
        channel.close();
    }

    public CompletableFuture<Object> send(Invocation invocation) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        ServiceConfig serviceConfig = invocation.getServiceConfig();
        MethodConfig methodConfig = invocation.getMethodConfig();

        Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(channel);
        Http2StreamChannel streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
        streamChannel.pipeline().addLast(new StreamChannelHandler(future, serviceConfig, methodConfig));

        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.add(Invocation.CONSUMER_KEY, invocation.getConsumer());
        headers.add(Invocation.PROVIDER_KEY, invocation.getProvider());
        headers.add(Invocation.SERVICE_KEY, invocation.getService());
        headers.add(Invocation.METHOD_KEY, invocation.getMethod());
        headers.addInt(Invocation.TIMEOUT_KEY, invocation.getTimeout());
        invocation.attributes().forEach((k, v) -> {
            if (k != null && v != null) {
                headers.add(k, v);
            }
        });

        headers.path(PATH).method(HttpMethod.POST.toString());

        byte[] content = encodeContent(invocation, serviceConfig, methodConfig);
        if (content != null) {
            streamChannel.write(new DefaultHttp2HeadersFrame(headers));

            ByteBufAllocator alloc = streamChannel.alloc();
            ByteBuf byteBuf = alloc.ioBuffer(content.length).writeBytes(content);
            streamChannel.writeAndFlush(new DefaultHttp2DataFrame(byteBuf, true));
        } else {
            streamChannel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true));
        }

        return future;
    }

    private byte[] encodeContent(Invocation invocation, ServiceConfig serviceConfig, MethodConfig methodConfig) {
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
        return content;
    }

    public static class StreamChannelHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

        private CompletableFuture<Object> future;
        private ServiceConfig serviceConfig;
        private MethodConfig methodConfig;

        public StreamChannelHandler(CompletableFuture<Object> future, ServiceConfig serviceConfig,
                MethodConfig methodConfig) {
            this.future = future;
            this.serviceConfig = serviceConfig;
            this.methodConfig = methodConfig;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
            if (msg instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
                if (headersFrame.isEndStream()) {
                    handle(headersFrame, null);
                } else {
                    ctx.channel().attr(HEADER_KEY).set(headersFrame);
                }
            } else if (msg instanceof Http2DataFrame) {
                Http2HeadersFrame headersFrame = ctx.channel().attr(HEADER_KEY).get();
                Http2DataFrame dataFrame = (Http2DataFrame) msg;
                handle(headersFrame, dataFrame);
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        private void handle(Http2HeadersFrame headersFrame, Http2DataFrame dataFrame) {
            Http2Headers headers = headersFrame.headers();
            if (headers.status().equals(HttpResponseStatus.OK.codeAsText())) {
                Object value = null;
                if (dataFrame != null) {
                    ByteBuf buf = dataFrame.content();
                    byte[] content = new byte[buf.readableBytes()];
                    buf.readBytes(content);

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
                        Class<?> contentType = methodConfig.getReturnType();
                        value = serializer.decode(content, contentType);
                    } catch (IOException e) {
                        throw new ServiceException(ErrorCode.CODEC_ERROR,
                                "serialize decode error: " + methodConfig.getMethodName(), e);
                    }
                }
                future.complete(value);
            } else {
                // error handle
                String message = null;
                if (dataFrame != null && dataFrame.content().readableBytes() > 0) {
                    ByteBuf buf = dataFrame.content();
                    byte[] content = new byte[buf.readableBytes()];
                    buf.readBytes(content);
                    message = new String(content, CharsetUtil.UTF_8);
                }

                int errorCode = headers.getInt(InvokeException.CODE_KEY, 0);
                future.complete(ExceptionUtil.invokeException(errorCode, message));
            }
        }

    }

}
