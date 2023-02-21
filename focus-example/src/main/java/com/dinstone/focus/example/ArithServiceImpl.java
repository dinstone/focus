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

import com.dinstone.focus.protobuf.ArithRequest;
import com.dinstone.focus.protobuf.ArithResponse;

public class ArithServiceImpl implements ArithService {

    @Override
    public ArithResponse Add(ArithRequest request) {
        return ArithResponse.newBuilder().setC(request.getA() + request.getB()).build();
    }

    @Override
    public ArithResponse Sub(ArithRequest request) {
        return ArithResponse.newBuilder().setC(request.getA() - request.getB()).build();
    }

    @Override
    public ArithResponse Mul(ArithRequest request) {
        return ArithResponse.newBuilder().setC(request.getA() * request.getB()).build();
    }

    @Override
    public ArithResponse Div(ArithRequest request) {
        return ArithResponse.newBuilder().setC(request.getA() / request.getB()).build();
    }

}
