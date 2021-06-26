/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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

import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.FilterChain;
import com.dinstone.focus.filter.FilterInitializer;
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

        FilterInitializer filterInitializer = new FilterInitializer() {

            @Override
            public void init(FilterChain chain) {
                chain.addFilter(tf);
            }
        };

        ConnectOptions connectOptions = new ConnectOptions();
        ClientOptions option = new ClientOptions().connect("localhost", 3333).setConnectOptions(connectOptions)
                .setFilterInitializer(filterInitializer);
        Client client = new Client(option);
        final DemoService ds = client.importing(DemoService.class);

        LOG.info("int end");

        // try {
        // ds.hello(null);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

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

        execute(ds, "hot: ");
        execute(ds, "exe: ");

        client.destroy();
    }

    private static void execute(DemoService ds, String tag) {
        int c = 0;
        long st = System.currentTimeMillis();
        int loopCount = 200000;
        while (c < loopCount) {
            ds.hello("dinstoneo", c);
            c++;
        }
        long et = System.currentTimeMillis() - st;
        System.out.println(tag + et + " ms, " + (loopCount * 1000 / et) + " tps");
    }

}
