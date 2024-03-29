/*
 * Copyright (C) 2019~2024 dinstone<dinstone@163.com>
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
 * invoke exception
 *
 * @author dinstone
 */
public class InvokeException extends FocusException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String CODE_KEY = "error.code";

    private final ErrorCode code;

    public InvokeException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public InvokeException(ErrorCode code, Throwable cause) {
        this(code, cause != null ? cause.getMessage() : "", cause);
    }

    public InvokeException(ErrorCode code, String message, Throwable cause) {
        super(message, cause, false, false);
        this.code = code;
    }

    /**
     * the code to get
     *
     * @return the code
     * 
     * @see InvokeException#code
     */
    public ErrorCode getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (msg != null) {
            return msg;
        }
        return ExceptionUtil.getMessage(getCause());
    }

    @Override
    public String toString() {
        return getClass().getName() + ": (" + code + ") " + getMessage();
    }

}
