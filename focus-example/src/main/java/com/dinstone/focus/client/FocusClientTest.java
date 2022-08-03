/*
 * Copyright (C) 2019~2022 dinstone<dinstone@163.com>
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
import java.util.concurrent.Future;

import com.dinstone.focus.example.AuthenCheck;
import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.tracing.TracingFilter;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.dinstone.photon.ConnectOptions;

import brave.Span.Kind;
import brave.Tracing;
import brave.rpc.RpcTracing;
import brave.sampler.Sampler;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class FocusClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(FocusClientTest.class);

    public static void main(String[] args) {

        LOG.info("init start");

        Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        Tracing tracing = Tracing.newBuilder().localServiceName("focus.client").addSpanHandler(spanHandler)
                .sampler(Sampler.create(1)).build();
        Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.CLIENT);

        ClientOptions option = new ClientOptions().setEndpoint("focus.example.client").connect("localhost", 3333)
                .setConnectOptions(new ConnectOptions()).addFilter(tf);
        FocusClient client = new FocusClient(option);
        final DemoService ds = client.reference(DemoService.class);
        LOG.info("init end");
        try {
            ds.hello("");
        } catch (Exception e) {
            e.printStackTrace();
        }

        AuthenCheck a = client.reference(AuthenCheck.class, "AuthenService", "", 2000);
        try {
            Future<Boolean> check1 = a.check(null);
            Future<Boolean> check2 = a.check("dinstone");
            CompletableFuture<String> future = a.token("dinstone");
            System.out.println("token 1 is " + future.get());
            System.out.println("check 2 is " + check2.get());
            System.out.println("check 1 is " + check1.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        conparal(ds);

        execute(ds, "hot: ");
        execute(ds, "exe: ");

        client.destroy();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private static void conparal(final DemoService ds) {
        for (int i = 1; i < 3; i++) {
            final int index = i;
            Thread t = new Thread() {
                @Override
                public void run() {
                    execute(ds, "client-" + index + " :");
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
