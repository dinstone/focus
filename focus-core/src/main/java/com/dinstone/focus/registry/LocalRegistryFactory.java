package com.dinstone.focus.registry;

public class LocalRegistryFactory implements RegistryFactory {

    @Override
    public String getSchema() {
        return "local";
    }

    @Override
    public ServiceRegistry createServiceRegistry(RegistryConfig registryConfig) {
        return null;
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(RegistryConfig registryConfig) {
        return null;
    }

}
