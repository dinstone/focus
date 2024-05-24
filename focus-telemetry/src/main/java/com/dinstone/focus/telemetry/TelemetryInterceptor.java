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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.utils.ConstantUtil;
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

    private final Kind kind;

    private final OpenTelemetry telemetry;

    private final TextMapGetter<Invocation> getter;

    private final TextMapSetter<Invocation> setter;

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
    public CompletableFuture<Object> intercept(Invocation invocation, Handler chain) {
        Tracer tracer = telemetry.getTracer(invocation.getService());
        if (kind == Kind.SERVER) {
            Span span = getServerSpan(invocation, tracer);
            try (Scope ignored = span.makeCurrent()) {
                return chain.handle(invocation).whenComplete((reply, error) -> {
                    finishSpan(invocation.context(), error, span);
                });
            } catch (RuntimeException e) {
                finishSpan(invocation.context(), e, span);
                throw e;
            }
        } else {
            Span span = getClientSpan(invocation, tracer);
            try (Scope ignored = span.makeCurrent()) {
                // Inject the request with the *current* Context, which contains our current Span.
                telemetry.getPropagators().getTextMapPropagator().inject(Context.current(), invocation, setter);
                return chain.handle(invocation).whenComplete((reply, error) -> {
                    finishSpan(invocation.context(), error, span);
                });
            } catch (RuntimeException e) {
                finishSpan(invocation.context(), e, span);
                throw e;
            }
        }
    }

    private static CompletableFuture<Object> completableFuture(Exception e) {
        CompletableFuture<Object> f = new CompletableFuture<Object>();
        f.completeExceptionally(e);
        return f;
    }

    private Span getClientSpan(Invocation invocation, Tracer tracer) {
        return tracer.spanBuilder(invocation.getEndpoint()).setSpanKind(SpanKind.CLIENT).startSpan();
    }

    private Span getServerSpan(Invocation invocation, Tracer tracer) {
        // Extract the SpanContext and other elements from the request.
        Context pc = telemetry.getPropagators().getTextMapPropagator().extract(Context.current(), invocation, getter);
        return tracer.spanBuilder(invocation.getEndpoint()).setSpanKind(SpanKind.SERVER).setParent(pc).startSpan();
    }

    private void finishSpan(com.dinstone.focus.invoke.Context context, Throwable error, Span span) {
        if (error != null) {
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.recordException(error);
        }
        if (context != null) {
            String link = context.get(ConstantUtil.RPC_LINK);
            span.setAttribute(ConstantUtil.RPC_LINK, link);

            List<ServiceInstance> retryList = context.get(ConstantUtil.RPC_RETRY);
            if (retryList != null) {
                StringBuilder builder = new StringBuilder();
                retryList.forEach(instance -> {
                    builder.append(instance.getInstanceAddress()).append(",");
                });
                if (builder.length() > 0) {
                    builder.setLength(builder.length() - 1);
                }
                span.setAttribute(ConstantUtil.RPC_RETRY, builder.toString());
            }
        }
        span.end();
    }

}
