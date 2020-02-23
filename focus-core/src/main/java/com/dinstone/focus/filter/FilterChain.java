package com.dinstone.focus.filter;

import java.util.Arrays;
import java.util.List;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public class FilterChain implements InvokeHandler {

    private FilterHandler filterHandler;

    public FilterChain(InvokeHandler invokeHandler, Filter... filters) {
        this(invokeHandler, Arrays.asList(filters));
    }

    public FilterChain(InvokeHandler invokeHandler, List<Filter> filters) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }
        filterHandler = new FilterHandler(null, invokeHandler);

        if (filters != null && !filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                filterHandler = new FilterHandler(filters.get(i), filterHandler);
            }
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        return filterHandler.invoke(call);
    }

}
