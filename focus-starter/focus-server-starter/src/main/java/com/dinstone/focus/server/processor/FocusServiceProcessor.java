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
package com.dinstone.focus.server.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.dinstone.focus.server.ExportOptions;
import com.dinstone.focus.server.FocusServer;
import com.dinstone.focus.server.annotation.FocusService;

public class FocusServiceProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        FocusService fsa = bean.getClass().getAnnotation(FocusService.class);
        if (fsa != null) {
            String endpoint = fsa.server().length() > 0 ? fsa.server() : "defaultFocusServer";
            FocusServer server = applicationContext.getBean(endpoint, FocusServer.class);
            Class<Object> clazz = (Class<Object>) bean.getClass();
            Class<Object>[] ifs = (Class<Object>[]) clazz.getInterfaces();
            if (ifs != null && ifs.length > 0) {
                clazz = ifs[0];
            }
            String service = fsa.service().length() > 0 ? fsa.service() : clazz.getName();
            server.exporting(clazz, bean, new ExportOptions(service, fsa.group()));
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
