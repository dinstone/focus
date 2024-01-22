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
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.client.ServiceLocator;
import com.dinstone.focus.client.config.ConsumerServiceConfig;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Invocation;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.transport.Connector;

public class RemoteInvokeHandler implements Handler {

    private final ConsumerServiceConfig serviceConfig;

    private final ServiceLocator locater;

    private final Connector connector;

    private final int connectRetry;

    public RemoteInvokeHandler(ServiceConfig serviceConfig, ServiceLocator locater, Connector connector) {
        this.serviceConfig = (ConsumerServiceConfig) serviceConfig;
        this.connector = connector;
        this.locater = locater;

        this.connectRetry = this.serviceConfig.getConnectRetry();
    }

    @Override
    public CompletableFuture<Object> handle(Invocation invocation) {
        MethodConfig methodConfig = invocation.getMethodConfig();
        return timeoutRetry(new CompletableFuture<>(), methodConfig.getTimeoutRetry(), invocation);
    }

    private CompletableFuture<Object> timeoutRetry(CompletableFuture<Object> future, int remain,
                                                   Invocation invocation) {

        connectRetry(invocation).thenApply(future::complete).exceptionally(e -> {
            if (e instanceof CompletionException) {
                e = e.getCause();
            }
            // check timeout exception to retry
            if (e instanceof TimeoutException) {
                if (remain <= 1) {
                    future.completeExceptionally(e);
                } else {
                    try {
                        timeoutRetry(future, remain - 1, invocation);
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
        for (int i = 0; i < connectRetry; i++) {
            ServiceInstance selected = locater.locate(invocation, exclusions);

            // check
            if (selected == null) {
                break;
            }

            // invoke
            try {
                final ServiceInstance instance = selected;
                long startTime = System.currentTimeMillis();
                InetSocketAddress socketAddress = instance.getInstanceAddress();
                return connector.send(invocation, socketAddress).whenComplete((reply, error) -> {
                    long finishTime = System.currentTimeMillis();
                    locater.feedback(instance, invocation, reply, error, finishTime - startTime);
                });
            } catch (ConnectException e) {
                // ignore and retry
                exclusions.add(selected);
            } catch (Exception e) {
                throw new ServiceException(ErrorCode.ACCESS_ERROR,
                        connectRetry + " retry for " + invocation.getService(), e);
            }
        }

        throw new ServiceException(ErrorCode.ACCESS_ERROR,
                connectRetry + " retry can't find a live service instance for " + invocation.getService());
    }

}
