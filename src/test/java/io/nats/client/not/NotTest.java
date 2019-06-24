// Copyright 2019 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.not;

import org.junit.Test;

import io.nats.client.Message;
import io.nats.client.impl.NatsMessageCheater;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

import static org.junit.Assert.*;

public class NotTest {
    @Test
    public void simpleInitTest() {
        Not.initTracing("test");
    }

    @Test
    public void testEndToEnd() {

        // Publish side...
        Tracer sendTracer = Not.initTracing("send");
        Span span = sendTracer.buildSpan("sendspan").start();
        span.setBaggageItem("k1", "v1");
        SpanContext sendContext = span.context();

        String origData = new String("hello");

        // encode, here we would send the data over the wire, and it'd be received
        // as a nats message on the other side.
        byte[] wireData = Not.encode(sendTracer, sendContext, origData.getBytes());

        // Receive side...
        Message m = NatsMessageCheater.createMessage("foo", "bar", wireData);

        Tracer recvTracer = Not.initTracing("receive");
        TraceMessage tm = Not.decode(recvTracer, m);
        assertTrue(new String(tm.getData()).equals(origData));
        assertTrue(tm.getSubject().equals("foo"));
        assertTrue(tm.getReplyTo().equals("bar"));
        
        SpanContext recvContext = tm.getSpanContext();
        Span childSpan = recvTracer.buildSpan("foo").asChildOf(recvContext).start();
        assertTrue(childSpan.getBaggageItem("k1").equals("v1"));
    }

    @Test
    public void testNonTraceMessage() {

        // Publish side...
        byte[] wireData = new String("hello").getBytes();
        Message m = NatsMessageCheater.createMessage("foo", "bar", wireData);

        // Receive side...
        Tracer recvTracer = Not.initTracing("receive");
        TraceMessage tm = Not.decode(recvTracer, m);
        assertTrue(new String(tm.getData()).equals("hello"));
        assertTrue(tm.getSubject().equals("foo"));
        assertTrue(tm.getReplyTo().equals("bar"));
        
        SpanContext recvContext = tm.getSpanContext();
        assertTrue(recvContext == null);
    }    
}
