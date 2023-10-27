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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

@Service
public class AuthenCheckLocal implements AuthenCheck {

    @Override
    public Future<Boolean> check(String name) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<String> token(String name) {
        return CompletableFuture.completedFuture("local : " + name);
    }

    @Override
    public boolean authen(String name) {
        if (name.equals("dinstone")) {
            return true;
        }
        return false;
    }

    @Override
    public String login(Account account) {
        return "local-token-" + account.getUsername();
    }

}
