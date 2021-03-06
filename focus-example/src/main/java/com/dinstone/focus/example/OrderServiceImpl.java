/*
 * Copyright (C) 2019~2021 dinstone<dinstone@163.com>
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

import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private UserService userService;

    private StoreService storeService;

    public OrderServiceImpl(UserService userService, StoreService storeService) {
        super();
        this.userService = userService;
        this.storeService = storeService;
    }

    @Override
    public OrderResponse createOrder(OrderRequest order) {
        if (!userService.isExist(order.getUid())) {
            throw new IllegalArgumentException("user id is valid");
        }
        log.info("user is exist:{}", order.getUid());

        storeService.checkExist(order.getUid(), order.getSn(), order.getPoi());
        return new OrderResponse().setOid(order.getUid() + "-" + order.getPoi() + "-" + order.getSn());
    }

}
