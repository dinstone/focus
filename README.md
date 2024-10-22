# Overview

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/dinstone/focus/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.dinstone.focus/focus-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=com.dinstone.focus)

**Focus** is the next generation cross language lightweight RPC framework. It can quickly and easily develop microservice applications, which greatly simplifies RPC programming.

[Focus](https://github.com/dinstone/focus) 是下一代跨平台、跨语言的轻量级RPC框架。旨在帮助开发者可以更加高效地构建和维护微服务应用程序。他们可以利用统一的接口和协议，在不同的平台和语言之间进行通信和协作，从而提高开发效率和系统可靠性，简化多平台下的RPC编程，可以很轻松地实现云端编程和移动端编程。

通常，评价一个 RPC 框架是否优秀、高效能，有 3 个基本标准：

* 简单易用，无侵入：不需要过多的研究使用文档，查看快速开始或 API 就能快速用起来，框架代码无需侵入业务代码就能完成调用。

* 抽象适度，可扩展：框架分层耦合合理、模块职责内聚、实现简洁易懂，能覆盖绝大多数场景，对特殊场景可通过设置或扩展来满足需求。

* 性能优越，可演进：“高性能”永远是一个绕不开的关注点，框架的实现也是编码能力的体现，保持 API 不变，但实现可持续迭代改进。

[Focus](https://github.com/dinstone/focus) 框架在高效能方面做了很多的努力，贯彻最小侵入性设计理念，坚持面向对象的SOLID原则，追求极简协议和代码简洁，能够让开发者和企业更轻松地集成和使用RPC框架。

对于希望使用和学习RPC框架的同学来说，选择一个高效、易用、灵活和可扩展的框架非常重要。Focus框架可以为他们提供更多的参考和选择，帮助他们更好地了解和掌握RPC框架的使用技巧和最佳实践。

# Features

* 跨语言支持。同时支持多种串行化协议：Jackson和Protobuff。
* 模块化API。模块化的客户端和服务端API，可扩展的系统架构核心小于1 MB。
* 分层架构。合理严谨的分层（包括API层、代理层、调用层、协议层、传输层）使得依赖最小化、可控，适用于更多运行环境。
* 可插拔的服务发现机制。支持 Consul，Nacos，Polaris等常见注册中心。
* 可插拔的调用拦截机制。可实现Logging、Tracing、Metrics、限流、熔断等服务安全、可观测性、服务治理功能。
* 支持同步调用、异步调用、泛化调用。满足各种场景下的不同诉求。
* 高效的自定义协议。二进制消息交换协议[Photon](https://github.com/dinstone/photon)和[Focus](https://github.com/dinstone/focus)的RPC协议。
* 不同级别的服务控制。全局级别、服务级别的序列化、压缩、超时、重试设置，方法级别的超时、重试设置。
* Spring boot 集成支持友好。简化Spring应用的集成、开发难度。

## Design Idea

* Modular client and server APIs, scalable system architecture, and framework core less than 1 MB in size.
* Support a variety of serialization protocol at the same time - Jackson、Protobuff、Protostuff
* Layered architecture, including API layer, Proxy layer, Invoke layer, Protocol layer, Transport layer
* Pluggable invoke interception mechanism, that facilitates extensions such as service security, observability, and service governance.

## Ease of use

* Out of the box client-side and server-side API
* Spring boot starter integration friendly
* Support synchronous, asynchronous and generalized calls

## Performance

* Efficient custom protocol ([Photon](https://github.com/dinstone/photon) message exchange protocol and Focus RPC protocol)
* High-performance NIO socket framework support - Netty4

# Quick Start

The quick start gives a basic example of running client and server on the same machine. For more advanced examples, please refer to the example project : [focus-examples](https://github.com/dinstone/focus-examples). For the detailed information about using and developing Focus, please jump to [Documents](#documents).

> The minimum requirements to run the quick start are:
>
> - JDK 1.8 or above
> - A java-based project management software like [Maven](maven) or [Gradle](gradle)

## Synchronous calls

1. create maven project focus-quickstart and add dependencies to pom.

```xml
<dependencies>
    <dependency>
        <groupId>com.dinstone.focus</groupId>
        <artifactId>focus-server-photon</artifactId>
        <version>1.4.1</version>
        <type>pom</type>
    </dependency>
    <dependency>
        <groupId>com.dinstone.focus</groupId>
        <artifactId>focus-client-photon</artifactId>
        <version>1.4.1</version>
        <type>pom</type>
    </dependency>
    <dependency>
        <groupId>com.dinstone.focus</groupId>
        <artifactId>focus-serialize-json</artifactId>
        <version>1.4.1</version>
    </dependency>
</dependencies>
```

2. create FooService interface.

```java
package focus.quickstart;

public interface FooService {

    public String hello(String name);

    public CompletableFuture<String> async(String name);
}
```

3. create FooService implement.

```java
package focus.quickstart.server;

import focus.quickstart.FooService;

public class FooServiceImpl implements FooService {

    public String hello(String name) {
        return "hello " + name;
    }

    public CompletableFuture<String> async(String name) {
        return CompletableFuture.completedFuture("async hello " + name);
    }
}
```

4. create focus server and exporting service.

```java
package focus.quickstart.server;

import java.io.IOException;

import com.dinstone.focus.server.FocusServer;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

import focus.quickstart.FooService;

public class FocusServerBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(FocusServerBootstrap.class);

    public static void main(String[] args) {
		ServerOptions serverOptions = new ServerOptions("focus.quickstart.server").listen("localhost", 3333);
		FocusServer server = new FocusServer(serverOptions);

		// exporting service
		server.exporting(FooService.class, new FooServiceImpl());

		server.start();
		LOG.info("server start");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.close();
		LOG.info("server stop");
    }

}
```

5. create focus client to importing service and invoke RPC.

```java
package focus.quickstart.client;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.FocusClient;

import focus.quickstart.FooService;

public class FocusClientBootstrap {

	public static void main(String[] args) throws Exception {
		ClientOptions option = new ClientOptions("focus.quickstart.client").connect("localhost", 3333);
		FocusClient client = new FocusClient(option);
		try {
			FooService fooService = client.importing(FooService.class);
			String reply = fooService.hello("dinstone");
			System.out.println(reply);

			CompletableFuture<String> rf = fooService.async("dinstone");
			System.out.println(rf.get());
		} finally {
			client.close();
		}
	}
}
```

## Asynchronous calls

1. another way to call RPC asynchronously is to create an asynchronous interface class in client side.

```java
package focus.quickstart.client;

import java.util.concurrent.CompletableFuture;

public interface FooAsyncService {
    public CompletableFuture<String> hello(String name);
}
```

2. create focus client to importing service and async invoke RPC.

```java
package focus.quickstart.client;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.ImportOptions;

public class FocusClientAsyncCallBootstrap {

	public static void main(String[] args) throws Exception {
		ClientOptions option = new ClientOptions("focus.quickstart.client").connect("localhost", 3333);
		FocusClient client = new FocusClient(option);
		try {
			ImportOptions importOptions = new ImportOptions("focus.quickstart.api.FooService");
			FooAsyncService fooService = client.importing(FooAsyncService.class, importOptions);
			CompletableFuture<String> replyFuture = fooService.hello("dinstone");
			System.out.println(replyFuture.get());
		} finally {
			client.close();
		}
	}

}
```

## Generic calls

1. the generalized call does not need to build the client interface class

2. create focus client to importing GenericService and sync/async invoke RPC.

```java
package focus.quickstart.client;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.client.GenericService;

public class FocusClientGenericCallBootstrap {

	public static void main(String[] args) throws Exception {
		ClientOptions option = new ClientOptions("focus.quickstart.client").connect("localhost", 3333);
		FocusClient client = new FocusClient(option);
		try {
			GenericService genericService = client.generic("focus.quickstart.server",
					"focus.quickstart.api.FooService");

			String reply = genericService.sync(String.class, "hello", "dinstone");
			System.out.println("sync call reply : " + reply);

			CompletableFuture<String> replyFuture = genericService.async(String.class, "hello", "dinstone");
			System.out.println("async call reply : " + replyFuture.get());
		} finally {
			client.close();
		}
	}

}
```

## Secure Call

1.Set server parameters, enable SSL in the accept options, and set certificate information.

```java
package focus.quickstart.ssl;

import java.io.IOException;
import java.security.cert.X509Certificate;

import com.dinstone.focus.server.FocusServer;
import com.dinstone.focus.server.ServerOptions;
import com.dinstone.focus.transport.photon.PhotonAcceptOptions;

import focus.quickstart.api.FooService;
import focus.quickstart.service.FooServiceImpl;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class SslFocusServerBootstrap {

    public static void main(String[] args) throws Exception {

        // setting ssl
        SelfSignedCertificate cert = new SelfSignedCertificate();
        PhotonAcceptOptions acceptOptions = new PhotonAcceptOptions();
        acceptOptions.setEnableSsl(true);
        acceptOptions.setPrivateKey(cert.key());
        acceptOptions.setCertChain(new X509Certificate[] { cert.cert() });

        // setting accept options
        ServerOptions serverOptions = new ServerOptions("focus.quickstart.server").listen("localhost", 3333)
                .setAcceptOptions(acceptOptions);

        FocusServer server = new FocusServer(serverOptions);

        // exporting service
        server.exporting(FooService.class, new FooServiceImpl());

        server.start();
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.close();
    }
}
```

2.Set client parameters and enable SSL in the connection options.

```java
package focus.quickstart.ssl;

import java.util.concurrent.CompletableFuture;

import com.dinstone.focus.client.ClientOptions;
import com.dinstone.focus.client.FocusClient;
import com.dinstone.focus.transport.photon.PhotonConnectOptions;

import focus.quickstart.api.FooService;

public class SslFocusClientBootstrap {

    public static void main(String[] args) throws Exception {
        PhotonConnectOptions connectOptions = new PhotonConnectOptions();
        // setting ssl
        connectOptions.setEnableSsl(true);
        ClientOptions clientOptions = new ClientOptions("focus.quickstart.client").connect("localhost", 3333)
                .setConnectOptions(connectOptions);
        FocusClient client = new FocusClient(clientOptions);
        try {
            FooService fooService = client.importing(FooService.class);
            String reply = fooService.hello("dinstone");
            System.out.println(reply);

            CompletableFuture<String> rf = fooService.async("dinstone");
            System.out.println(rf.get());
        } finally {
            client.close();
        }
    }

}
```

# Documents

- [Wiki](https://github.com/dinstone/focus/wiki)
- [Wiki(中文)](https://github.com/dinstone/focus/wiki/home_zh)

# License

Focus is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
