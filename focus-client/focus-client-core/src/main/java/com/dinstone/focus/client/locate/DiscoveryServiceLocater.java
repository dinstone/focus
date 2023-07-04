package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;

public class DiscoveryServiceLocater implements ServiceLocater {

	private final AtomicInteger index = new AtomicInteger();

	private ServiceDiscovery serviceDiscovery;

	public DiscoveryServiceLocater(ClutchOptions clutchOptions) {
		ServiceLoader<ClutchFactory> serviceLoader = ServiceLoader.load(ClutchFactory.class);
		for (ClutchFactory clutchFactory : serviceLoader) {
			if (clutchFactory.appliable(clutchOptions)) {
				serviceDiscovery = clutchFactory.createServiceDiscovery(clutchOptions);
				break;
			}
		}
		if (serviceDiscovery == null) {
			throw new FocusException("can't find discovery implement for " + clutchOptions);
		}
	}

	@Override
	public InetSocketAddress locate(Call call, InetSocketAddress selected) {
		try {
			List<InetSocketAddress> addresses = new ArrayList<>();

			Collection<ServiceInstance> serviceInstances = serviceDiscovery.discovery(call.getTarget());
			// routing
			for (ServiceInstance instance : serviceInstances) {
				if (instance.getSocketAddress().equals(selected)) {
					continue;
				}
				addresses.add(instance.getSocketAddress());
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
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	@Override
	public void subscribe(String serviceName) {
		try {
			serviceDiscovery.subscribe(serviceName);
		} catch (Exception e) {
			throw new FocusException("subscribe " + serviceName + " error", e);
		}
	}

	@Override
	public void destroy() {
		serviceDiscovery.destroy();
	}

}
