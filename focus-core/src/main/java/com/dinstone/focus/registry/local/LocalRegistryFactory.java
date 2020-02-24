package com.dinstone.focus.registry.local;

import com.dinstone.focus.registry.RegistryConfig;
import com.dinstone.focus.registry.RegistryFactory;
import com.dinstone.focus.registry.ServiceDiscovery;
import com.dinstone.focus.registry.ServiceRegistry;

public class LocalRegistryFactory implements RegistryFactory {

    @Override
    public String getSchema() {
        return "local";
    }

    @Override
    public ServiceRegistry createServiceRegistry(RegistryConfig registryConfig) {
//        registryConfig.
        return null;
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(RegistryConfig registryConfig) {
        return null;
    }

}
