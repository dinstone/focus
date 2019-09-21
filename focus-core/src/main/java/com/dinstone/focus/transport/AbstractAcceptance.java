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

package com.dinstone.focus.transport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetSocketAddress;

import com.dinstone.focus.RpcException;
import com.dinstone.focus.invoker.ServiceInvoker;
import com.dinstone.focus.protocol.Call;
import com.dinstone.focus.protocol.Request;
import com.dinstone.focus.protocol.Response;
import com.dinstone.focus.protocol.Reply;

public abstract class AbstractAcceptance implements Acceptance {

    protected ServiceInvoker serviceInvoker;

    protected TransportConfig transportConfig;

    protected InetSocketAddress serviceAddress;

    public AbstractAcceptance(ServiceInvoker serviceInvoker, TransportConfig transportConfig,
            InetSocketAddress serviceAddress) {
        if (serviceInvoker == null) {
            throw new IllegalArgumentException("serviceInvoker is null");
        }
        this.serviceInvoker = serviceInvoker;
        this.serviceAddress = serviceAddress;
        this.transportConfig = transportConfig;
    }

    @Override
    public Response handle(Request request) {
        Reply result = null;
        Call call = request.getCall();
        try {
            Object resObj = serviceInvoker.invoke(call.getService(), call.getGroup(), call.getMethod(),
                call.getTimeout(), call.getParams(), call.getParamTypes());
            result = new Reply(200, resObj);
        } catch (RpcException e) {
            result = new Reply(e.getCode(), e.getMessage());
        } catch (NoSuchMethodException e) {
            result = new Reply(405, "unkown method: [" + call.getGroup() + "]" + e.getMessage());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            String message = "illegal access: [" + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + e.getMessage();
            result = new Reply(502, message);
        } catch (InvocationTargetException e) {
            Throwable t = getTargetException(e);
            String message = "service exception: " + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + t.getMessage();
            result = new Reply(500, message);
        } catch (Throwable e) {
            String message = "service exception: " + call.getGroup() + "]" + call.getService() + "." + call.getMethod()
                    + "(): " + e.getMessage();
            result = new Reply(509, message);
        }

        return new Response(request.getMessageId(), request.getSerializeType(), result);
    }

    private Throwable getTargetException(InvocationTargetException e) {
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
