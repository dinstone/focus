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
package com.dinstone.focus.codec.photon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Attach;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.utils.ByteStreamUtil;

public class PhotonProtocolCodec implements ProtocolCodec {

    private Serializer serializer;
    private Compressor compressor;
    private int compressThreshold;

    public PhotonProtocolCodec(Serializer serializer, Compressor compressor, int compressThreshold) {
        this.serializer = serializer;
        this.compressor = compressor;
        this.compressThreshold = compressThreshold;
    }

    @Override
    public Request encode(Call call, Class<?> paramType) throws CodecException {
        byte[] content = encodeData(call.attach(), call.getParameter(), paramType);

        Request request = new Request();
        Headers headers = request.headers();
        headers.add(Call.GROUP_KEY, call.getGroup());
        headers.add(Call.SERVICE_KEY, call.getService());
        headers.add(Call.METHOD_KEY, call.getMethod());
        headers.setAll(call.attach());

        request.setTimeout(call.getTimeout());
        request.setContent(content);
        return request;
    }

    @Override
    public Call decode(Request request, Class<?> paramType) throws CodecException {
        Headers headers = request.headers();
        Object content = decodeData(headers, request.getContent(), paramType);
        Call call = new Call();
        call.setGroup(headers.get(Call.GROUP_KEY));
        call.setService(headers.get(Call.SERVICE_KEY));
        call.setMethod(headers.get(Call.METHOD_KEY));
        call.setTimeout(request.getTimeout());
        call.attach().putAll(headers);
        call.setParameter(content);
        return call;
    }

    @Override
    public Response encode(Reply reply, Class<?> returnType) throws CodecException {
        Response response = new Response();
        if (reply.getData() instanceof InvokeException) {
            response.setStatus(Status.FAILURE);
            response.setContent(encodeError((InvokeException) reply.getData()));
        } else {
            response.setStatus(Status.SUCCESS);
            response.setContent(encodeData(reply.attach(), reply.getData(), returnType));
        }
        response.headers().setAll(reply.attach());
        return response;
    }

    @Override
    public Reply decode(Response response, Class<?> returnType) throws CodecException {
        Headers headers = response.headers();

        Reply reply = new Reply();
        if (response.getStatus() == Status.SUCCESS) {
            reply.setData(decodeData(headers, response.getContent(), returnType));
        } else {
            reply.setData(decodeError(response.getContent()));
        }
        reply.attach().putAll(headers);
        return reply;
    }

    private byte[] encodeError(InvokeException exception) {
        try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ByteStreamUtil.writeInt(bao, exception.getCode());
            ByteStreamUtil.writeString(bao, exception.getMessage());
            return bao.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    private InvokeException decodeError(byte[] encoded) {
        try {
            if (encoded != null) {
                ByteArrayInputStream bai = new ByteArrayInputStream(encoded);
                int code = ByteStreamUtil.readInt(bai);
                String message = ByteStreamUtil.readString(bai);
                return new InvokeException(code, message);
            }
            return new InvokeException(99, "unkown exception");
        } catch (IOException e) {
            return new InvokeException(199, e);
        }
    }

    private byte[] encodeData(Attach attach, Object content, Class<?> contentType) {
        if (content == null) {
            return null;
        }

        byte[] cs;
        try {
            cs = serializer.encode(content, contentType);
            attach.put(Serializer.TYPE_KEY, serializer.serializerType());
        } catch (IOException e) {
            throw new CodecException("serialize encode error", e);
        }

        if (compressor != null && cs != null && cs.length > compressThreshold) {
            try {
                cs = compressor.encode(cs);
                attach.put(Compressor.TYPE_KEY, compressor.compressorType());
            } catch (IOException e) {
                throw new CodecException("compress encode error", e);
            }
        }
        return cs;
    }

    private Object decodeData(Headers headers, byte[] content, Class<?> contentType) {
        if (content == null || content.length == 0) {
            return null;
        }

        String compressorType = headers.get(Compressor.TYPE_KEY);
        if (compressor != null && compressorType != null) {
            try {
                content = compressor.decode(content);
            } catch (IOException e) {
                throw new CodecException("compress decode error", e);
            }
        }

        try {
            return serializer.decode(content, contentType);
        } catch (IOException e) {
            throw new CodecException("serialize decode error", e);
        }
    }

}
