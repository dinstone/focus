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

import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.photon.Acceptor;
import com.dinstone.photon.ExchangeException;
import com.dinstone.photon.codec.ExceptionCodec;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Status;
import com.dinstone.photon.processor.MessageProcessor;

import io.netty.channel.ChannelHandlerContext;

public class AcceptorFactory {

    private ServerOptions serverOption;

    public AcceptorFactory(ServerOptions serverOption) {
        this.serverOption = serverOption;
    }

    public Acceptor create(final InvokeHandler invoker) {
        Acceptor acceptor = new Acceptor(serverOption.getAcceptOptions());
        acceptor.setMessageProcessor(new MessageProcessor() {

            @Override
            public void process(ChannelHandlerContext ctx, Notice notice) {
                // rpcProcessor.process(ctx, notice);
            }

            @Override
            public void process(ChannelHandlerContext ctx, Request request) {
                Call call = null;
                ExchangeException exception = null;
                try {
                    // decode call from request
                    call = CodecManager.decode(request);

                    // invoke call
                    Reply reply = invoker.invoke(call);

                    Response response = new Response();
                    response.setMsgId(request.getMsgId());
                    response.setStatus(Status.SUCCESS);
                    response.setCodec(request.getCodec());
                    // encode reply to response
                    CodecManager.encode(response, reply);
                    // send response
                    ctx.writeAndFlush(response);
                } catch (NoSuchMethodException e) {
                    String message = "unkown method: [" + call.getGroup() + "]" + call.getService() + "."
                            + call.getMethod();
                    exception = new ExchangeException(405, message, e);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    String message = "illegal access: [" + call.getGroup() + "]" + call.getService() + "."
                            + call.getMethod();
                    exception = new ExchangeException(502, message, e);
                } catch (Throwable e) {
                    String message = "service exception: [" + call.getGroup() + "]" + call.getService() + "."
                            + call.getMethod();
                    exception = new ExchangeException(509, message, e);
                }

                if (exception != null) {
                    Response response = new Response();
                    response.setMsgId(request.getMsgId());
                    response.setStatus(Status.FAILURE);
                    response.setContent(ExceptionCodec.encode(exception));

                    ctx.writeAndFlush(response);
                }
            }
        });
        return acceptor;
    }

}
