package com.dinstone.focus.client.locate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.client.ServiceLocator;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;

public abstract class AbstractServiceLocator implements ServiceLocator {

    private final AtomicInteger index = new AtomicInteger();

    @Override
    public ServiceInstance locate(Invocation invocation, List<ServiceInstance> exclusions) {
        // routing
        List<ServiceInstance> instances = routing(invocation, exclusions);
        // balance
        return balance(invocation, instances);
    }

    protected abstract List<ServiceInstance> routing(Invocation invocation, List<ServiceInstance> exclusions);

    protected ServiceInstance balance(Invocation invocation, List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        } else if (instances.size() == 1) {
            return instances.get(0);
        } else {
            int next = Math.abs(index.getAndIncrement());
            return instances.get(next % instances.size());
        }
    }

    @Override
    public void feedback(ServiceInstance instance, Invocation invocation, Object reply, Throwable error, long delay) {

    }

    @Override
    public void subscribe(String serviceName) {

    }

    @Override
    public void destroy() {

    }
}
