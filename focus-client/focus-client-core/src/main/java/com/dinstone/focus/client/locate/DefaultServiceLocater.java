package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.protocol.Call;

public abstract class DefaultServiceLocater implements ServiceLocater {

	private final AtomicInteger index = new AtomicInteger();

	public DefaultServiceLocater() {
		super();
	}

	@Override
	public InetSocketAddress locate(Call call, InetSocketAddress selected) {
		try {
			// routing
			List<InetSocketAddress> addresses = routing(call, selected);
			// balance
			return balance(call, addresses);
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	@Override
	public void feedback(Call call, InetSocketAddress selected, boolean ok) {
	}

	@Override
	public void subscribe(String serviceName) {
	}

	@Override
	public void destroy() {
	}

	protected abstract List<InetSocketAddress> routing(Call call, InetSocketAddress selected);

	protected InetSocketAddress balance(Call call, List<InetSocketAddress> addresses) {
		if (addresses.size() == 0) {
			return null;
		} else if (addresses.size() == 1) {
			return addresses.get(0);
		} else {
			int next = Math.abs(index.getAndIncrement());
			return addresses.get(next % addresses.size());
		}
	}

}