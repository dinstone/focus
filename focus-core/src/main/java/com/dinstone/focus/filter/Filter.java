package com.dinstone.focus.filter;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.rpc.Call;
import com.dinstone.focus.rpc.Reply;

public interface Filter {

    Reply invoke(InvokeHandler next, Call call) throws Exception;

}
