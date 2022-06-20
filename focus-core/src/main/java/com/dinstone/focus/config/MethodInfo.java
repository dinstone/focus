package com.dinstone.focus.config;

import java.lang.reflect.Method;

public class MethodInfo {

    private Method method;

    private Class<?> paramType;

    private Class<?> returnType;

    public MethodInfo(Method method, Class<?> paramType) {
        this.method = method;
        this.paramType = paramType;
        this.returnType = method.getReturnType();
    }

    public String getMethodName() {
        return method.getName();
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getParamType() {
        return paramType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?> getDeclarClass() {
        return method.getDeclaringClass();
    }

    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

}
