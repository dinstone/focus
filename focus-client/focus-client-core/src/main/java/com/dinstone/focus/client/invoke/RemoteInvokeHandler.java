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
package com.dinstone.focus.client.invoke;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.transport.Connector;

public class RemoteInvokeHandler implements Handler {

	private Connector connector;

	private ServiceConfig serviceConfig;

	private ServiceLocater serivceLocater;

	private int connectRetry;

	public RemoteInvokeHandler(Connector connector, ServiceConfig serviceConfig, ServiceLocater serivceLocater) {
		this.connector = connector;
		this.serviceConfig = serviceConfig;
		this.serivceLocater = serivceLocater;

		this.connectRetry = serviceConfig.getRetry();
	}

	@Override
	public CompletableFuture<Reply> handle(Call call) throws Exception {
		InetSocketAddress selected = null;
		// find an address
		for (int i = 0; i < connectRetry; i++) {
			selected = serivceLocater.locate(call, selected);

			// check
			if (selected == null) {
				continue;
			}

			try {
				return connector.send(call, serviceConfig, selected);
			} catch (ConnectException e) {
				// ignore and retry
			} catch (Exception e) {
				throw e;
			}
		}

		throw new FocusException("can't find a live service instance for " + call.getService());
	}

}
