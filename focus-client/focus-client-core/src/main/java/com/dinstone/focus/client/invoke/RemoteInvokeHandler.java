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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.dinstone.focus.client.ServiceLocater;
import com.dinstone.focus.client.config.ConsumerServiceConfig;
import com.dinstone.focus.config.MethodConfig;
import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.exception.ErrorCode;
import com.dinstone.focus.exception.ServiceException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.transport.Connector;

public class RemoteInvokeHandler implements Handler {

    private final ConsumerServiceConfig serviceConfig;

    private final ServiceLocater locater;

    private final Connector connector;

    private final int connectRetry;

    public RemoteInvokeHandler(ServiceConfig serviceConfig, ServiceLocater locater, Connector connector) {
        this.serviceConfig = (ConsumerServiceConfig) serviceConfig;
        this.connector = connector;
        this.locater = locater;

        this.connectRetry = this.serviceConfig.getConnectRetry();
    }

    @Override
    public CompletableFuture<Reply> handle(Call call) throws Exception {
        MethodConfig methodConfig = serviceConfig.lookup(call.getMethod());
        return timeoutRetry(new CompletableFuture<>(), methodConfig.getTimeoutRetry(), call);
    }

    private CompletableFuture<Reply> timeoutRetry(CompletableFuture<Reply> future, int remain, Call call)
            throws Exception {

        connectRetry(call).thenApply(future::complete).exceptionally(e -> {
            if (e instanceof CompletionException) {
                e = e.getCause();
            }
            // check timeout exception to retry
            if (e instanceof TimeoutException) {
                if (remain <= 1) {
                    future.completeExceptionally(e);
                } else {
                    try {
                        timeoutRetry(future, remain - 1, call);
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

    private CompletableFuture<Reply> connectRetry(Call call) throws Exception {
        ServiceInstance selected = null;
        // find an address
        for (int i = 0; i < connectRetry; i++) {
            selected = locater.locate(call, selected);

            // check
            if (selected == null) {
                break;
            }

            // invoke
            try {
                final ServiceInstance instance = selected;
                long startTime = System.currentTimeMillis();
                return connector.send(call, serviceConfig, instance.getInstanceAddress())
                        .whenComplete((reply, error) -> {
                            long finishTime = System.currentTimeMillis();
                            locater.feedback(instance, call, reply, error, finishTime - startTime);
                        });
            } catch (ConnectException e) {
                // ignore and retry
            } catch (Exception e) {
                throw new ServiceException(ErrorCode.ACCESS_ERROR, connectRetry + " retry for " + call.getService(), e);
            }
        }

        throw new ServiceException(ErrorCode.ACCESS_ERROR,
                connectRetry + " retry can't find a live service instance for " + call.getService());
    }

}
