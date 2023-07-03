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
package com.dinstone.focus.server.starter;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.dinstone.focus.server.FocusServer;
import com.dinstone.focus.server.ServerOptions;

@Configurable
@EnableFocusServer
@SpringBootApplication
class EnableFocusServerTest {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(EnableFocusServerTest.class, args);
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    FocusServer defaultFocusServer() {
        return new FocusServer(new ServerOptions("demo.server").listen(2222)).start();
    }

}
