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
package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;

public class DirectServiceLocater implements ServiceLocater {

	private final AtomicInteger index = new AtomicInteger();

	private List<InetSocketAddress> serviceAddresses = new ArrayList<InetSocketAddress>();

	public DirectServiceLocater(List<InetSocketAddress> connectAddresses) {
		if (connectAddresses == null || connectAddresses.isEmpty()) {
			throw new FocusException("connectAddresses is empty, please set connectAddresses");
		}
		this.serviceAddresses.addAll(connectAddresses);
	}

	private List<ServiceInstance> routing(ServiceInstance selected, List<ServiceInstance> instances) {
		List<ServiceInstance> sds = new ArrayList<ServiceInstance>();
		for (ServiceInstance instance : instances) {
			if (instance == selected) {
				continue;
			}
			sds.add(instance);
		}
		return sds;
	}

	@Override
	public InetSocketAddress locate(Call call, InetSocketAddress selected) {
		List<InetSocketAddress> addresses = new ArrayList<>();
		// routing
		for (InetSocketAddress address : serviceAddresses) {
			if (address.equals(selected)) {
				continue;
			}
			addresses.add(address);
		}

		// balance
		if (addresses.size() == 0) {
			return null;
		} else if (addresses.size() == 1) {
			return addresses.get(0);
		} else {
			int next = Math.abs(index.getAndIncrement());
			return addresses.get(next % addresses.size());
		}
	}

	@Override
	public void subscribe(String serviceName) {

	}

	@Override
	public void destroy() {

	}

}
