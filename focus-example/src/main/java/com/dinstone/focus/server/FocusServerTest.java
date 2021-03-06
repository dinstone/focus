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
package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.focus.example.DemoService;
import com.dinstone.focus.example.DemoServiceImpl;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.FilterChain;
import com.dinstone.focus.filter.FilterInitializer;
import com.dinstone.focus.tracing.TracingFilter;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

import brave.Span.Kind;
import brave.Tracing;
import brave.rpc.RpcTracing;
import brave.sampler.Sampler;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class FocusServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(FocusServerTest.class);

    public static void main(String[] args) {
        Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        Tracing tracing = Tracing.newBuilder().localServiceName("focus.server").sampler(Sampler.create(1))
                .addSpanHandler(spanHandler).build();

        final Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.SERVER);

        FilterInitializer filterInitializer = new FilterInitializer() {

            @Override
            public void init(FilterChain chain) {
                chain.addFilter(tf);
            }
        };

        ServerOptions serverOptions = new ServerOptions();
        serverOptions.listen("localhost", 3333);
        serverOptions.setFilterInitializer(filterInitializer);
        Server server = new Server(serverOptions);
        server.exporting(DemoService.class, new DemoServiceImpl());
        // server.start();
        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.destroy();
        LOG.info("server stop");
    }

}
