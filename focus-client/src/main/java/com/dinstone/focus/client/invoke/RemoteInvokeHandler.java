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
package com.dinstone.focus.client.invoke;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.transport.ConnectionFactory;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.codec.ProtocolCodec;
import com.dinstone.focus.compress.Compressor;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.AsyncReply;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.serialize.Serializer;
import com.dinstone.photon.Connection;
import com.dinstone.photon.message.Request;
import com.dinstone.photon.message.Response;

public class RemoteInvokeHandler implements InvokeHandler {

	private static final AtomicInteger IDGENER = new AtomicInteger();

	private ServiceConfig serviceConfig;

	private ProtocolCodec protocolCodec;

	private ConnectionFactory connectionFactory;

	public RemoteInvokeHandler(ServiceConfig serviceConfig, ProtocolCodec protocolCodec,
			ConnectionFactory connectionFactory) {
		this.serviceConfig = serviceConfig;
		this.protocolCodec = protocolCodec;
		this.connectionFactory = connectionFactory;
	}

	@Override
	public Reply invoke(Call call) throws Exception {
		ServiceInstance instance = call.context().get("service.instance");
		if (instance == null) {
			throw new ConnectException("can't find a service instance to connect");
		}

		call.attach().put("provider.endpoint", instance.getEndpointCode());
		call.attach().put(Serializer.SERIALIZER_KEY, serviceConfig.getSerializerId());
		call.attach().put(Compressor.COMPRESSOR_KEY, serviceConfig.getCompressorId());

		MethodConfig methodConfig = serviceConfig.getMethodConfig(call.getMethod());
		if (methodConfig.isAsyncInvoke()) {
			return async(call, instance, methodConfig);
		} else {
			return sync(call, instance, methodConfig);
		}
	}

	private Reply sync(Call call, ServiceInstance instance, MethodConfig mi) throws Exception {
		// process request
		Request request = protocolCodec.encode(call, mi.getParamType());
		request.setMsgId(IDGENER.incrementAndGet());

		Connection connection = connectionFactory.create(instance.getServiceAddress());
		Response response = connection.sync(request);
		return protocolCodec.decode(response, mi.getReturnType());
	}

	private Reply async(Call call, ServiceInstance instance, MethodConfig mi) throws Exception {
		// process request
		Request request = protocolCodec.encode(call, mi.getParamType());
		request.setMsgId(IDGENER.incrementAndGet());

		CompletableFuture<Reply> replyFuture = new CompletableFuture<>();
		Connection connection = connectionFactory.create(instance.getServiceAddress());
//		connection.async(request).addListener(new GenericFutureListener<Future<Response>>() {
//
//			@Override
//			public void operationComplete(Future<Response> responseFuture) throws Exception {
//				if (responseFuture.isSuccess()) {
//					Reply reply = protocolCodec.decode(responseFuture.get(), mi.getReturnType());
//					replyFuture.complete(reply);
//				} else {
//					replyFuture.completeExceptionally(responseFuture.cause());
//				}
//			}
//		});
		replyFuture = connection.asyncRequest(request).thenApply((response) -> {
			return protocolCodec.decode(response, mi.getReturnType());
		});
		return new AsyncReply(replyFuture);
	}

}
