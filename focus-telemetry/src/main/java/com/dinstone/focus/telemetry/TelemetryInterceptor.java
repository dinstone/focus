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
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;

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

    private TextMapGetter<Call> getter;

    private TextMapSetter<Call> setter;

    public TelemetryInterceptor(OpenTelemetry telemetry, Kind kind) {
        this.telemetry = telemetry;
        this.kind = kind;

        this.getter = new TextMapGetter<Call>() {

            public String get(Call carrier, String key) {
                if (carrier.attach().containsKey(key)) {
                    return carrier.attach().get(key);
                }
                return null;
            }

            public Iterable<String> keys(Call carrier) {
                return carrier.attach().keySet();
            }
        };
        this.setter = new TextMapSetter<Call>() {

            public void set(Call carrier, String key, String value) {
                carrier.attach().put(key, value);
            }
        };
    }

    @Override
    public CompletableFuture<Reply> intercept(Call call, Handler chain) throws Exception {
        Tracer tracer = telemetry.getTracer(call.getService());
        if (kind == Kind.SERVER) {
            // Extract the SpanContext and other elements from the request.
            Context ec = telemetry.getPropagators().getTextMapPropagator().extract(Context.current(), call, getter);
            try (Scope ss = ec.makeCurrent()) {
                return chain.handle(call);
            }
        } else {
            Span span = tracer.spanBuilder(call.getMethod()).setSpanKind(SpanKind.CLIENT).startSpan();
            try (Scope ss = span.makeCurrent()) {
                span.setAttribute(RPC_SERVICE, call.getService()).setAttribute(RPC_METHOD, call.getMethod());
                // Inject the request with the *current* Context, which contains our current
                // Span.
                telemetry.getPropagators().getTextMapPropagator().inject(Context.current(), call, setter);
                return chain.handle(call).whenComplete((reply, error) -> {
                    finishSpan(reply, error, span);
                });
            } catch (Throwable error) {
                finishSpan(null, error, span);
                throw error;
            }
        }

    }

    private void finishSpan(Reply reply, Throwable error, Span span) {
        if (error != null) {
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.recordException(error);
        }
        span.end();
    }

}
