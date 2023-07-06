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
package com.dinstone.focus.transport.photon;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.CodecException;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.focus.transport.Connector;
import com.dinstone.photon.Connection;
import com.dinstone.photon.message.Headers;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;
import com.dinstone.photon.message.Response.Status;

import io.netty.util.CharsetUtil;

public class PhotonConnector implements Connector {

	private static final AtomicInteger IDGENER = new AtomicInteger();

	private final PhotonConnectionFactory factory;

	public PhotonConnector(PhotonConnectOptions connectOptions) {
		if (connectOptions == null) {
			throw new IllegalArgumentException("connectOptions is null");
		}
		this.factory = new PhotonConnectionFactory(connectOptions);
	}

	@Override
	public CompletableFuture<Reply> send(Call call, ServiceConfig serviceConfig, InetSocketAddress socketAddress)
			throws Exception {
		// create connection
		Connection connection = factory.create(socketAddress);

		MethodConfig methodConfig = serviceConfig.getMethodConfig(call.getMethod());
		// process request
		Request request = encode(call, serviceConfig, methodConfig);

		return connection.sendRequest(request).thenApplyAsync((response) -> {
			// process response
			return decode(response, serviceConfig, methodConfig);
		});
	}

	private Reply decode(Response response, ServiceConfig serviceConfig, MethodConfig methodConfig) {
		Reply reply = new Reply();
		Headers headers = response.headers();
		if (response.getStatus() == Status.SUCCESS) {
			Object value;
			byte[] content = response.getContent();
			if (content == null || content.length == 0) {
				value = null;
			} else {
				String compressorType = headers.get(Compressor.TYPE_KEY);
				Compressor compressor = serviceConfig.getCompressor();
				if (compressor != null && compressorType != null) {
					try {
						content = compressor.decode(content);
					} catch (IOException e) {
						throw new CodecException("compress decode error: " + methodConfig.getMethodName(), e);
					}
				}

				try {
					Serializer serializer = serviceConfig.getSerializer();
					Class<?> contentType = methodConfig.getReturnType();
					value = serializer.decode(content, contentType);
				} catch (IOException e) {
					throw new CodecException("serialize decode error: " + methodConfig.getMethodName(), e);
				}
			}
			reply.value(value);
		} else {
			int code = headers.getInt(InvokeException.CODE_KEY, 0);

			InvokeException error;
			byte[] encoded = response.getContent();
			if (encoded == null || encoded.length == 0) {
				error = new InvokeException(code, "unkown exception");
			} else {
				String message = new String(encoded, CharsetUtil.UTF_8);
				error = new InvokeException(code, message);
			}
			reply.error(error);
		}
		reply.attach().putAll(headers);
		return reply;
	}

	private Request encode(Call call, ServiceConfig serviceConfig, MethodConfig methodConfig) {
		byte[] content = null;
		if (call.getParameter() != null) {
			try {
				Serializer serializer = serviceConfig.getSerializer();
				content = serializer.encode(call.getParameter(), methodConfig.getParamType());
				call.attach().put(Serializer.TYPE_KEY, serializer.serializerType());
			} catch (IOException e) {
				throw new CodecException("serialize encode error: " + methodConfig.getMethodName(), e);
			}

			Compressor compressor = serviceConfig.getCompressor();
			if (compressor != null && content.length > serviceConfig.getCompressThreshold()) {
				try {
					content = compressor.encode(content);
					call.attach().put(Compressor.TYPE_KEY, compressor.compressorType());
				} catch (IOException e) {
					throw new CodecException("compress encode error: " + methodConfig.getMethodName(), e);
				}
			}
		}

		Request request = new Request();
		request.setMsgId(IDGENER.incrementAndGet());
		Headers headers = request.headers();
		headers.add(Call.CONSUMER_KEY, call.getConsumer());
		headers.add(Call.PROVIDER_KEY, call.getProvider());
		headers.add(Call.SERVICE_KEY, call.getService());
		headers.add(Call.METHOD_KEY, call.getMethod());
		request.setTimeout(call.getTimeout());
		headers.setAll(call.attach());
		request.setContent(content);
		return request;
	}

	@Override
	public void destroy() {
		factory.destroy();
	}

}
