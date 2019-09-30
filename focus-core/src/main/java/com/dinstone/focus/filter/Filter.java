package com.dinstone.focus.filter;

import com.dinstone.focus.invoke.InvokeHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

public interface Filter {

    Reply invoke(InvokeHandler next, Call call) throws Exception;

}
