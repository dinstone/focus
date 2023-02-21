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

import java.util.Date;

import com.dinstone.focus.example.OrderRequest;
import com.dinstone.focus.example.OrderResponse;
import com.dinstone.focus.example.OrderService;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.filter.Filter.Kind;
import com.dinstone.focus.telemetry.TelemetryFilter;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

public class OrderServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceClient.class);

    public static void main(String[] args) {

        // Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        // AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        // Tracing tracing = Tracing.newBuilder().localServiceName("focus.client").addSpanHandler(spanHandler)
        // .sampler(Sampler.create(1)).build();
        // Filter tf = new TracingFilter(RpcTracing.create(tracing), Filter.Kind.CLIENT);
        String serviceName = "order.client";
        OpenTelemetry openTelemetry = getTelemetry(serviceName);
        Filter tf = new TelemetryFilter(openTelemetry, Kind.CLIENT);

        ClientOptions clientOptions = new ClientOptions().setEndpoint(serviceName).connect("localhost", 3303)
                .addFilter(tf);
        FocusClient client = new FocusClient(clientOptions);

        OrderService oc = client.importing(OrderService.class);

        OrderRequest order = new OrderRequest();
        order.setCt(new Date());
        order.setPoi("1234");
        order.setSn("MDHEWED");
        order.setUid("dinstone");

        try {
            OrderResponse o = oc.createOrder(order);
            LOG.info("order id = {}", o.getOid());
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.destroy();
        }

    }

    private static OpenTelemetry getTelemetry(String serviceName) {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder().build()).build())
                .setResource(resource).build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        return openTelemetry;
    }

}
