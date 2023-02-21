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

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.CodecException;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
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

    private Channel channel;

    public Http2Channel(Channel channel) {
        this.channel = channel;
    }

    public boolean isActive() {
        return channel.isActive();
    }

    public void destroy() {
        channel.close();
    }

    public CompletableFuture<Reply> send(Call call, ServiceConfig serviceConfig) {
        CompletableFuture<Reply> future = new CompletableFuture<>();

        MethodConfig methodConfig = serviceConfig.getMethodConfig(call.getMethod());
        Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(channel);
        Http2StreamChannel streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
        streamChannel.pipeline().addLast(new StreamChannelHandler(future, serviceConfig, methodConfig));

        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.add(Call.GROUP_KEY, call.getGroup());
        headers.add(Call.SERVICE_KEY, call.getService());
        headers.add(Call.METHOD_KEY, call.getMethod());
        headers.addInt(Call.TIMEOUT_KEY, call.getTimeout());
        call.attach().forEach(e -> {
            if (e.getKey() != null && e.getValue() != null) {
                headers.add(e.getKey(), e.getValue());
            }
        });

        headers.path(PATH).method(HttpMethod.POST.toString());

        byte[] content = encodeContent(call, serviceConfig, methodConfig);
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

    private byte[] encodeContent(Call call, ServiceConfig serviceConfig, MethodConfig methodConfig) {
        byte[] content = null;
        if (call.getParameter() != null) {
            try {
                Serializer serializer = serviceConfig.getSerializer();
                content = serializer.encode(call.getParameter(), methodConfig.getParamType());
                call.attach().put(Serializer.TYPE_KEY, serializer.serializerType());
            } catch (IOException e) {
                throw new CodecException("serialize encode error: " + methodConfig.getMethodName(), e);
            }

            Compressor compressor = serviceConfig.getCompressor();
            if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
                try {
                    content = compressor.encode(content);
                    call.attach().put(Compressor.TYPE_KEY, compressor.compressorType());
                } catch (IOException e) {
                    throw new CodecException("compress encode error: " + methodConfig.getMethodName(), e);
                }
            }
        }
        return content;
    }

    public class StreamChannelHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

        private CompletableFuture<Reply> future;
        private ServiceConfig serviceConfig;
        private MethodConfig methodConfig;

        public StreamChannelHandler(CompletableFuture<Reply> future, ServiceConfig serviceConfig,
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
            Reply reply = new Reply();
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
                            throw new CodecException("compress decode error: " + methodConfig.getMethodName(), e);
                        }
                    }

                    try {
                        Serializer serializer = serviceConfig.getSerializer();
                        Class<?> contentType = methodConfig.getReturnType();
                        value = serializer.decode(content, contentType);
                    } catch (IOException e) {
                        throw new CodecException("serialize decode error: " + methodConfig.getMethodName(), e);
                    }
                }
                reply.value(value);
            } else {
                InvokeException error;
                if (dataFrame == null) {
                    error = new InvokeException(99, "unkown exception");
                } else {
                    int code = headers.getInt(InvokeException.CODE_KEY, 0);
                    ByteBuf buf = dataFrame.content();
                    byte[] content = new byte[buf.readableBytes()];
                    buf.readBytes(content);
                    String message = new String(content, CharsetUtil.UTF_8);
                    error = new InvokeException(code, message);
                }
                reply.error(error);
            }

            headers.forEach(e -> {
                if (e.getKey() != null && e.getValue() != null) {
                    reply.attach().put(e.getKey().toString(), e.getValue().toString());
                }
            });

            future.complete(reply);
        }

    }

}
