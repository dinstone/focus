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

import java.net.InetSocketAddress;
import java.util.function.Function;

import javax.net.ssl.SSLException;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.transport.Acceptor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;

public class Http2Acceptor implements Acceptor {
    private static final AttributeKey<Http2HeadersFrame> HEADER_KEY = AttributeKey.newInstance("header.key");
    private Http2AcceptOptions acceptOptions;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;
    private ServerBootstrap bootstrap;
    private FocusMessageProcessor messageProcessor;

    public Http2Acceptor(Http2AcceptOptions acceptOptions) {
        this.acceptOptions = acceptOptions;

        final SslContext sslContext;
        if (acceptOptions.isEnableSsl()) {
            try {
                SslProvider provider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL
                        : SslProvider.JDK;
                sslContext = SslContextBuilder.forClient().sslProvider(provider)
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .applicationProtocolConfig(
                                new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                                        SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2))
                        .build();
            } catch (SSLException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            sslContext = null;
        }

        bossGroup = new NioEventLoopGroup(acceptOptions.getAcceptSize(), new DefaultThreadFactory("PAT-Boss"));
        workGroup = new NioEventLoopGroup(acceptOptions.getWorkerSize(), new DefaultThreadFactory("PAT-Work"));
        bootstrap = new ServerBootstrap().group(bossGroup, workGroup);
        bootstrap.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                if (sslContext != null) {
                    ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
                }

                ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                ch.pipeline().addLast(new Http2MultiplexHandler(new Http2StreamHandler()));

                ch.pipeline().addLast(new IdleStateHandler(2 * acceptOptions.getIdleTimeout(), 0, 0));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        if (evt instanceof IdleStateEvent) {
                            IdleStateEvent event = (IdleStateEvent) evt;
                            if (event.state() == IdleState.READER_IDLE) {
                                ctx.close();
                            }
                        } else {
                            super.userEventTriggered(ctx, evt);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void bind(InetSocketAddress serviceAddress, Function<String, ServiceConfig> serviceLookupper)
            throws Exception {
        messageProcessor = new FocusMessageProcessor(serviceLookupper, acceptOptions.getExecutorSelector());
        bootstrap.bind(serviceAddress).sync().channel();
    }

    @Override
    public void destroy() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
    }

    @Sharable
    public class Http2StreamHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
                if (headersFrame.isEndStream()) {
                    handle(ctx.channel(), headersFrame, null);
                } else {
                    ctx.channel().attr(HEADER_KEY).set(headersFrame);
                }
            } else if (msg instanceof Http2DataFrame) {
                Http2HeadersFrame headersFrame = ctx.channel().attr(HEADER_KEY).get();
                Http2DataFrame dataFrame = (Http2DataFrame) msg;
                try {
                    handle(ctx.channel(), headersFrame, dataFrame);
                } finally {
                    dataFrame.release();
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        private void handle(Channel channel, Http2HeadersFrame headersFrame, Http2DataFrame dataFrame) {
            messageProcessor.process(channel, headersFrame, dataFrame);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }

    }

}
