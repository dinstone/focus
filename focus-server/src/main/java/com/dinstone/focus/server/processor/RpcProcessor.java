/*
 * Copyright (C) 2018~2020 dinstone<dinstone@163.com>
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
import com.dinstone.focus.codec.Codec;
import com.dinstone.focus.codec.CodecManager;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Reply;
import com.dinstone.focus.server.transport.AcceptorFactory;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.handler.MessageContext;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Notice;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Status;

public final class RpcProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptorFactory.class);

    private final InvokeHandler invoker;

    public RpcProcessor(InvokeHandler invoker) {
        this.invoker = invoker;
    }

    public void process(MessageContext context, Notice notice) {
        LOG.info("notice is {}", notice.getContent());
    }

    public void process(MessageContext context, Request request) {
        String cname = request.getHeaders().get("rpc.codec");
        Codec codec = CodecManager.find(cname);

        RpcException exception = null;
        try {
            Reply reply = invoker.invoke(codec.decode(request));

            Response response = new Response();
            response.setId(request.getId());
            response.setStatus(Status.SUCCESS);

            Headers headers = new Headers();
            headers.put("rpc.codec", cname);
            response.setHeaders(headers);

            codec.encode(response, reply);

            context.getConnection().write(response);
        } catch (RpcException e) {
            exception = e;
        } catch (Throwable e) {
            exception = new RpcException(500, "service exception", e);
        }

        if (exception != null) {
            Response response = new Response();
            response.setId(request.getId());
            response.setStatus(Status.ERROR);

            Headers headers = new Headers();
            response.setHeaders(headers);

            CodecManager.getErrorCodec().encode(response, exception);

            context.getConnection().write(response);
        }
    }

}