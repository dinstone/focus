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
package com.dinstone.focus.client.polaris;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.invoke.Invocation;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext.RequestContext;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;

public class CircuitBreakInterceptor implements Interceptor, AutoCloseable {

    private static final String DEFAULT_NAMESPACE = "default";

    private final CircuitBreakAPI circuitBreak;

    private final SDKContext polarisContext;

    public CircuitBreakInterceptor(String... polarisAddress) {
        if (polarisAddress == null || polarisAddress.length == 0) {
            polarisContext = SDKContext.initContext();
        } else {
            Configuration config = ConfigAPIFactory.createConfigurationByAddress(polarisAddress);
            polarisContext = SDKContext.initContextByConfig(config);
        }

        circuitBreak = CircuitBreakAPIFactory.createCircuitBreakAPIByContext(polarisContext);
    }

    @Override
    public CompletableFuture<Object> intercept(Invocation invocation, Handler handler) {
        ServiceKey skey = new ServiceKey(DEFAULT_NAMESPACE, invocation.getProvider());
        RequestContext requestContext = new FunctionalDecoratorRequest(skey, invocation.getMethod());
        InvokeHandler invokeHandler = circuitBreak.makeInvokeHandler(requestContext);

        try {
            invokeHandler.acquirePermission();
        } catch (CallAbortedException e) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(new ServiceException(ErrorCode.CIRCUIT_BREAK_ERROR,
                    invocation.getEndpoint() + " circuit break", e));
            return future;
        }

        long startTimeMillis = System.currentTimeMillis();
        return handler.handle(invocation).whenComplete((reply, error) -> {

            long delayTimeMillis = System.currentTimeMillis() - startTimeMillis;

            InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
            responseContext.setDuration(delayTimeMillis);
            responseContext.setDurationUnit(TimeUnit.MILLISECONDS);

            if (error != null) {
                responseContext.setError(error);
                invokeHandler.onError(responseContext);
            } else {
                responseContext.setResult(reply);
                invokeHandler.onSuccess(responseContext);
            }
        });
    }

    @Override
    public void close() {
        polarisContext.close();
    }

}
