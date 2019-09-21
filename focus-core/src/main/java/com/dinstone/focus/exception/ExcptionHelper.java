package com.dinstone.focus.exception;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExcptionHelper {

    public static Throwable getTargetException(InvocationTargetException e) {
        Throwable t = e.getTargetException();
        if (t instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException ut = (UndeclaredThrowableException) t;
            t = ut.getCause();
            if (t instanceof InvocationTargetException) {
                return getTargetException((InvocationTargetException) t);
            }
        }
        return t;
    }

}
