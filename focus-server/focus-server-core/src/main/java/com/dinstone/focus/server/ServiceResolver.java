package com.dinstone.focus.server;

import com.dinstone.focus.config.ServiceConfig;

public interface ServiceResolver {

	void registry(ServiceConfig serviceConfig);

	ServiceConfig lookup(String serviceName);

	void destroy();

}