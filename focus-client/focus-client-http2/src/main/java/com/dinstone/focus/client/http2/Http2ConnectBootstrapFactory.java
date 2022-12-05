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
package com.dinstone.focus.client.http2;

import com.dinstone.focus.client.transport.ConnectBootstrap;
import com.dinstone.focus.client.transport.ConnectBootstrapFactory;
import com.dinstone.focus.client.transport.ConnectOptions;

public class Http2ConnectBootstrapFactory implements ConnectBootstrapFactory {

    @Override
    public boolean appliable(ConnectOptions connectOptions) {
        return connectOptions instanceof Http2ConnectOptions;
    }

    @Override
    public ConnectBootstrap create(ConnectOptions connectOptions) {
        return new Http2ConnectBootstrap((Http2ConnectOptions) connectOptions);
    }

}
