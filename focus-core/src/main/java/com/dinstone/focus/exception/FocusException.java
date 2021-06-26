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
package com.dinstone.focus.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * business exception.
 * 
 * @author guojf
 * 
 * @version 1.0.0.2013-10-28
 */
public class FocusException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 1L;

    private String traces;

    /**
     * @param message
     */
    public FocusException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public FocusException(String message, Throwable cause) {
        super(message, cause);
        if (cause != null) {
            this.setStackTrace(cause.getStackTrace());
        }
    }

    /**
     * @param message
     * @param cause
     */
    public FocusException(String message, String traces) {
        super(message, null, false, false);
        this.traces = traces;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (traces != null) {
            s.print(traces);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (traces != null) {
            s.print(traces);
        } else {
            super.printStackTrace(s);
        }
    }
}
