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

import java.net.ConnectException;
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
import com.dinstone.focus.invoke.Context;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.transport.Connector;
import com.dinstone.focus.utils.ConstantUtil;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class RemoteInvokeHandler implements Handler {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteInvokeHandler.class);

    private final ServiceLocator serviceLocator;
    private final Connector connector;
    private final int connectRetry;

    public RemoteInvokeHandler(ServiceConfig serviceConfig, ServiceLocator serviceLocator, Connector connector) {
        this.connectRetry = ((ConsumerServiceConfig) serviceConfig).getConnectRetry();
        this.serviceLocator = serviceLocator;
        this.connector = connector;
    }

    @Override
    public CompletableFuture<Object> handle(Invocation invocation) {
        int timeoutRetry = invocation.getMethodConfig().getTimeoutRetry();
        return timeoutRetry(new CompletableFuture<>(), invocation, timeoutRetry);
    }

    private CompletableFuture<Object> timeoutRetry(CompletableFuture<Object> future, Invocation invocation,
            int remain) {
        connectRetry(invocation).whenComplete((r, e) -> {
            if (e == null) {
                future.complete(r);
                return;
            }

            // handle error
            if (e instanceof CompletionException) {
                e = e.getCause();
            }
            // check timeout exception to retry
            if (e instanceof TimeoutException || e instanceof CancellationException) {
                if (remain > 0) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            LOG.info("timeout retry remain {}", remain);
                            timeoutRetry(future, invocation, remain - 1);
                        } catch (Exception ex) {
                            future.completeExceptionally(ex);
                        }
                    });
                } else {
                    future.completeExceptionally(e);
                }
            } else {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<Object> connectRetry(Invocation invocation) {
        Context context = invocation.context();

        List<ServiceInstance> exclusions = new LinkedList<>();
        ServiceInstance exclusion = context.get(ConstantUtil.RPC_SERVER);
        if (exclusion != null) {
            exclusions.add(exclusion);
        }

        int total = connectRetry + 1;
        for (int i = 1; i <= total; i++) {
            // find a service instance
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
                context.put(ConstantUtil.RPC_RETRY, exclusions);
                context.put(ConstantUtil.RPC_SERVER, selected);

                return connector.send(invocation, instance).whenComplete((reply, error) -> {
                    long finishTime = System.currentTimeMillis();
                    serviceLocator.feedback(instance, invocation, reply, error, finishTime - startTime);
                });
            } catch (Exception e) {
                long finishTime = System.currentTimeMillis();
                serviceLocator.feedback(instance, invocation, null, e, finishTime - startTime);

                if (e instanceof ConnectException) {
                    exclusions.add(selected);
                } else {
                    throw new InvokeException(ErrorCode.INVOKE_ERROR,
                            invocation.getService() + " connect to " + instance.getInstanceAddress(), e);
                }
            }

        }

        throw new ServiceException(ErrorCode.ACCESS_ERROR,
                total + " times connect error for " + invocation.getProvider() + "/" + invocation.getService());
    }

}
