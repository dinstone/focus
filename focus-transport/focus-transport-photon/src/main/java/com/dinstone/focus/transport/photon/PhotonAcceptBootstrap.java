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
package com.dinstone.focus.transport.photon;

import java.net.InetSocketAddress;

import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.transport.AcceptBootstrap;
import com.dinstone.focus.transport.ExecutorSelector;
import com.dinstone.photon.Acceptor;

public class PhotonAcceptBootstrap implements AcceptBootstrap {

    private Acceptor acceptor;
    private ExecutorSelector executorSelector;

    public PhotonAcceptBootstrap(PhotonAcceptOptions acceptOptions) {
        acceptor = new Acceptor(acceptOptions);
        executorSelector = acceptOptions.getExecutorSelector();
    }

    @Override
    public void bind(InetSocketAddress serviceAddress, ImplementBinding implementBinding) throws Exception {
        acceptor.setMessageProcessor(new FocusMessageProcessor(implementBinding, executorSelector));
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
