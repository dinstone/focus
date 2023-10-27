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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.dinstone.focus.example.AuthenService;
import com.dinstone.focus.serialze.protostuff.ProtostuffSerializer;
import com.dinstone.focus.transport.photon.PhotonAcceptOptions;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

public class SslFocusServerTest {

    public static void main(String[] args) throws CertificateException {

        String appName = "focus.ssl.server";
        ServerOptions serverOptions = new ServerOptions(appName);
        serverOptions.listen("localhost", 3333);

        // setting ssl
        PhotonAcceptOptions acceptOptions = new PhotonAcceptOptions();
        acceptOptions.setEnableSsl(true);
        acceptOptions.setIdleTimeout(100000000);
        SelfSignedCertificate cert = new SelfSignedCertificate();
        acceptOptions.setPrivateKey(cert.key());
        acceptOptions.setCertChain(new X509Certificate[] { cert.cert() });
        // setting accept options
        serverOptions.setAcceptOptions(acceptOptions);
        // setting global default serilizer
        serverOptions.setSerializerType(ProtostuffSerializer.SERIALIZER_TYPE);

        FocusServer server = new FocusServer(serverOptions);

        // export alias service
        server.exporting(AuthenService.class, new AuthenService(), "AuthenService");

        server.start();
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.close();
    }

    private static OpenTelemetry getTelemetry(String serviceName) {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                // .addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder().build()).build())
                .setResource(resource).build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        return openTelemetry;
    }

}
