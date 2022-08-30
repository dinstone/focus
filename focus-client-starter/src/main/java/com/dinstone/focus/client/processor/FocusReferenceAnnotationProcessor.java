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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.annotation.FocusReference;

public class FocusReferenceAnnotationProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            FocusReference autowired = AnnotationUtils.getAnnotation(field, FocusReference.class);
            if (autowired != null) {
                String cid = autowired.client();
                cid = cid.length() > 0 ? cid : "defaultClient";
                FocusClient client = applicationContext.getBean(cid, FocusClient.class);

                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, bean,
                        client.importing(field.getType(), autowired.service(), autowired.group(), autowired.timeout()));
            }
        });
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
