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
package com.dinstone.focus.client.processor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;

import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.ImportOptions;

public class FocusReferenceFactoryBean implements FactoryBean<Object> {
    private BeanFactory beanFactory;
    private Class<?> type;
    private String client;
    private String service;
    private String group;
    private int timeout;

    @Override
    public Object getObject() {
        if (client != null || client.isEmpty()) {
            client = "defaultFocusClient";
        }
        FocusClient focusClient = beanFactory.getBean(client, FocusClient.class);
        return focusClient.importing(type, new ImportOptions(service, group).setTimeout(timeout));
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
