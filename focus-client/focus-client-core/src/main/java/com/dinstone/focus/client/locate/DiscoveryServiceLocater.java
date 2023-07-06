package com.dinstone.focus.client.locate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceInstance;
import com.dinstone.focus.exception.FocusException;
import com.dinstone.focus.protocol.Call;

public class DiscoveryServiceLocater extends DefaultServiceLocater {

	private ServiceDiscovery serviceDiscovery;

	public DiscoveryServiceLocater(ClutchOptions clutchOptions) {
		super();
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

	protected List<InetSocketAddress> routing(Call call, InetSocketAddress selected) {
		List<InetSocketAddress> addresses = new ArrayList<>();

		try {
			Collection<ServiceInstance> serviceInstances = serviceDiscovery.discovery(call.getProvider());
			// routing
			for (ServiceInstance instance : serviceInstances) {
				if (instance.getSocketAddress().equals(selected)) {
					continue;
				}
				addresses.add(instance.getSocketAddress());
			}
		} catch (Exception e) {
			// igonre
		}

		return addresses;
	}

}
