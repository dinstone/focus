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

import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;

public class DirectLinkServiceLocater extends DefaultServiceLocater {

	private List<InetSocketAddress> connectAddresses = new ArrayList<InetSocketAddress>();

	public DirectLinkServiceLocater(List<InetSocketAddress> connectAddresses) {
		if (connectAddresses == null || connectAddresses.isEmpty()) {
			throw new FocusException("connectAddresses is empty, please set connectAddresses");
		}
		this.connectAddresses.addAll(connectAddresses);
	}

	@Override
	protected List<InetSocketAddress> routing(Call call, InetSocketAddress selected) {
		if (selected == null) {
			return connectAddresses;
		}

		List<InetSocketAddress> addresses = new ArrayList<>();
		for (InetSocketAddress address : connectAddresses) {
			if (address.equals(selected)) {
				continue;
			}
			addresses.add(address);
		}
		return addresses;
	}

}
