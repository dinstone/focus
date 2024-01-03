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
package com.dinstone.focus.transport.photon;

import java.net.InetSocketAddress;
import java.util.function.Function;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.transport.Acceptor;
import com.dinstone.focus.transport.ExecutorSelector;

public class PhotonAcceptor implements Acceptor {

    private com.dinstone.photon.Acceptor acceptor;
    private ExecutorSelector executorSelector;

    public PhotonAcceptor(PhotonAcceptOptions acceptOptions) {
        acceptor = new com.dinstone.photon.Acceptor(acceptOptions);
        executorSelector = acceptOptions.getExecutorSelector();
    }

    @Override
    public void bind(InetSocketAddress serviceAddress, Function<String, ServiceConfig> serviceLookupper)
            throws Exception {
        acceptor.setMessageProcessor(new PhotonMessageProcessor(serviceLookupper, executorSelector));
        acceptor.bind(serviceAddress);
    }

    @Override
    public void destroy() {
        acceptor.destroy();
        if (executorSelector != null) {
            executorSelector.destroy();
        }
    }

}
