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
import java.util.ServiceLoader;

import com.dinstone.focus.codec.CodecException;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.compress.CompressorFactory;
import com.dinstone.focus.endpoint.EndpointOptions;
import com.dinstone.focus.exception.ExceptionUtil;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Attach;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.serialize.SerializerFactory;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;
import com.dinstone.photon.utils.ByteStreamUtil;

public class PhotonProtocolCodec implements ProtocolCodec {

    public PhotonProtocolCodec(EndpointOptions<?> endpointOptions) {
        // init serializer
        ServiceLoader<SerializerFactory> sfLoader = ServiceLoader.load(SerializerFactory.class);
        for (SerializerFactory serializerFactory : sfLoader) {
            SerializerFactory.regist(serializerFactory.create());
        }
        // init compressor
        ServiceLoader<CompressorFactory> cfLoader = ServiceLoader.load(CompressorFactory.class);
        for (CompressorFactory compressorFactory : cfLoader) {
            CompressorFactory.regist(compressorFactory.create(endpointOptions.getCompressThreshold()));
        }
    }

    @Override
    public Request encode(Call call, Class<?> paramType) throws CodecException {
        byte[] content = encodeContent(call.attach(), call.getParameter(), paramType);

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
        Object content = decodeContent(headers, request.getContent(), paramType);
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
            response.setContent(encodeException((InvokeException) reply.getData()));
        } else {
            response.setStatus(Status.SUCCESS);
            response.setContent(encodeContent(reply.attach(), reply.getData(), returnType));
        }
        response.headers().setAll(reply.attach());
        return response;
    }

    @Override
    public Reply decode(Response response, Class<?> returnType) throws CodecException {
        Headers headers = response.headers();

        Reply reply = new Reply();
        if (response.getStatus() == Status.SUCCESS) {
            reply.setData(decodeContent(headers, response.getContent(), returnType));
        } else {
            reply.setData(decodeException(response.getContent()));
        }
        reply.attach().putAll(headers);
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
            return new InvokeException(199, e);
        }
    }

    private Object decodeContent(Headers headers, byte[] content, Class<?> contentType) {
        if (content == null || content.length == 0) {
            return null;
        }

        String sid = headers.get(Serializer.HEADER_KEY);
        Serializer serializer = SerializerFactory.lookup(sid);
        if (serializer == null) {
            throw new CodecException("can't not find serializer");
        }

        String cid = headers.get(Compressor.HEADER_KEY);
        Compressor compressor = CompressorFactory.lookup(cid);
        if (compressor != null) {
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

    private byte[] encodeContent(Attach attach, Object content, Class<?> contentType) {
        if (content == null) {
            return null;
        }

        String sid = attach.get(Serializer.HEADER_KEY);
        Serializer serializer = SerializerFactory.lookup(sid);
        if (serializer == null) {
            throw new CodecException("can't not find serializer : " + sid);
        }

        byte[] cs;
        try {
            cs = serializer.encode(content, contentType);
        } catch (IOException e) {
            throw new CodecException("serialize encode error", e);
        }

        String cid = attach.get(Compressor.HEADER_KEY);
        Compressor compressor = CompressorFactory.lookup(cid);
        if (compressor != null && compressor.enable(cs)) {
            try {
                cs = compressor.encode(cs);
            } catch (IOException e) {
                throw new CodecException("compress encode error", e);
            }
        } else {
            attach.remove(Compressor.HEADER_KEY);
        }
        return cs;
    }

}
