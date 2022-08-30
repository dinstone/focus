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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionUtil {

    /**
     * get target exception
     *
     * @param e
     *
     * @return
     */
    public static Throwable getTargetException(InvocationTargetException e) {
        Throwable t = e.getTargetException();
        if (t instanceof UndeclaredThrowableException) {
            t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            if (t instanceof InvocationTargetException) {
                return getTargetException((InvocationTargetException) t);
            }
        }
        return t;
    }

    /**
     * get non empty message
     *
     * @param cause
     *
     * @return
     */
    public static String getMessage(Throwable cause) {
        if (cause == null) {
            return null;
        }
        String msg = cause.getMessage();
        if (msg != null) {
            return msg;
        }
        return getMessage(cause.getCause());
    }

    /**
     * get exception stack trace string
     *
     * @param e
     *
     * @return
     */
    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.getBuffer().toString();
    }

    public static String getStackTrace(StackTraceElement[] stes) {
        StringWriter sw = new StringWriter();
        if (stes != null && stes.length > 0) {
            PrintWriter s = new PrintWriter(sw);
            for (StackTraceElement traceElement : stes)
                s.println("\tat " + traceElement);
        }
        return sw.getBuffer().toString();
    }

}
