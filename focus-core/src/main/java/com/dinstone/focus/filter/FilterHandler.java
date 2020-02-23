package com.dinstone.focus.filter;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public class FilterHandler implements InvokeHandler {

    private final Filter nextFilter;

    private final InvokeHandler invokeHandler;

    public FilterHandler(Filter nextFilter, InvokeHandler invokeHandler) {
        super();
        this.nextFilter = nextFilter;
        this.invokeHandler = invokeHandler;

        if (nextFilter == null && invokeHandler == null) {
            throw new RpcException(400, "nextFilter and invokeHandler is null");
        }
    }

    @Override
    public Reply invoke(Call call) throws Exception {
        if (nextFilter != null) {
            return nextFilter.invoke(invokeHandler, call);
        } else {
            return invokeHandler.invoke(call);
        }
    }
}
