# OpenTracing with NATS

Over the years, we've had periodic requests to support distributed tracing in
[NATS](https://nats.io).  While distributed tracing is valuable,
philosophically we did not want to add external dependencies to NATS,
internally or via API.  Nor did we want to provide guidance that would make
developers work in ways that  didn't feel natural or aligned with the tenets
of NATS.  We left it to the application developers using NATS.

[OpenTracing](https://opentracing.io) changes this and offers a way to
implement distributed tracing with NATS that aligns with our goals and
philosophy of simplicity and ease of use and does not require adding
dependencies into NATS.  This repository provides a reference to
facilitate the use of OpenTracing with NATS enabled applications.

## What is OpenTracing

OpenTracing provides a non-intrusive vendor-neutral API and instrumentation
for distributed tracing, with wide language support.  Because each use case
is slightly different, we've decided not to provide specific implementations
at this point.  Instead we are providing a reference architecture with
examples demonstrating easy usage of OpenTracing with NATS.  In line with
other NATS projects, these canonical examples are provided in
[Go](https://golang.org), but this approach should port smoothly into many
other languages.  More language examples will be added soon.

## How it works

OpenTracing is actually fairly simple to implement in an applicaton.
A "Trace" is defined, and then sets up "spans" to represent an operation
or event and log information, which is reported to the OpenTracing aggregator
for reporting and display.

To propogate, span contexts are serialized into a NATS message using the
binary format. We provide a `Not` class which will do what is needed to
[inject](https://opentracing.io/docs/overview/inject-extract/) span contexts
into messages and to extract them on the other side.

Using the API is relatively simple, and the same steps are taken in
sending or receiving data regardless of which message pattern is used.

### Sending a message

After establishing an opentracing tracer, span, and span context, simply
pass the payload into the static `Not.encode` function. `Not.encode` will
encode the payload to be a payload traceable by by the Jaeger Open Tracing
system.

```java
    Tracer tracer = Not.initTracing("Your sending service");
    Connection nc = Nats.connect("demo.nats.io");

    Span span = tracer.buildSpan("Send Data").withTag("type", "sender").start();
    SpanContext spanContext = span.context();

    // Encode the message with trace data before sending
    nc.publish(subject, Not.encode(tracer, spanContext,
        message.getBytes(StandardCharsets.UTF_8)));

    span.finish();
```

### Receiving a message

When receiving a message, generate a `TraceMessage` from `Not.decode`.  You can
use this as a regular NATS message, except there is another API to get the 
span context.  If `TraceMessage.getSpanContext()` is null, the message has no
trace information and should be handled normally.

```java
    Tracer tracer = Not.initTracing("Your receiving service");

    Connection nc = Nats.connect("demo.nats.io");

    Dispatcher d = nc.createDispatcher((msg) -> {

        // Decode the raw payload into a trace message.
        TraceMessage tm = Not.decode(tracer, msg);

        // If there is a span context, the message has trace information.
        SpanContext sc = tm.getSpanContext();
        if (sc != null) {
            Span recvSpan = tracer.buildSpan("Process data")
                .asChildOf(sc).withTag("type", "receiver").start();

            doSomething(tm.getData());

            recvSpan.finish();
        } else {
            // This was not a trace message, so process normally.  Note that
            // you can still use the trace message object here simplifying
            // your code.
            doSomething(tm.getData());
        }
    });

    // subscribe to receive messages and and other application code.
    // ...
```

Check out the [examples](./examples) for additional usage.

## Setting up the Jaeger Tracer

To run the the examples, we setup [Jaeger](https://www.jaegertracing.io/)
as the OpenTracing tracer with its convenient "all-in-one" docker image.
Jaeger is a CNCF open source, end-to-end distributed tracing project.

```bash
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 9411:9411 \
  jaegertracing/all-in-one:1.9
```

See Jaeger's [getting started](https://www.jaegertracing.io/docs/1.9/getting-started/)
documentation for more information.

## Building

To build, run `gradle build fatJar`:

```bash
$ gradle build fatJar
BUILD SUCCESSFUL in 2s
5 actionable tasks: 5 executed
```

## Examples

These examples use `demo.nats.io` as the server name.  If you choose to
run a NATS [server](http://github.com/nats-io/gnatsd) locally, use `localhost`.

## Request/Reply Examples

* [Request Example](./src/main/java/io/nats/client/not/examples/Request.java)
* [Reply Example](./src/main/java/io/nats/client/not/examples/Reply.java)

Open two terminals, in one terminal use the example helper script
and run:

```bash
./scripts/run_example.sh Reply demo.nats.io foo
```

In the other terminal, run:

```bash
./scripts/run_example.sh Request demo.nats.io foo help
```

### Request Output

```text
[main] INFO io.jaegertracing.Configuration - Initialized tracer=JaegerTracer(version=Java-0.34.1-SNAPSHOT, serviceName=NATS OpenTracing Requestor, reporter=CompositeReporter(reporters=[RemoteReporter(sender=UdpSender(), closeEnqueueTimeout=1000), LoggingReporter(logger=org.slf4j.impl.SimpleLogger(io.jaegertracing.internal.reporters.LoggingReporter))]), sampler=ConstSampler(decision=true, tags={sampler.type=const, sampler.param=true}), tags={hostname=MacBook-Pro-2.local, jaeger.version=Java-0.34.1-SNAPSHOT, ip=192.168.0.6}, zipkinSharedRpcSpan=false, expandExceptionLogs=false, useTraceId128Bit=false)

[main] INFO io.jaegertracing.internal.reporters.LoggingReporter - Span reported: 5af849840792a3d8:5af849840792a3d8:0:1 - Request

Received reply "Here's some help" on subject "_INBOX.60KGSMo5oQOK1OOYNc0W6J.60KGSMo5oQOK1OOYNc0WBd"
```

### Reply Output

```text
[main] INFO io.jaegertracing.Configuration - Initialized tracer=JaegerTracer(version=Java-0.34.1-SNAPSHOT, serviceName=NATS OpenTracing Replier, reporter=CompositeReporter(reporters=[RemoteReporter(sender=UdpSender(), closeEnqueueTimeout=1000), LoggingReporter(logger=org.slf4j.impl.SimpleLogger(io.jaegertracing.internal.reporters.LoggingReporter))]), sampler=ConstSampler(decision=true, tags={sampler.type=const, sampler.param=true}), tags={hostname=MacBook-Pro-2.local, jaeger.version=Java-0.34.1-SNAPSHOT, ip=192.168.0.6}, zipkinSharedRpcSpan=false, expandExceptionLogs=false, useTraceId128Bit=false)

[nats:3] INFO io.jaegertracing.internal.reporters.LoggingReporter - Span reported: 3098e369b9c2c3e0:6392c76ebe76c84a:3098e369b9c2c3e0:1 - Process request
```

### Viewing the Request/Reply output in the Jaeger UI

Navigate with a browser to <http://localhost:16686>.  Find the _NATS Requestor_
service in the services list and click the _Find Traces_ button.  Click on
the _NATS OpenTracing Requestor_ service and you will see a screen similar to the following:

![Jaeger UI Request Reply](./images/Java-RequestReply.jpg)

You can see the entire span of the request and the associated replier span.

## Publish/Subscribe Examples

* [Publisher Example](./src/main/java/io/nats/client/not/examples/Publish.java)
* [Subscriber Example](./src/main/java/io/nats/client/not/examples/Subscribe.java)

Open three terminals, in the first two terminals go to the subscribe example
directory and run:

```bash
./scripts/run_example.sh Subscribe demo.nats.io foo
```

and in the second terminal:

```bash
./scripts/run_example.sh Subscribe demo.nats.io foo
```

And finally in the third terminal go to the publish example directory:

```bash
./scripts/run_example.sh Publish demo.nats.io foo hello
```

Navigate with a browser to <http://localhost:16686>.  Find the _NATS Publisher_
service in the services list and click the _Find Traces_ button.  Click on the
_NATS OpenTracing Publisher_ service and you will see a screen to the following:

![Jaeger UI Publish Subscribe](./images/Java-PubSub.jpg)

You can see the publish span and the two associated subscriber spans.  The gap
the middle includes the NATS client library publishing the message, the NATS server
routing and fanning out the message, and the subscriber NATS clients receiving the
messages and passing them to application code where the subscriber span is reported.

### Subscriber Output

```text
[main] INFO io.jaegertracing.Configuration - Initialized tracer=JaegerTracer(version=Java-0.34.1-SNAPSHOT, serviceName=NATS OpenTracing Subscriber, reporter=CompositeReporter(reporters=[RemoteReporter(sender=UdpSender(), closeEnqueueTimeout=1000), LoggingReporter(logger=org.slf4j.impl.SimpleLogger(io.jaegertracing.internal.reporters.LoggingReporter))]), sampler=ConstSampler(decision=true, tags={sampler.type=const, sampler.param=true}), tags={hostname=MacBook-Pro-2.local, jaeger.version=Java-0.34.1-SNAPSHOT, ip=192.168.0.6}, zipkinSharedRpcSpan=false, expandExceptionLogs=false, useTraceId128Bit=false)

Connected to demo.nats.io, subscribed to foo.

[main] INFO io.jaegertracing.internal.reporters.LoggingReporter - Span reported: 2d16a66cb0ea9a3:d9739bb64e5af66a:2d16a66cb0ea9a3:1 - Process message
Received message "hello" on subject "foo"
```

### Publisher Output

```text
[main] INFO io.jaegertracing.Configuration - Initialized tracer=JaegerTracer(version=Java-0.34.1-SNAPSHOT, serviceName=NATS OpenTracing Publisher, reporter=CompositeReporter(reporters=[RemoteReporter(sender=UdpSender(), closeEnqueueTimeout=1000), LoggingReporter(logger=org.slf4j.impl.SimpleLogger(io.jaegertracing.internal.reporters.LoggingReporter))]), sampler=ConstSampler(decision=true, tags={sampler.type=const, sampler.param=true}), tags={hostname=MacBook-Pro-2.local, jaeger.version=Java-0.34.1-SNAPSHOT, ip=192.168.0.6}, zipkinSharedRpcSpan=false, expandExceptionLogs=false, useTraceId128Bit=false)


Sending hello on foo, server is nats://localhost:4222

[main] INFO io.jaegertracing.internal.reporters.LoggingReporter - Span reported: 2d16a66cb0ea9a3:2d16a66cb0ea9a3:0:1 - Publish
```

## Our sponsor for this project

Many thanks to [MasterCard](http://mastercard.com) for sponsoring this project.

We appreciate MasterCard's support of NATS, CNCF, and the OSS community.
