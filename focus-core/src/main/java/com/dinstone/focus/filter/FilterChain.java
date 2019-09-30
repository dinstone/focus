package com.dinstone.focus.filter;

import java.util.Arrays;
import java.util.List;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public class FilterChain implements InvokeHandler {

    private FilterInvokeHandler filterInvokeHandler;

    public FilterChain(InvokeHandler invokeHandler, Filter... filters) {
        this(invokeHandler, Arrays.asList(filters));
    }

    public FilterChain(InvokeHandler invokeHandler, List<Filter> filters) {
        if (invokeHandler == null) {
            throw new IllegalArgumentException("invokeHandler is null");
        }
        filterInvokeHandler = new FilterInvokeHandler(null, invokeHandler);

        if (filters != null && !filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                filterInvokeHandler = new FilterInvokeHandler(filters.get(i), filterInvokeHandler);
            }
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        return filterInvokeHandler.invoke(call);
    }

}
