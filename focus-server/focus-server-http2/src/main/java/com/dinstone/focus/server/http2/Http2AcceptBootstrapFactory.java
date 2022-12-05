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
package com.dinstone.focus.server.http2;

import com.dinstone.focus.server.transport.AcceptBootstrap;
import com.dinstone.focus.server.transport.AcceptBootstrapFactory;
import com.dinstone.focus.server.transport.AcceptOptions;

public class Http2AcceptBootstrapFactory implements AcceptBootstrapFactory {

    @Override
    public boolean appliable(AcceptOptions acceptOptions) {
        return acceptOptions instanceof Http2AcceptOptions;
    }

    @Override
    public AcceptBootstrap create(AcceptOptions acceptOptions) {
        return new Http2AcceptBootstrap((Http2AcceptOptions) acceptOptions);
    }

}
