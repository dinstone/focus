/*
 * Copyright (C) 2019~2020 dinstone<dinstone@163.com>
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
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.server.ExecutorSelector;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.photon.Acceptor;
import com.dinstone.photon.ExchangeException;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.processor.MessageProcessor;
import com.dinstone.photon.util.ExceptionUtil;

import io.netty.channel.ChannelHandlerContext;

public class AcceptorFactory {

    private ServerOptions serverOption;

    public AcceptorFactory(ServerOptions serverOption) {
        this.serverOption = serverOption;
    }

    public Acceptor create(final ImplementBinding binding) {
        Acceptor acceptor = new Acceptor(serverOption.getAcceptOptions());
        acceptor.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof Request) {
                    Request request = (Request) msg;
                    Executor exe = null;
                    ExecutorSelector selector = serverOption.getExecutorSelector();
                    if (selector != null) {
                        String g = request.getHeaders().get("rpc.call.group");
                        String s = request.getHeaders().get("rpc.call.service");
                        String m = request.getHeaders().get("rpc.call.method");
                        exe = selector.select(g, s, m);
                    }
                    if (exe != null) {
                        exe.execute(new Runnable() {

                            @Override
                            public void run() {
                                invoke(binding, ctx, request);
                            }
                        });
                    } else {
                        invoke(binding, ctx, request);
                    }
                }

                // long s = System.currentTimeMillis();
                // executor.execute(new Runnable() {
                //
                // @Override
                // public void run() {
                // long e = System.currentTimeMillis();
                // if (request.getTimeout() > 0 && e - s >= request.getTimeout()) {
                // // timeout
                // return;
                // }
                //
                // }
                // });
            }

            private void invoke(final ImplementBinding binding, ChannelHandlerContext ctx, Request request) {
                ExchangeException exception = null;
                try {
                    // decode call from request
                    Call call = CodecManager.decode(request);

                    ServiceConfig wrapper = binding.lookup(call.getService(), call.getGroup());
                    if (wrapper == null) {
                        throw new NoSuchMethodException(
                                "unkown service: " + call.getService() + "[" + call.getGroup() + "]");
                    }

                    // invoke call
                    Reply reply = wrapper.getHandler().invoke(call);

                    Response response = new Response();
                    response.setMsgId(request.getMsgId());
                    response.setStatus(Status.SUCCESS);
                    response.setCodec(request.getCodec());
                    // encode reply to response
                    CodecManager.encode(response, reply);
                    // send response with reply
                    ctx.writeAndFlush(response);
                } catch (NoSuchMethodException e) {
                    String message = ExceptionUtil.getMessage(e);
                    exception = new ExchangeException(104, message, e);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    String message = ExceptionUtil.getMessage(e);
                    exception = new ExchangeException(103, message, e);
                } catch (Throwable e) {
                    String message = ExceptionUtil.getMessage(e);
                    exception = new ExchangeException(109, message, e);
                }

                if (exception != null) {
                    Response response = new Response();
                    response.setMsgId(request.getMsgId());
                    response.setStatus(Status.FAILURE);
                    response.setContent(ExceptionCodec.encode(exception));
                    // send response with exception
                    ctx.writeAndFlush(response);
                }
            }
        });
        return acceptor;
    }

}
