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
package com.dinstone.focus.invoke;

import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;

public class HandlerTest {

    @Test
    public void handleTest01() throws Exception {
        Handler h = invocation -> CompletableFuture.completedFuture(invocation.getEndpoint());
        CompletableFuture<Object> cf = h.handle(new DefaultInvocation("service name", "method name", "parameter"));
        Assert.assertEquals(cf.get(), "service name" + "/" + "method name");
    }

    @Test
    public void handleTest02() throws Exception {
        Handler h = invocation -> {
            try {
                return CompletableFuture.completedFuture(invocation.getEndpoint());
            } catch (Exception e) {
                CompletableFuture<Object> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        };

        CompletableFuture<Object> cf = h.handle(null);
        cf.handle((r, e) -> {
            Assert.assertEquals(e.getClass(), NullPointerException.class);
            return e;
        }).get();
    }
}
