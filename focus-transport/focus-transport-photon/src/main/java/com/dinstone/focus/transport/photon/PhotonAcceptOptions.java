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
package com.dinstone.focus.transport.photon;

import com.dinstone.focus.transport.AcceptOptions;
import com.dinstone.focus.transport.ExecutorSelector;

public class PhotonAcceptOptions extends com.dinstone.photon.AcceptOptions implements AcceptOptions {

    private ExecutorSelector executorSelector;

    @Override
    public ExecutorSelector getExecutorSelector() {
        return executorSelector;
    }

    public void setExecutorSelector(ExecutorSelector executorSelector) {
        this.executorSelector = executorSelector;
    }

    @Override
    public String getProtocol() {
        return "photon";
    }

}
