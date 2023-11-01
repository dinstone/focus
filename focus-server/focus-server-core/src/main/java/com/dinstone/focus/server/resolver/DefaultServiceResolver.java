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
package com.dinstone.focus.server.resolver;

import com.dinstone.focus.naming.DefaultInstance;
import com.dinstone.focus.naming.ServiceInstance;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.server.ServiceResolver;

public class DefaultServiceResolver implements ServiceResolver {

    @Override
    public void publish(ServerOptions serverOptions) throws Exception {
    }

    @Override
    public void destroy() {
    }

    protected ServiceInstance createInstance(ServerOptions serverOptions) {
        String app = serverOptions.getApplication();
        String host = serverOptions.getListenAddress().getHostString();
        int port = serverOptions.getListenAddress().getPort();

        DefaultInstance instance = new DefaultInstance();
        String code = host + ":" + port;
        instance.setInstanceCode(code);
        instance.setInstanceHost(host);
        instance.setInstancePort(port);
        instance.setServiceName(app);

        instance.setProtocolType(serverOptions.getAcceptOptions().getProtocol());
        instance.setMetadata(serverOptions.getMetadata());
        return instance;
    }

}
