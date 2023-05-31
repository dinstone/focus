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
package com.dinstone.focus.clutch.polaris;

import com.dinstone.focus.clutch.ClutchFactory;
import com.dinstone.focus.clutch.ClutchOptions;
import com.dinstone.focus.clutch.ServiceDiscovery;
import com.dinstone.focus.clutch.ServiceRegistry;

public class PolarisClutchFactory implements ClutchFactory {

    @Override
    public boolean appliable(ClutchOptions clutchOptions) {
        return clutchOptions instanceof PolarisClutchOptions;
    }

    @Override
    public ServiceRegistry createServiceRegistry(ClutchOptions clutchOptions) {
        return new PolarisServiceRegistry((PolarisClutchOptions) clutchOptions);
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(ClutchOptions clutchOptions) {
        return new PolarisServiceDiscovery((PolarisClutchOptions) clutchOptions);
    }

}
