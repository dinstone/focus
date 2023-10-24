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
package com.dinstone.focus.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.focus.example.AuthenCheck;
import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.OrderRequest;
import com.dinstone.focus.example.OrderService;
import com.dinstone.focus.example.Page;
import com.dinstone.focus.example.User;
import com.dinstone.focus.example.UserService;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.telemetry.TelemetryInterceptor;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

public class FocusClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClientTest.class);

    public static void main(String[] args) throws Exception {

        LOG.info("init start");

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("endpoint.name"), "focus.example.client")));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                // .addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder().build()).build())
                .setResource(resource).build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.CLIENT);

        ClientOptions option = new ClientOptions("focus.example.client").connect("localhost", 3333).addInterceptor(tf);
        // option.setSerializerType(ProtostuffSerializer.SERIALIZER_TYPE);

        FocusClient client = new FocusClient(option);

        LOG.info("init end");

        try {
            UserService us = client.importing(UserService.class);
            Page<User> ps = us.listUser(1);
            System.out.println(ps);

            User u = us.getUser(10);
            System.out.println(u);

            AuthenCheck ac = client.importing(AuthenCheck.class,
                    new ImportOptions("AuthenService").setTimeoutMillis(20000));
            asyncError(ac);

            asyncExecute(ac, "AuthenCheck async hot: ");
            asyncExecute(ac, "AuthenCheck async exe: ");

            // DemoService ds = client.importing(DemoService.class);
            // syncError(ds);
            // conparal(ds);
            // execute(ds, "DemoService sync hot: ");
            // execute(ds, "DemoService sync exe: ");
            //
            // OrderService oc = client.importing(OrderService.class, new ImportOptions(OrderService.class.getName())
            // .setSerializerType(ProtostuffSerializer.SERIALIZER_TYPE));
            // executeOrderService(oc, "OrderService sync hot: ");
            // executeOrderService(oc, "OrderService sync exe: ");
            //
            // oc = client.importing(OrderService.class,
            // new ImportOptions("OrderService").setSerializerType(JacksonSerializer.SERIALIZER_TYPE));
            // executeOrderService(oc, "OrderService sync hot: ");
            // executeOrderService(oc, "OrderService sync exe: ");

        } finally {
            client.close();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private static void executeOrderService(OrderService oc, String tag) {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 100000;
        while (c < loopCount) {
            OrderRequest or = new OrderRequest();
            or.setUid("u-" + c);
            or.setPoi("p-" + c);
            or.setSn("s-" + c);
            oc.findOldOrder(or);
            c++;
        }
        long et = System.currentTimeMillis() - st;
        System.out.println(tag + et + " ms, " + (loopCount * 1000 / et) + " tps");
    }

    private static void asyncExecute(AuthenCheck ac, String tag) throws InterruptedException {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 100000;
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger ng = new AtomicInteger();
        CountDownLatch cdl = new CountDownLatch(loopCount);
        while (c < loopCount) {
            ac.token("dinstoneo").whenComplete((r, e) -> {
                if (e == null) {
                    ok.incrementAndGet();
                } else {
                    ng.incrementAndGet();
                }
                cdl.countDown();
            });
            c++;
        }
        cdl.await();
        long et = System.currentTimeMillis() - st;
        System.out.println(
                tag + et + " ms, ok=" + ok.get() + ",ng=" + ng.get() + ", " + (loopCount * 1000 / et) + " tps");
    }

    private static void syncError(final DemoService ds) {
        try {
            System.out.println("====================");
            ds.hello("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void asyncError(AuthenCheck ac) {
        try {
            Future<Boolean> check2 = ac.check("dinstone");
            CompletableFuture<String> future = ac.token("dinstone");
            Future<Boolean> check1 = ac.check(null);
            System.out.println("token 1 is " + future.get());
            System.out.println("check 2 is " + check2.get());
            System.out.println("check 1 is " + check1.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void conparal(final DemoService ds) {
        for (int i = 1; i < 3; i++) {
            final int index = i;
            Thread t = new Thread() {
                @Override
                public void run() {
                    execute(ds, "client-" + index + ": ");
                }
            };
            t.setName("rpc-client-" + i);
            t.start();
        }
    }

    private static void execute(DemoService ds, String tag) {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 100000;
        while (c < loopCount) {
            ds.hello("dinstoneo");
            c++;
        }
        long et = System.currentTimeMillis() - st;
        System.out.println(tag + et + " ms, " + (loopCount * 1000 / et) + " tps");
    }

}
