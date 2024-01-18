/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.telemetry;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.invoke.Invocation;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

public class TelemetryInterceptor implements Interceptor {

    private static final String RPC_SERVICE = "rpc.service";

    private static final String RPC_METHOD = "rpc.method";

    private Kind kind;

    private OpenTelemetry telemetry;

    private TextMapGetter<Invocation> getter;

    private TextMapSetter<Invocation> setter;

    public TelemetryInterceptor(OpenTelemetry telemetry, Kind kind) {
        this.telemetry = telemetry;
        this.kind = kind;

        this.getter = new TextMapGetter<Invocation>() {

            public String get(Invocation carrier, String key) {
                if (carrier.attributes().containsKey(key)) {
                    return carrier.attributes().get(key);
                }
                return null;
            }

            public Iterable<String> keys(Invocation carrier) {
                return carrier.attributes().keySet();
            }
        };
        this.setter = new TextMapSetter<Invocation>() {

            public void set(Invocation carrier, String key, String value) {
                carrier.attributes().put(key, value);
            }
        };
    }

    @Override
    public CompletableFuture<Object> intercept(Invocation invocation, Handler chain) throws Exception {
        Tracer tracer = telemetry.getTracer(invocation.getService());
        if (kind == Kind.SERVER) {
            Span span = getServerSpan(invocation, tracer);
            try (Scope ignored = span.makeCurrent()) {
                return chain.handle(invocation).whenComplete((reply, error) -> {
                    finishSpan(reply, error, span);
                });
            } catch (Throwable error) {
                finishSpan(null, error, span);
                throw error;
            }
        } else {
            Span span = getClientSpan(invocation, tracer);
            try (Scope ignored = span.makeCurrent()) {
                // Inject the request with the *current* Context, which contains our current
                // Span.
                telemetry.getPropagators().getTextMapPropagator().inject(Context.current(), invocation, setter);
                return chain.handle(invocation).whenComplete((reply, error) -> {
                    finishSpan(reply, error, span);
                });
            } catch (Throwable error) {
                finishSpan(null, error, span);
                throw error;
            }
        }

    }

    private Span getClientSpan(Invocation call, Tracer tracer) {
        Span span = tracer.spanBuilder(call.getEndpoint()).setSpanKind(SpanKind.CLIENT).startSpan();
        return span.setAttribute(RPC_SERVICE, call.getService()).setAttribute(RPC_METHOD, call.getMethod());
    }

    private Span getServerSpan(Invocation call, Tracer tracer) {
        // Extract the SpanContext and other elements from the request.
        Context pc = telemetry.getPropagators().getTextMapPropagator().extract(Context.current(), call, getter);
        Span span = tracer.spanBuilder(call.getEndpoint()).setSpanKind(SpanKind.SERVER).setParent(pc).startSpan();
        return span.setAttribute(RPC_SERVICE, call.getService()).setAttribute(RPC_METHOD, call.getMethod());
    }

    private void finishSpan(Object reply, Throwable error, Span span) {
        if (error != null) {
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.recordException(error);
        }
        span.end();
    }

}
