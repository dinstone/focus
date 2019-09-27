/*
 * Copyright (C) 2013~2017 dinstone<dinstone@163.com>
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

package com.dinstone.focus.server.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.binding.ImplementBinding;
import com.dinstone.focus.exception.ExcptionHelper;
import com.dinstone.focus.invoker.InvocationHandler;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Reply;
import com.dinstone.focus.proxy.ServiceProxy;

public class LocalInvocationHandler implements InvocationHandler {

    private ImplementBinding implementBinding;

    public LocalInvocationHandler(ImplementBinding implementBinding) {
        this.implementBinding = implementBinding;
    }

    @Override
    public Reply handle(Call call) throws Throwable {
        ServiceProxy<?> wrapper = implementBinding.lookup(call.getService(), call.getGroup());
        if (wrapper == null) {
            throw new RpcException(404, "unkown service: " + call.getService() + "[" + call.getGroup() + "]");
        }

        Reply reply = null;
        try {
            Class<?>[] paramTypes = getParamTypes(call.getParams(), call.getParamTypes());
            Method method = wrapper.getService().getDeclaredMethod(call.getMethod(), paramTypes);
            Object resObj = method.invoke(wrapper.getTarget(), call.getParams());
            reply = new Reply(200, resObj);
        } catch (RpcException e) {
            reply = new Reply(e.getCode(), e.getMessage());
        } catch (NoSuchMethodException e) {
            reply = new Reply(405, "unkown method: [" + call.getGroup() + "]" + e.getMessage());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            String message = "illegal access: [" + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + e.getMessage();
            reply = new Reply(502, message);
        } catch (InvocationTargetException e) {
            Throwable t = ExcptionHelper.getTargetException(e);
            String message = "service exception: " + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + t.getMessage();
            reply = new Reply(500, message);
        } catch (Throwable e) {
            String message = "service exception: " + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + e.getMessage();
            reply = new Reply(509, message);
        }
        return reply;
    }

    private Class<?>[] getParamTypes(Object[] params, Class<?>[] paramTypes) {
        if (paramTypes == null && params != null) {
            paramTypes = parseParamTypes(params);
        }
        return paramTypes;
    }

    protected Class<?>[] getParamTypes(Call call) {
        Class<?>[] paramTypes = call.getParamTypes();
        Object[] params = call.getParams();
        if (paramTypes == null && params != null) {
            paramTypes = parseParamTypes(params);
        }
        return paramTypes;
    }

    private Class<?>[] parseParamTypes(Object[] args) {
        Class<?>[] cs = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            cs[i] = arg.getClass();
        }

        return cs;
    }

}
