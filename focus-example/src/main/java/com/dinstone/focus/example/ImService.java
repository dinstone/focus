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

import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dinstone.focus.annotation.ServiceReference;

@Service
public class ImService {

    @Autowired
    AuthenCheck check;

    @ServiceReference(service = "AuthenService")
    AuthenCheck check2;

    public String sayHi(String string) throws Exception {
        Future<Boolean> cf = check.check(string);
        boolean ar = check.authen("dinstone");
        boolean ar2 = check.authen("focus");
        return "check result " + cf.get() + ", authen result " + (ar && ar2);
    }

}
