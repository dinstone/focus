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
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.telemetry.TelemetryInterceptor;
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

        String appName = "order.client";
        OpenTelemetry openTelemetry = getTelemetry(appName);
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.CLIENT);

        ClientOptions clientOptions = new ClientOptions(appName).connect("localhost", 3303).addInterceptor(tf);
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
            client.close();
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
