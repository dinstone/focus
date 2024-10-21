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
package com.dinstone.focus.transport.photon;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.dinstone.focus.config.ServiceConfig;
import com.dinstone.focus.transport.Acceptor;
import com.dinstone.focus.transport.ExecutorSelector;

public class PhotonAcceptor implements Acceptor {

    private final com.dinstone.photon.Acceptor delegateAcceptor;
    private final ExecutorSelector executorSelector;
    private final ExecutorService sharedExecutor;

    public PhotonAcceptor(PhotonAcceptOptions acceptOptions) {
        delegateAcceptor = new com.dinstone.photon.Acceptor(acceptOptions);
        executorSelector = acceptOptions.getExecutorSelector();

        int businessSize = acceptOptions.getBusinessSize();
        if (businessSize < 1) {
            businessSize = PhotonAcceptOptions.DEFAULT_BUSINESS_SIZE;
        }
        sharedExecutor = Executors.newFixedThreadPool(businessSize);
    }

    @Override
    public void bind(InetSocketAddress serviceAddress, Function<String, ServiceConfig> serviceFinder) throws Exception {
        delegateAcceptor.setProcessor(new PhotonProcessor(serviceFinder, sharedExecutor, executorSelector));
        delegateAcceptor.bind(serviceAddress);
    }

    @Override
    public void destroy() {
        delegateAcceptor.destroy().awaitUninterruptibly();
        if (executorSelector != null) {
            executorSelector.destroy();
        }
        if (sharedExecutor != null) {
            sharedExecutor.shutdown();
        }
    }

}
