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
package com.dinstone.focus.example;

import org.springframework.stereotype.Component;

import com.dinstone.focus.annotation.ServiceDefinition;

@Component
@ServiceDefinition(service = "AuthenService")
public class AuthenService {

    public String login(Account account) {
        if ("dinstone".equals(account.getUsername()) && "123456".endsWith(account.getPassword())) {
            return token(account.getUsername());
        }
        throw new IllegalArgumentException("username or password is error");
    }

    public boolean check(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username is empty");
        }
        return false;
    }

    public boolean authen(String token) {
        if ("token-dinstone".equals(token)) {
            return true;
        }
        return false;
    }

    public String token(String name) {
        return "token-" + name;
    }

}
