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

import com.dinstone.photon.utils.ExceptionUtil;

/**
 * framework exception
 * 
 * @author dinstone
 *
 */
public class ExchangeException extends FocusException {
    /**  */
    private static final long serialVersionUID = 1L;

    private int code;

    private String traces;

    public ExchangeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ExchangeException(int code, String message, Throwable cause) {
        super(message, cause, false, false);
        this.code = code;
        if (cause != null) {
            setStackTrace(cause.getStackTrace());
        }
    }

    public ExchangeException(int code, String message, String traces) {
        super(message, null, false, false);
        this.traces = traces;
        this.code = code;
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

    public String getTraces() {
        return traces;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (traces != null) {
            s.println(this);
            s.print(traces);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (traces != null) {
            s.println(this);
            s.print(traces);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + ": (" + code + ")" + getMessage();
    }

}
