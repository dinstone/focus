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
package com.dinstone.focus.transport.http2;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
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

public class Http2ChannelFactory {

    private static final DefaultHttp2PingFrame HEARTBEAT = new DefaultHttp2PingFrame(0);

    private final ConcurrentMap<InetSocketAddress, Http2Channel> channelMap;

    private EventLoopGroup workGroup;

    private Bootstrap bootstrap;

    public Http2ChannelFactory(Http2ConnectOptions connectOptions) {
        this.channelMap = new ConcurrentHashMap<>();

        try {
            init(connectOptions);
        } catch (SSLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void init(Http2ConnectOptions connectOptions) throws SSLException {
        final SslContext sslContext;
        if (connectOptions.isEnableSsl()) {
            SslProvider provider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL
                    : SslProvider.JDK;
            sslContext = SslContextBuilder.forClient().sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(
                            new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                                    SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2))
                    .build();
        } else {
            sslContext = null;
        }

        workGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                if (sslContext != null) {
                    ch.pipeline().addFirst(sslContext.newHandler(ch.alloc()));
                }

                Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forClient()
                        .initialSettings(Http2Settings.defaultSettings()).build();
                ch.pipeline().addLast(http2FrameCodec);
                ch.pipeline().addLast(new Http2MultiplexHandler(new ChannelInboundHandlerAdapter()));

                int idleTimeout = connectOptions.getIdleTimeout();
                ch.pipeline().addLast(new IdleStateHandler(2 * idleTimeout, idleTimeout, 0));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        if (evt instanceof IdleStateEvent) {
                            IdleStateEvent event = (IdleStateEvent) evt;
                            if (event.state() == IdleState.READER_IDLE) {
                                ctx.close();
                            } else if (event.state() == IdleState.WRITER_IDLE) {
                                ctx.writeAndFlush(HEARTBEAT);
                            }
                        } else {
                            super.userEventTriggered(ctx, evt);
                        }
                    }

                });
            }

        });
    }

    public Http2Channel create(InetSocketAddress socketAddress) {
        Http2Channel http2Channel = channelMap.get(socketAddress);
        if (http2Channel == null || !http2Channel.isActive()) {
            if (http2Channel != null) {
                http2Channel.destroy();
            }
            http2Channel = channelMap.computeIfAbsent(socketAddress, sa -> {
                try {
                    return createChannel(sa);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return http2Channel;
    }

    private Http2Channel createChannel(InetSocketAddress sa) throws Exception {
        ChannelFuture channelFuture = bootstrap.connect(sa).awaitUninterruptibly();
        if (!channelFuture.isDone()) {
            throw new ConnectException("Connect Timeout Exception : " + sa);
        }

        if (channelFuture.isCancelled()) {
            throw new ConnectException("Connect Cancelled Exception : " + sa);
        }

        if (!channelFuture.isSuccess()) {
            if (channelFuture.cause() instanceof ConnectException) {
                throw (ConnectException) channelFuture.cause();
            } else {
                throw new ConnectException("Connect Exception : " + sa + " " + channelFuture.cause());
            }
        }

        return new Http2Channel(channelFuture.channel());
    }

    public void destroy() {
        channelMap.forEach((k, v) -> v.destroy());
        workGroup.shutdownGracefully();
    }

}
