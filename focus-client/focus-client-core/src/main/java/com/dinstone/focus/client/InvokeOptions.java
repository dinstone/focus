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
package com.dinstone.focus.client;

/**
 * method level options
 * 
 * @author dinstone
 *
 */
public class InvokeOptions {

    private String methodName;

    private int invokeTimeout;

    private int invokeRetry;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getInvokeTimeout() {
        return invokeTimeout;
    }

    public void setInvokeTimeout(int invokeTimeout) {
        this.invokeTimeout = invokeTimeout;
    }

    public int getInvokeRetry() {
        return invokeRetry;
    }

    public void setInvokeRetry(int invokeRetry) {
        this.invokeRetry = invokeRetry;
    }

    @Override
    public String toString() {
        return "InvokeOptions [methodName=" + methodName + ", invokeTimeout=" + invokeTimeout + ", invokeRetry="
                + invokeRetry + "]";
    }

}
