# What
**Focus** is a lightweight RPC framework. It enables quick and easy development of RPC applications. It greatly simplifies RPC programming.

# Features
## Design Idea
* Unified API for client and server
* Support a variety of serialization protocol at the same time - Jackson and Protobuff
* Layered architecture, including API layer, Proxy layer, Invoke layer, Protocol layer, Transport layer
* Pluggable service discovery - registry with [Clutch](https://github.com/dinstone/clutch) for Zookeeper, Consul

## Ease of use
* Out of the box client-side and server-side API
* Spring integration friendly

## Performance
* Efficient custom protocol ([Photon](https://github.com/dinstone/photon) message exechange protocol and Focus RPC protocol)
* High-performance NIO socket framework support - Netty4

# Quick Start
select API dependency:

		<dependency>
			<groupId>com.dinstone.focus</groupId>
			<artifactId>focus-client</artifactId>
			<version>0.8.0</version>
		</dependency>
		<dependency>
			<groupId>com.dinstone.focus</groupId>
			<artifactId>focus-server</artifactId>
			<version>0.8.0</version>
		</dependency>


if you need service registry and discovery, please add dependencies :

	<dependency>
		<groupId>com.dinstone.clutch</groupId>
		<artifactId>clutch-zookeeper</artifactId>
		<version>1.2.0</version>
		<exclusions>
			<exclusion>
				<groupId>log4j</groupId>
				<artifactId>log4j</artifactId>
			</exclusion>
		</exclusions>
	</dependency>
	
# Example
For more details, please refer to the example project : [focus-example](https://github.com/dinstone/focus/tree/master/focus-example)

## java programming by API
### export service:
```java
	Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        Tracing tracing = Tracing.newBuilder().localServiceName("focus.server").sampler(Sampler.create(1))
                .addSpanHandler(spanHandler).build();

        final Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.SERVER);

        FilterInitializer filterInitializer = new FilterInitializer() {

            @Override
            public void init(FilterChain chain) {
                chain.addFilter(tf);
            }
        };

        ServerOptions serverOptions = new ServerOptions();
        serverOptions.listen("localhost", 3333);
        serverOptions.setFilterInitializer(filterInitializer);
        Server server = new Server(serverOptions);
        server.exporting(DemoService.class, new DemoServiceImpl());
        // server.start();
        LOG.info("server start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.destroy();
        LOG.info("server stop");
```

### import service:
```java
	Sender sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);
        Tracing tracing = Tracing.newBuilder().localServiceName("focus.client").addSpanHandler(spanHandler)
                .sampler(Sampler.create(1)).build();

        Filter tf = new TracingFilter(RpcTracing.create(tracing), Kind.CLIENT);

        FilterInitializer filterInitializer = new FilterInitializer() {

            @Override
            public void init(FilterChain chain) {
                chain.addFilter(tf);
            }
        };

        ConnectOptions connectOptions = new ConnectOptions();
        ClientOptions option = new ClientOptions().connect("localhost", 3333).setConnectOptions(connectOptions)
                .setFilterInitializer(filterInitializer);
        Client client = new Client(option);
        final DemoService ds = client.importing(DemoService.class);

        LOG.info("int end");

        try {
        	ds.hello(null);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        client.destroy();
```
