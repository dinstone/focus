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

import com.dinstone.focus.example.UserService;
import com.dinstone.focus.filter.Filter;
import com.dinstone.focus.protobuf.UserCheckRequest;
import com.dinstone.focus.protobuf.UserCheckResponse;
import com.dinstone.focus.serialze.protobuf.ProtobufSerializer;
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

public class UserServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceClient.class);

    public static void main(String[] args) {

        Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        Tracing tracing = Tracing.newBuilder().localServiceName("focus.client").addSpanHandler(spanHandler)
                .sampler(Sampler.create(1)).build();
        Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.CLIENT);

        ConnectOptions connectOptions = new ConnectOptions();
        ClientOptions option = new ClientOptions().setSerializerId(ProtobufSerializer.SERIALIZER_KEY)
                .connect("localhost", 3301).setConnectOptions(connectOptions).addFilter(tf);
        FocusClient client = new FocusClient(option);

        final UserService ds = client.importing(UserService.class);

        try {
            UserCheckRequest ucr = UserCheckRequest.newBuilder().setUserId("dinstone").build();
            UserCheckResponse res = ds.checkExist(ucr);
            LOG.info("user check is {}", res.getExist());
        } catch (Exception e) {
            e.printStackTrace();
        }

        client.destroy();
    }

}