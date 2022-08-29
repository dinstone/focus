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
package com.dinstone.focus.clutch.consul;

import com.dinstone.focus.clutch.ClutchOptions;

public class ConsulClutchOptions implements ClutchOptions {

    private String agentHost = "localhost";
    private int agentPort = 8500;
    private int interval = 3;
    private int checkTtl = 6;

    public String getAgentHost() {
        return agentHost;
    }

    public ConsulClutchOptions setAgentHost(String agentHost) {
        this.agentHost = agentHost;
        return this;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public ConsulClutchOptions setAgentPort(int agentPort) {
        this.agentPort = agentPort;
        return this;
    }

    public int getInterval() {
        return interval;
    }

    public ConsulClutchOptions setInterval(int interval) {
        this.interval = interval;
        return this;
    }

    public int getCheckTtl() {
        return checkTtl;
    }

    public ConsulClutchOptions setCheckTtl(int checkTtl) {
        this.checkTtl = checkTtl;
        return this;
    }

    @Override
    public String clutchType() {
        return "consul";
    }

}
