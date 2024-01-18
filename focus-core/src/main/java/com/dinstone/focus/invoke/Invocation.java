/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
package com.dinstone.focus.invoke;

import java.net.InetSocketAddress;
import java.util.Map;

public interface Invocation {
    String CONSUMER_KEY = "call.consumer";
    String PROVIDER_KEY = "call.provider";
    String SERVICE_KEY = "call.service";
    String METHOD_KEY = "call.method";
    String TIMEOUT_KEY = "call.timeout";

    String getService();

    String getMethod();

    Object getParameter();

    String getEndpoint();

    String getConsumer();

    String getProvider();

    int getTimeout();

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

    Map<String, String> attributes();
}
