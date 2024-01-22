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
package com.dinstone.focus.client.consul;

import com.dinstone.focus.client.LocatorFactory;
import com.dinstone.focus.client.LocatorOptions;
import com.dinstone.focus.client.ServiceLocator;

public class ConsulLocatorFactory implements LocatorFactory {

    public boolean applicable(LocatorOptions locatorOptions) {
        return locatorOptions instanceof ConsulLocatorOptions;
    }

    @Override
    public ServiceLocator create(LocatorOptions locatorOptions) {
        return new ConsulServiceLocator((ConsulLocatorOptions) locatorOptions);
    }

}
