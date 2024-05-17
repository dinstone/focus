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
package com.dinstone.focus.client.invoke;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.client.ServiceLocator;
import com.dinstone.focus.client.config.ConsumerServiceConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.ChainHandler;
import com.dinstone.focus.invoke.Context;
import com.dinstone.focus.invoke.DefaultInvocation;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.propagate.Baggage;
import com.dinstone.focus.propagate.Propagator;

/**
 * client-side service invoker.
 *
 * @author dinstone
 * 
 * @version 1.0.0
 */
public class ConsumerChainHandler extends ChainHandler {

    private final ServiceLocator serviceLocator;

    private final Propagator propagator;

    private final int connectRetry;

    public ConsumerChainHandler(ServiceConfig serviceConfig, Handler invokeHandler, ServiceLocator serviceLocator) {
        super(serviceConfig, invokeHandler);
        this.serviceLocator = serviceLocator;
        this.propagator = new Propagator();
        this.connectRetry = ((ConsumerServiceConfig) serviceConfig).getConnectRetry();
    }

    public CompletableFuture<Object> handle(Invocation invocation) {
        try (Context context = Context.create()) {
            ((DefaultInvocation) invocation).context(context);

            // inject propagate baggage to invocation
            Baggage baggage = context.get(Baggage.ContextKey);
            if (baggage != null) {
                propagator.inject(invocation, baggage);
            }

            int timeoutRetry = invocation.getMethodConfig().getTimeoutRetry();
            return timeoutRetry(new CompletableFuture<>(), invocation, timeoutRetry - 1);
        }
    }

    private CompletableFuture<Object> timeoutRetry(CompletableFuture<Object> future, Invocation invocation,
            int remain) {

        connectRetry(invocation).thenApply(future::complete).exceptionally(e -> {
            if (e instanceof CompletionException) {
                e = e.getCause();
            }
            // check timeout exception to retry
            if (e instanceof TimeoutException || e instanceof CancellationException) {
                if (remain < 1) {
                    future.completeExceptionally(e);
                } else {
                    try {
                        timeoutRetry(future, invocation, remain - 1);
                    } catch (Exception e1) {
                        future.completeExceptionally(e);
                    }
                }
            } else {
                future.completeExceptionally(e);
            }

            return true;
        });

        return future;
    }

    private CompletableFuture<Object> connectRetry(Invocation invocation) {
        List<ServiceInstance> exclusions = new LinkedList<>();
        // find a service instance
        for (int i = 1; i <= connectRetry; i++) {
            ServiceInstance selected = serviceLocator.locate(invocation, exclusions);

            // check
            if (selected == null) {
                throw new ServiceException(ErrorCode.ACCESS_ERROR,
                        "locate " + i + "times, can't find a live service instance for " + invocation.getProvider()
                                + "/" + invocation.getService());
            }

            // invoke
            final ServiceInstance instance = selected;
            long startTime = System.currentTimeMillis();
            try {
                invocation.context().put(Context.SERVICE_INSTANCE_KEY, selected);
                return invokeHandler.handle(invocation).whenComplete((reply, error) -> {
                    long finishTime = System.currentTimeMillis();
                    serviceLocator.feedback(instance, invocation, reply, error, finishTime - startTime);
                });
            } catch (InvokeException e) {
                long finishTime = System.currentTimeMillis();
                serviceLocator.feedback(instance, invocation, null, e, finishTime - startTime);
                // connection exception can retry
                if (e.getCode() == ErrorCode.CONNECT_ERROR) {
                    exclusions.add(selected);
                } else {
                    throw e;
                }
            }
        }

        throw new ServiceException(ErrorCode.ACCESS_ERROR,
                connectRetry + " times connect error for " + invocation.getProvider() + "/" + invocation.getService());
    }

}
