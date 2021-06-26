/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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
package com.dinstone.focus.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.dinstone.focus.exception.ExceptionHelper;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Attach;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.util.ByteStreamUtil;

public abstract class AbstractCodec implements ProtocolCodec {

    @Override
    public Request encode(Call call) throws CodecException {
        Request request = new Request();
        Headers headers = request.headers();
        headers.put("rpc.call.group", call.getGroup());
        headers.put("rpc.call.service", call.getService());
        headers.put("rpc.call.method", call.getMethod());

        // request.setMsgId(IDGENER.incrementAndGet());
        request.setTimeout(call.getTimeout());
        request.setCodec(codecId());
        request.setContent(writeCall(call));
        return request;
    }

    @Override
    public Call decode(Request request) throws CodecException {
        Call call = readCall(request.getContent());

        Headers headers = request.getHeaders();
        call.setGroup(headers.get("rpc.call.group"));
        call.setService(headers.get("rpc.call.service"));
        call.setMethod(headers.get("rpc.call.method"));
        call.attach(new Attach(headers));
        call.setTimeout(request.getTimeout());
        return call;
    }

    @Override
    public Response encode(Reply reply) throws CodecException {
        Response response = new Response();
        response.headers().putAll(reply.attach());
        response.setCodec(codecId());
        if (reply.isError()) {
            response.headers().put("rpc.reply.error", "true");
            response.setContent(writeError(reply));
        } else {
            response.setContent(writeReply(reply));
        }
        return response;
    }

    @Override
    public Reply decode(Response response) throws CodecException {
        Reply reply = null;

        Headers headers = response.getHeaders();
        if (headers != null && Boolean.valueOf(headers.get("rpc.reply.error"))) {
            reply = readError(response.getContent());
        } else {
            reply = readReply(response.getContent());
        }
        if (headers != null) {
            reply.attach(new Attach(headers));
        }

        return reply;
    }

    private byte[] writeError(Reply reply) {
        try {
            Throwable exception = (Throwable) reply.getData();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ByteStreamUtil.writeString(bao, exception.getMessage());
            ByteStreamUtil.writeString(bao, ExceptionHelper.getStackTrace(exception));
            return bao.toByteArray();
        } catch (IOException e) {
            throw new CodecException("encode error message failure", e);
        }
    }

    private Reply readError(byte[] content) {
        try {
            Reply reply = new Reply();
            ByteArrayInputStream bai = new ByteArrayInputStream(content);
            String message = ByteStreamUtil.readString(bai);
            String stackTrace = ByteStreamUtil.readString(bai);
            reply.setData(new FocusException(message, stackTrace));
            return reply;
        } catch (IOException e) {
            throw new CodecException("decode error message failure", e);
        }
    }

    protected abstract byte[] writeCall(Call call) throws CodecException;

    protected abstract Call readCall(byte[] content) throws CodecException;

    protected abstract byte[] writeReply(Reply reply) throws CodecException;

    protected abstract Reply readReply(byte[] content) throws CodecException;

}
