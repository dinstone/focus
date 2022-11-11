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
package com.dinstone.focus.client.starter;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.ClientOptions;

@SpringBootApplication
@EnableFocusClient
@Configurable
class EnableFocusClientTest {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext c = SpringApplication.run(EnableFocusClientTest.class, args);
        ImService im = c.getBean(ImService.class);

        String m = im.sayHi("dinstone");
        System.out.println("message is " + m);

        c.close();
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    FocusClient defaultFocusClient() {
        return new FocusClient(new ClientOptions().connect("127.0.0.1", 2222));
    }
}