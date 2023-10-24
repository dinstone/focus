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

import com.dinstone.focus.example.UserCheckService;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.protobuf.UserCheckRequest;
import com.dinstone.focus.protobuf.UserCheckResponse;
import com.dinstone.focus.serialze.protobuf.ProtobufSerializer;
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

public class UserServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceClient.class);

    public static void main(String[] args) {

        // Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        // AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        // Tracing tracing =
        // Tracing.newBuilder().localServiceName("focus.client").addSpanHandler(spanHandler)
        // .sampler(Sampler.create(1)).build();
        // Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.CLIENT);

        String serviceName = "user.client";
        OpenTelemetry openTelemetry = getTelemetry(serviceName);
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.CLIENT);

        ClientOptions option = new ClientOptions(serviceName).connect("localhost", 3301).addInterceptor(tf);
        FocusClient client = new FocusClient(option);

        ImportOptions ro = new ImportOptions(UserCheckService.class.getName())
                .setSerializerType(ProtobufSerializer.SERIALIZER_TYPE);
        final UserCheckService ds = client.importing(UserCheckService.class, ro);

        try {
            UserCheckRequest ucr = UserCheckRequest.newBuilder().setUserId("dinstone").build();
            UserCheckResponse res = ds.checkExist(ucr);
            LOG.info("user check is {}", res.getExist());
        } catch (Exception e) {
            e.printStackTrace();
        }

        client.close();
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
