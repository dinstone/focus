# What
**Focus** is the next generation cross language lightweight RPC framework. It can quickly and easily develop microservice applications, which greatly simplifies RPC programming.

# Features
## Design Idea
* Unified API for client and server
* Support a variety of serialization protocol at the same time - Jackson and Protobuff
* Layered architecture, including API layer, Proxy layer, Invoke layer, Protocol layer, Transport layer
* Pluggable service discovery - registry with [Clutch](https://github.com/dinstone/focus/tree/master/focus-clutch) for Zookeeper, Consul

## Ease of use
* Out of the box client-side and server-side API
* Spring boot starter integration friendly

## Performance
* Efficient custom protocol ([Photon](https://github.com/dinstone/photon) message exchange protocol and Focus RPC protocol)
* High-performance NIO socket framework support - Netty4

# Quick Start
select API dependency:

		<dependency>
			<groupId>com.dinstone.focus</groupId>
			<artifactId>focus-client</artifactId>
			<version>0.9.5</version>
		</dependency>
		<dependency>
			<groupId>com.dinstone.focus</groupId>
			<artifactId>focus-server</artifactId>
			<version>0.9.5</version>
		</dependency>


if you need service registry and discovery, please add dependencies :

	<dependency>
		<groupId>com.dinstone.focus.clutch</groupId>
		<artifactId>focus-clutch-zookeeper</artifactId>
		<version>0.9.5</version>
		<exclusions>
			<exclusion>
				<groupId>log4j</groupId>
				<artifactId>log4j</artifactId>
			</exclusion>
		</exclusions>
	</dependency>
	
	or
	
	<dependency>
		<groupId>com.dinstone.focus.clutch</groupId>
		<artifactId>focus-clutch-consul</artifactId>
		<version>0.9.5</version>
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

        ServerOptions serverOptions = new ServerOptions();
        serverOptions.listen("localhost", 3333).setEndpoint("focus.example.server").addFilter(tf);
        FocusServer server = new FocusServer(serverOptions);
        server.publish(DemoService.class, new DemoServiceImpl());
        server.publish(OrderService.class, new OrderServiceImpl(null, null));

        server.publish(AuthenService.class, "AuthenService", null, 0, new AuthenService());

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

        ClientOptions option = new ClientOptions().setEndpoint("focus.example.client").connect("localhost", 3333)
                .setConnectOptions(new ConnectOptions()).addFilter(tf);
        FocusClient client = new FocusClient(option);
        final DemoService ds = client.reference(DemoService.class);
        
        // common invoke
        try {
        	ds.hello(null);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        // generic service sync invoke
        GenericService gs = client.genericService("com.dinstone.focus.example.OrderService", "", 30000);
        Map<String, String> p = new HashMap<String, String>();
        p.put("sn", "S001");
        p.put("uid", "U981");
        p.put("poi", "20910910");
        p.put("ct", "2022-06-17");

        Map<String, Object> r = gs.sync(HashMap.class, "findOldOrder", Map.class, p);
        System.out.println("result =  " + r);
        
        // generic service async invoke
        Future<String> future = gs.async(String.class, "hello", String.class, "dinstone");
        System.out.println("result =  " + future.get());
        
        client.destroy();
```
