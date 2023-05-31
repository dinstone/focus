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
import java.util.function.Function;

import com.dinstone.focus.invoke.Handler;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.ResultToErrorCode;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;

public class CircuitBreakInterceptor implements Interceptor {

    private CircuitBreakAPI circuitBreak;

    public CircuitBreakInterceptor(String... addresses) {
        circuitBreak = CircuitBreakAPIFactory.createCircuitBreakAPIByAddress(addresses);
    }

    @Override
    public CompletableFuture<Reply> intercept(Call call, Handler handler) throws Exception {
        ServiceKey skey = new ServiceKey("default", call.getService());
        FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(skey, call.getMethod());
        makeDecoratorRequest.setResultToErrorCode(new ResultToErrorCode() {
            @Override
            public int onSuccess(Object value) {
                return 200;
            }

            @Override
            public int onError(Throwable throwable) {
                return 500;
            }
        });
        FunctionalDecorator decorator = circuitBreak.makeFunctionalDecorator(makeDecoratorRequest);
        Function<Call, CompletableFuture<Reply>> f = decorator
                .decorateFunction(new Function<Call, CompletableFuture<Reply>>() {

                    @Override
                    public CompletableFuture<Reply> apply(Call call) {
                        try {
                            return handler.handle(call);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        return f.apply(call);
    }

}
