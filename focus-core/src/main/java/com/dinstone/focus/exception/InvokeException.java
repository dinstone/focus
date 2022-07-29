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
package com.dinstone.focus.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * invoke exception
 * 
 * @author dinstone
 *
 */
public class InvokeException extends FocusException {
    /**  */
    private static final long serialVersionUID = 1L;

    private int code;

    private String detail;

    public InvokeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public InvokeException(int code, Throwable cause) {
        super(null, cause, false, false);
        this.code = code;
        if (cause != null) {
            this.detail = ExceptionUtil.getStackTrace(cause);
        }
    }

    public InvokeException(int code, String message, String detail) {
        super(message, null, false, false);
        this.code = code;
        this.detail = detail;
    }

    /**
     * the code to get
     * 
     * @return the code
     * 
     * @see FocusException#code
     */
    public int getCode() {
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

    public String getDetail() {
        return detail;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (detail != null) {
            s.println(this);
            s.print("Caused by: " + detail);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (detail != null) {
            s.println(this);
            s.print(detail);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + ": (" + code + ") " + getMessage();
    }

}
