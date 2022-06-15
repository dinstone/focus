/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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

import com.dinstone.focus.protocol.Reply;

import brave.Span;
import brave.internal.Nullable;
import brave.rpc.RpcClientHandler;
import brave.rpc.RpcClientRequest;
import brave.rpc.RpcRequest;
import brave.rpc.RpcServerHandler;

//Intentionally the same as Apache Dubbo, even though we don't use the first arg.
//When the signature is the same, it reduces work porting bug fixes or tests
abstract class FinishSpan {

    static void finish(TracingFilter filter, RpcRequest request, @Nullable Reply result, @Nullable Throwable error,
            Span span) {
        if (request instanceof RpcClientRequest) {
            filter.clientHandler.handleReceive(new FocusClientResponse((FocusClientRequest) request, result, error),
                    span);
        } else {
            filter.serverHandler.handleSend(new FocusServerResponse((FocusServerRequest) request, result, error), span);
        }
    }

    static FinishSpan create(TracingFilter filter, RpcRequest request, Reply result, Span span) {
        if (request instanceof RpcClientRequest) {
            return new FinishClientSpan(span, result, filter.clientHandler, (FocusClientRequest) request);
        }
        return new FinishServerSpan(span, result, filter.serverHandler, (FocusServerRequest) request);
    }

    final Span span;
    final Reply result;

    FinishSpan(Span span, Reply reply) {
        if (span == null)
            throw new NullPointerException("span == null");
        if (reply == null)
            throw new NullPointerException("result == null");
        this.span = span;
        this.result = reply;
    }

    /** One, but not both parameters can be {@code null}. */
    public abstract void accept(@Nullable Object unused, @Nullable Throwable error);

    static final class FinishClientSpan extends FinishSpan {
        final RpcClientHandler clientHandler;
        final FocusClientRequest request;

        FinishClientSpan(Span span, Reply result, RpcClientHandler clientHandler, FocusClientRequest request) {
            super(span, result);
            this.clientHandler = clientHandler;
            this.request = request;
        }

        @Override
        public void accept(@Nullable Object unused, @Nullable Throwable error) {
            clientHandler.handleReceive(new FocusClientResponse(request, result, error), span);
        }
    }

    static final class FinishServerSpan extends FinishSpan {
        final RpcServerHandler serverHandler;
        final FocusServerRequest request;

        FinishServerSpan(Span span, Reply result, RpcServerHandler serverHandler, FocusServerRequest request) {
            super(span, result);
            this.serverHandler = serverHandler;
            this.request = request;
        }

        @Override
        public void accept(@Nullable Object unused, @Nullable Throwable error) {
            serverHandler.handleSend(new FocusServerResponse(request, result, error), span);
        }
    }
}
