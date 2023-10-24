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
package com.dinstone.focus.server;

import java.io.IOException;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.ImportOptions;
import com.dinstone.focus.example.OrderService;
import com.dinstone.focus.example.OrderServiceImpl;
import com.dinstone.focus.example.StoreService;
import com.dinstone.focus.example.UserCheckService;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.serialze.protobuf.ProtobufSerializer;
import com.dinstone.focus.telemetry.TelemetryInterceptor;
import com.dinstone.focus.transport.photon.PhotonAcceptOptions;
import com.dinstone.focus.transport.photon.PhotonConnectOptions;
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

public class OrderServiceServer {

    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceServer.class);

    public static void main(String[] args) {

        FocusServer sss = createOrderServiceServer();

        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sss.close();
        LOG.info("server stop");
    }

    private static FocusServer createOrderServiceServer() {
        // Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        // AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        // Tracing tracing =
        // Tracing.newBuilder().localServiceName("order.service").addSpanHandler(spanHandler)
        // .sampler(Sampler.create(1)).build();
        //
        // final Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.SERVER);

        String appName = "order.service";
        OpenTelemetry openTelemetry = getTelemetry(appName);
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.SERVER);

        ServerOptions serverOptions = new ServerOptions(appName).setAcceptOptions(new PhotonAcceptOptions());
        serverOptions.listen("localhost", 3303);
        serverOptions.addInterceptor(tf);
        FocusServer server = new FocusServer(serverOptions);

        UserCheckService userService = createUserServiceRpc(openTelemetry);
        StoreService storeService = createStoreServiceRpc(openTelemetry);
        OrderService orderService = new OrderServiceImpl(userService, storeService);

        server.exporting(OrderService.class, orderService);

        return server.start();
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

    private static StoreService createStoreServiceRpc(OpenTelemetry openTelemetry) {
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.CLIENT);

        ClientOptions option = new ClientOptions("store.service.client").connect("localhost", 3302)
                .setConnectOptions(new PhotonConnectOptions()).addInterceptor(tf);
        FocusClient client = new FocusClient(option);
        return client.importing(StoreService.class);
    }

    private static UserCheckService createUserServiceRpc(OpenTelemetry openTelemetry) {
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.CLIENT);

        ClientOptions option = new ClientOptions("user.service.client").connect("localhost", 3301)
                .setConnectOptions(new PhotonConnectOptions()).addInterceptor(tf);
        FocusClient client = new FocusClient(option);

        ImportOptions ro = new ImportOptions(UserCheckService.class.getName())
                .setSerializerType(ProtobufSerializer.SERIALIZER_TYPE);
        return client.importing(UserCheckService.class, ro);
    }

}
