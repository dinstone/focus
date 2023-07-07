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

import com.dinstone.focus.annotation.ServiceDefination;

@Component
@ServiceDefination(service = "AuthenService")
public class AuthenService {

	public boolean check(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name is empty");
		}
		return false;
	}

	public boolean authen(String name) {
		if ("dinstone".equals(name)) {
			return true;
		}
		return false;
	}

	public String token(String name) {
		return "token-" + name;
	}

}
