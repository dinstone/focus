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
package com.dinstone.focus.exception;

/**
 * If an exception is thrown by the business party, such as the user does not exist, the original exception will be
 * directly thrown.
 * 
 * @author dinstone
 *
 */
public class BusinessException extends InvokeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(ErrorCode code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public BusinessException(ErrorCode code, String message) {
        super(code, message);
    }

    public BusinessException(ErrorCode code, Throwable cause) {
        super(code, cause);
    }

}
