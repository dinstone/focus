/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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

import com.dinstone.focus.exception.InvokeException;
import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext.RequestContext;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;

public class CircuitBreakInterceptor implements Interceptor {

    private CircuitBreakAPI circuitBreak;

    public CircuitBreakInterceptor(String... addresses) {
        circuitBreak = CircuitBreakAPIFactory.createCircuitBreakAPIByAddress(addresses);
    }

    @Override
    public CompletableFuture<Reply> intercept(Call call, Handler handler) throws Exception {
        ServiceKey skey = new ServiceKey("default", call.getProvider());
        RequestContext makeDecoratorRequest = new FunctionalDecoratorRequest(skey, call.getMethod());
        InvokeHandler invokeHandler = circuitBreak.makeInvokeHandler(makeDecoratorRequest);

        invokeHandler.acquirePermission();

        long startTimeMillis = System.currentTimeMillis();
        try {
            CompletableFuture<Reply> future = handler.handle(call);
            return future.whenComplete((reply, error) -> {

                long delayTimeMillis = System.currentTimeMillis() - startTimeMillis;

                InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                responseContext.setDuration(delayTimeMillis);
                responseContext.setDurationUnit(TimeUnit.MILLISECONDS);

                if (error == null && reply.isError()) {
                    InvokeException e = (InvokeException) reply.getData();
                }

                if (error == null) {
                    responseContext.setResult(reply);
                    invokeHandler.onSuccess(responseContext);
                } else {
                    responseContext.setError(error);
                    invokeHandler.onError(responseContext);
                }
            });
        } catch (Throwable e) {
            long delayTimeMillis = System.currentTimeMillis() - startTimeMillis;
            InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
            responseContext.setDuration(delayTimeMillis);
            responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
            responseContext.setError(e);
            invokeHandler.onError(responseContext);

            throw e;
        }

    }

}
