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
package com.dinstone.focus.server.processor;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.codec.ErrorCodec;
import com.dinstone.focus.codec.RpcCodec;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Status;

import io.netty.channel.ChannelHandlerContext;

public final class RpcProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RpcProcessor.class);

    private final InvokeHandler invoker;

    public RpcProcessor(InvokeHandler invoker) {
        this.invoker = invoker;
    }

    public void process(ChannelHandlerContext ctx, Request request) {

        RpcException exception = null;
        try {
            RpcCodec codec = CodecManager.codec(request.getCodec());
            Reply reply = invoker.invoke(codec.decode(request));

            Response response = new Response();
            response.setMsgId(request.getMsgId());
            response.setCodec(request.getCodec());
            response.setStatus(Status.SUCCESS);
            codec.encode(response, reply);

            ctx.writeAndFlush(response);
        } catch (RpcException e) {
            exception = e;
        } catch (Throwable e) {
            exception = new RpcException(500, "service exception", e);
        }

        if (exception != null) {
            ErrorCodec codec = CodecManager.error();

            Response response = new Response();
            response.setMsgId(request.getMsgId());
            response.setCodec(request.getCodec());
            response.setStatus(Status.ERROR);
            codec.encode(response, exception);

            ctx.writeAndFlush(response);
        }
    }

    public void process(ChannelHandlerContext ctx, Notice msg) {
        // TODO Auto-generated method stub

    }

}