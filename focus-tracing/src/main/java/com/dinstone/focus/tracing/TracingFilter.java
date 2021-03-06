/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.focus.tracing;

import static brave.internal.Throwables.propagateIfFatal;

import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.FilterContext;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

import brave.Span;
import brave.Span.Kind;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.TraceContext;
import brave.rpc.RpcClientHandler;
import brave.rpc.RpcClientRequest;
import brave.rpc.RpcRequest;
import brave.rpc.RpcServerHandler;
import brave.rpc.RpcServerRequest;
import brave.rpc.RpcTracing;

public class TracingFilter implements Filter {

    CurrentTraceContext currentTraceContext;
    RpcClientHandler clientHandler;
    RpcServerHandler serverHandler;
    Kind kind;

    public TracingFilter(RpcTracing rpcTracing, Kind kind) {
        if (rpcTracing == null) {
            throw new NullPointerException("rpcTracing == null");
        }

        currentTraceContext = rpcTracing.tracing().currentTraceContext();
        clientHandler = RpcClientHandler.create(rpcTracing);
        serverHandler = RpcServerHandler.create(rpcTracing);
        this.kind = kind;
    }

    @Override
    public Reply invoke(FilterContext next, Call call) throws Exception {
        Span span;
        RpcRequest request;
        if (kind.equals(Kind.CLIENT)) {
            RpcClientRequest clientRequest = new FocusClientRequest(call);
            TraceContext traceContext = currentTraceContext.get();
            span = clientHandler.handleSendWithParent(clientRequest, traceContext);
            request = clientRequest;
        } else {
            RpcServerRequest serverRequest = new FocusServerRequest(call);
            request = serverRequest;
            span = serverHandler.handleReceive(serverRequest);
        }

        Scope scope = currentTraceContext.newScope(span.context());
        Reply reply = null;
        Throwable error = null;
        try {
            reply = next.invoke(call);
            return reply;
        } catch (Throwable e) {
            propagateIfFatal(e);
            error = e;
            throw e;
        } finally {
            FinishSpan.finish(this, request, reply, error, span);
            scope.close();
        }
    }

}
