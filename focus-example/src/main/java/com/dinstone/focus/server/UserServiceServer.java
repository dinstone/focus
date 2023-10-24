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

import com.dinstone.focus.example.UserCheckService;
import com.dinstone.focus.example.UserCheckServiceImpl;
import com.dinstone.focus.invoke.Interceptor;
import com.dinstone.focus.serialze.protobuf.ProtobufSerializer;
import com.dinstone.focus.telemetry.TelemetryInterceptor;
import com.dinstone.focus.transport.photon.PhotonAcceptOptions;
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

public class UserServiceServer {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceServer.class);

    public static void main(String[] args) {

        FocusServer uss = createUserServiceServer();

        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        uss.close();
        LOG.info("server stop");
    }

    private static FocusServer createUserServiceServer() {

        String serviceName = "user.service";
        OpenTelemetry openTelemetry = getTelemetry(serviceName);
        Interceptor tf = new TelemetryInterceptor(openTelemetry, Interceptor.Kind.SERVER);

        ServerOptions serverOptions = new ServerOptions(serviceName);
        serverOptions.listen("localhost", 3301);
        serverOptions.addInterceptor(tf).setAcceptOptions(new PhotonAcceptOptions());
        FocusServer server = new FocusServer(serverOptions);
        server.exporting(UserCheckService.class, new UserCheckServiceImpl(),
                new ExportOptions(UserCheckService.class.getName())
                        .setSerializerType(ProtobufSerializer.SERIALIZER_TYPE));

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

}
