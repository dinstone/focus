/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
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

/**
 * The Call is the abstract of RPC request.
 *
 * @author dinstone
 *
 */
public class Call implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String EMPTY_VALUE = "";

	public static final String CONSUMER_KEY = "call.consumer";

	public static final String PROVIDER_KEY = "call.provider";

	public static final String SERVICE_KEY = "call.service";

	public static final String METHOD_KEY = "call.method";

	public static final String TIMEOUT_KEY = "call.timeout";

	/**
	 * source application identity
	 */
	private String consumer;

	/**
	 * target application identity
	 */
	private String provider;

	private String service;

	private String method;

	private int timeout;

	private Object parameter;

	private Attach attach = new Attach();

	public Call(String service, String method, Object parameter) {
		this.service = service;
		this.method = method;
		this.parameter = parameter;
	}

	public String getService() {
		return service;
	}

	public String getMethod() {
		return method;
	}

	public Object getParameter() {
		return parameter;
	}

	public String getConsumer() {
		if (consumer == null) {
			return EMPTY_VALUE;
		}
		return consumer;
	}

	public void setConsumer(String consumer) {
		this.consumer = consumer;
	}

	public String getProvider() {
		if (provider == null) {
			return EMPTY_VALUE;
		}
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
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
	 * the attach to set
	 *
	 * @param other
	 *
	 * @return
	 */
	public Call attach(Attach other) {
		if (other != null) {
			attach.putAll(other);
		}
		return this;
	}

	@Override
	public String toString() {
		return "Call [consumer=" + consumer + ", provider=" + provider + ", service=" + service + ", method=" + method
				+ ", timeout=" + timeout + ", parameter=" + parameter + ", attach=" + attach + "]";
	}

}
