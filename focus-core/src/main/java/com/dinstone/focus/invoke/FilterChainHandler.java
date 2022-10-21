package com.dinstone.focus.invoke;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.FilterContext;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class FilterChainHandler implements InvokeHandler {

    protected ServiceConfig serviceConfig;

    protected FilterContext filterChain;

    public FilterChainHandler(ServiceConfig serviceConfig, InvokeHandler invokeHandler) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }

        this.serviceConfig = serviceConfig;
        this.filterChain = new FilterContext(serviceConfig, null, new Filter() {

            @Override
            public CompletableFuture<Reply> invoke(FilterContext next, Call call) throws Exception {
                return invokeHandler.invoke(call);
            }
        });
    }

    public FilterChainHandler addFilter(Filter... filters) {
        return addFilter(Arrays.asList(filters));
    }

    public FilterChainHandler addFilter(List<Filter> filters) {
        if (filters != null) {
            for (ListIterator<Filter> iterator = filters.listIterator(filters.size()); iterator.hasPrevious();) {
                filterChain = new FilterContext(serviceConfig, filterChain, iterator.previous());
            }
        }
        return this;
    }

    @Override
    public CompletableFuture<Reply> invoke(Call call) throws Exception {
        return filterChain.invoke(call);
    }
}
