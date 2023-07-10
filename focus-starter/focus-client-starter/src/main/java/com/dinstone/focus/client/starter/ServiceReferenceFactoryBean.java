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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;

import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.ImportOptions;

public class ServiceReferenceFactoryBean implements FactoryBean<Object> {
    private BeanFactory beanFactory;
    private ImportOptions options;
    private Class<?> type;

    @Override
    public Object getObject() {
        return beanFactory.getBean(FocusClient.class).importing(type, options);
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

    public void setOptions(ImportOptions options) {
        this.options = options;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

}
