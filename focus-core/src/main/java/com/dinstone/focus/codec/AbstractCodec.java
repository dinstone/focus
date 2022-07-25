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
package com.dinstone.focus.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.dinstone.focus.exception.ExceptionUtil;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.utils.ByteStreamUtil;

public abstract class AbstractCodec implements ProtocolCodec {

    @Override
    public Request encode(Call call, Class<?> paramType) throws CodecException {
        Request request = new Request();
        Headers headers = request.headers();
        headers.add(Call.GROUP_KEY, call.getGroup());
        headers.add(Call.SERVICE_KEY, call.getService());
        headers.add(Call.METHOD_KEY, call.getMethod());
        headers.add(ProtocolCodec.CODEC_KEY, codecId());
        headers.setAll(call.attach());

        request.setTimeout(call.getTimeout());
        request.setContent(encodeContent(call.getParameter(), paramType));
        return request;
    }

    @Override
    public Call decode(Request request, Class<?> paramType) throws CodecException {
        Call call = new Call();
        Headers headers = request.headers();
        call.setGroup(headers.get(Call.GROUP_KEY));
        call.setService(headers.get(Call.SERVICE_KEY));
        call.setMethod(headers.get(Call.METHOD_KEY));
        call.setTimeout(request.getTimeout());
        call.attach().putAll(headers);
        call.setParameter(decodeContent(request.getContent(), paramType));
        return call;
    }

    @Override
    public Response encode(Reply reply, Class<?> returnType) throws CodecException {
        Response response = new Response();
        response.headers().setAll(reply.attach());
        response.headers().add(ProtocolCodec.CODEC_KEY, codecId());
        if (reply.getData() instanceof InvokeException) {
            response.setStatus(Status.FAILURE);
            response.setContent(encodeException((InvokeException) reply.getData()));
        } else {
            response.setStatus(Status.SUCCESS);
            response.setContent(encodeContent(reply.getData(), returnType));
        }
        return response;
    }

    @Override
    public Reply decode(Response response, Class<?> returnType) throws CodecException {
        Reply reply = new Reply();
        Headers headers = response.headers();
        if (!headers.isEmpty()) {
            reply.attach().putAll(headers);
        }

        if (response.getStatus() == Status.SUCCESS) {
            Object data = decodeContent(response.getContent(), returnType);
            reply.setData(data);
        } else {
            reply.setData(decodeException(response.getContent()));
        }
        return reply;
    }

    private byte[] encodeException(InvokeException exception) {
        try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ByteStreamUtil.writeInt(bao, exception.getCode());
            ByteStreamUtil.writeString(bao, exception.getMessage());
            if (exception.getDetail() != null) {
                ByteStreamUtil.writeString(bao, exception.getDetail());
            } else if (exception.getCause() != null) {
                ByteStreamUtil.writeString(bao, ExceptionUtil.getStackTrace(exception.getCause()));
            }
            return bao.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    private InvokeException decodeException(byte[] encoded) {
        try {
            if (encoded != null) {
                ByteArrayInputStream bai = new ByteArrayInputStream(encoded);
                int code = ByteStreamUtil.readInt(bai);
                String message = ByteStreamUtil.readString(bai);
                String details = ByteStreamUtil.readString(bai);
                return new InvokeException(code, message, details);
            }
            return new InvokeException(99, "unkown exception");
        } catch (IOException e) {
            return new InvokeException(199, "decode exception error", e);
        }
    }

    protected abstract Object decodeContent(byte[] contentBytes, Class<?> contentType);

    protected abstract byte[] encodeContent(Object content, Class<?> contentType);

}
