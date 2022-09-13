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
package com.dinstone.focus.clutch.zookeeper;

import com.dinstone.focus.clutch.ServiceInstance;
import com.google.gson.Gson;

public class ServiceInstanceSerializer {

    private final Gson gson = new Gson();

    public byte[] serialize(ServiceInstance service) throws Exception {
        return gson.toJson(service).getBytes("utf-8");
    }

    public ServiceInstance deserialize(byte[] bytes) throws Exception {
        return gson.fromJson(new String(bytes, "utf-8"), ServiceInstance.class);
    }

}
