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
package com.dinstone.focus.client.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.FocusClient;

@Configuration
@ConditionalOnBean(FocusClientConfiguration.Marker.class)
public class FocusClientAutoConfiguration {

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    FocusClient defaultFocusClient(ClientOptions clientOptions) {
        return new FocusClient(clientOptions);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceReferenceProcessor serviceReferenceProcessor() {
        return new ServiceReferenceProcessor();
    }
}
