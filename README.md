# Focus
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/dinstone/focus/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.dinstone.focus/focus-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=com.dinstone.focus)

# Overview
**Focus** is the next generation cross language lightweight RPC framework. It can quickly and easily develop microservice applications, which greatly simplifies RPC programming.

# Features
## Design Idea
* Modular client and server APIs, scalable system architecture, and framework core less than 1 MB in size.
* Support a variety of serialization protocol at the same time - Jackson、Protobuff、Protostuff
* Layered architecture, including API layer, Proxy layer, Invoke layer, Protocol layer, Transport layer
* Pluggable service discovery - registry with [Clutch](https://github.com/dinstone/focus/tree/master/focus-clutch) for Zookeeper, Consul, Nacos

## Ease of use
* Out of the box client-side and server-side API
* Spring boot starter integration friendly
* Support synchronous, asynchronous and generalized calls

## Performance
* Efficient custom protocol ([Photon](https://github.com/dinstone/photon) message exchange protocol and Focus RPC protocol)
* High-performance NIO socket framework support - Netty4

# Quick Start
The quick start gives a basic example of running client and server on the same machine. For more advanced examples, please refer to the example project : [focus-example](https://github.com/dinstone/focus/tree/master/focus-example). For the detailed information about using and developing Focus, please jump to [Documents](#documents).

> The minimum requirements to run the quick start are:
>
> - JDK 1.8 or above
> - A java-based project management software like [Maven][maven] or [Gradle][gradle]

## Synchronous calls
1. create maven project focus-quickstart and add dependencies to pom.
```xml
<dependency>
	<groupId>com.dinstone.focus</groupId>
	<artifactId>focus-server</artifactId>
	<version>0.9.7</version>
</dependency>
<dependency>
	<groupId>com.dinstone.focus</groupId>
	<artifactId>focus-client</artifactId>
	<version>0.9.7</version>
</dependency>
<dependency>
	<groupId>com.dinstone.focus</groupId>
	<artifactId>focus-serialize-json</artifactId>
	<version>0.9.7</version>
</dependency>
```

2. create FooService interface.
```java
package focus.quickstart;

public interface FooService {
    public String hello(String name);
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
        ServerOptions serverOptions = new ServerOptions().listen("localhost", 3333)
                .setEndpoint("focus.quickstart.server");
        FocusServer server = new FocusServer(serverOptions);

        // exporting service
        server.exporting(FooService.class, new FooServiceImpl());

        // server.start();
        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.destroy();
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

    public static void main(String[] args) {
        ClientOptions option = new ClientOptions().setEndpoint("focus.quickstart.client").connect("localhost", 3333);
        FocusClient client = new FocusClient(option);
        try {
            FooService fooService = client.importing(FooService.class);
            String reply = fooService.hello("dinstone");
            System.out.println(reply);
        } finally {
            client.destroy();
        }
    }

}
```

## Asynchronous calls
1. create service interface  for async invoke RPC.
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
        ClientOptions option = new ClientOptions().setEndpoint("focus.quickstart.client").connect("localhost", 3333);
        FocusClient client = new FocusClient(option);
        try {
            ImportOptions importOptions = new ImportOptions("focus.quickstart.FooService");
            FooAsyncService fooService = client.importing(FooAsyncService.class, importOptions);
            CompletableFuture<String> replyFuture = fooService.hello("dinstone");
            System.out.println(replyFuture.get());
        } finally {
            client.destroy();
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
        ClientOptions option = new ClientOptions().setEndpoint("focus.quickstart.client").connect("localhost", 3333);
        FocusClient client = new FocusClient(option);
        try {
            GenericService genericService = client.generic("focus.quickstart.FooService", null, 3000);

            String reply = genericService.sync(String.class, "hello", "dinstone");
            System.out.println("sync call reply : " + reply);

            CompletableFuture<String> replyFuture = genericService.async(String.class, "hello", "dinstone");
            System.out.println("async call reply : " + replyFuture.get());
        } finally {
            client.destroy();
        }
    }

}
```

# Documents
- [Wiki](https://github.com/dinstone/focus/wiki)
- [Wiki(中文)](https://github.com/dinstone/focus/wiki/zh)

# License
Focus is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
