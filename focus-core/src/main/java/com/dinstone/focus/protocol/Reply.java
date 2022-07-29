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
package com.dinstone.focus.protocol;

import java.io.Serializable;

import com.dinstone.focus.exception.InvokeException;

/**
 * The Reply is the abstract of RPC response.
 * 
 * @author dinstone
 * 
 */
public class Reply implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private Attach attach = new Attach();

    private Object data;

    public Reply() {
        super();
    }

    public Reply(Object result) {
        setData(result);
    }

    /**
     * the attach to get
     * 
     * @return
     */
    public Attach attach() {
        return attach;
    }

    /**
     * the data to get
     * 
     * @return the data
     * 
     * @throws Exception
     * 
     * @see Reply#data
     */
    public Object getData() {
        return data;
    }

    /**
     * the data to set
     * 
     * @param data
     * 
     * @see Reply#data
     */
    public void setData(Object data) {
        if (data instanceof Throwable && !(data instanceof InvokeException)) {
            this.data = new InvokeException(199, (Throwable) data);
        } else {
            this.data = data;
        }
    }

}
