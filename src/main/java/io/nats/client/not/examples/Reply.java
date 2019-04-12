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

package io.nats.client.not.examples;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.not.Not;
import io.nats.client.not.TraceMessage;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

public class Reply {

    static final String usageString =
            "\nUsage: java NatsReply [server] <subject>\n"
            + "\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
            + "\nUse the URL for user/pass/token authentication.\n";

    public static void main(String args[]) {
        String subject;
        String server;

        if (args.length == 2) {
            server = args[0];
            subject = args[1];
        } else if (args.length == 1) {
            server = Options.DEFAULT_URL;
            subject = args[0];
        } else {
            usage();
            return;
        }

        try {
            // Build our tracer and a parent span for the received message.
            Tracer tracer = Not.initTracing("NATS OpenTracing Replier");

            Connection nc = Nats.connect(server);
            CountDownLatch latch = new CountDownLatch(1); // dispatcher runs callback in another thread
            
            System.out.println();
            Dispatcher d = nc.createDispatcher((msg) -> {

                // Create the trace message from the received NATS message
                TraceMessage tm = Not.decode(tracer, msg);

                String logMsg = String.format("Received message \"%s\" on subject \"%s\", replying to %s\n", 
                                        new String(tm.getData(), StandardCharsets.UTF_8), 
                                        tm.getSubject(), tm.getReplyTo());
            
                // Extract the SpanContext from the trace message.  It will be null
                // if this message is not a trace message.
                SpanContext sc = tm.getSpanContext();
                if (sc != null) {
                    Span recvSpan = tracer.buildSpan("Process request").asChildOf(sc).withTag("type", "replier").start();
                    recvSpan.log(logMsg);

                    // respond inside the span to get accurate timing.
                    nc.publish(tm.getReplyTo(), "Here's some help".getBytes(StandardCharsets.UTF_8));
                    
                    recvSpan.finish();
                } else {
                    // This was not a trace message.  We can still use the 
                    // trace message though.
                    nc.publish(tm.getReplyTo(), tm.getData());
                }
                latch.countDown();
            });
            d.subscribe(subject);

            nc.flush(Duration.ofSeconds(5));

            latch.await();

            nc.closeDispatcher(d); // This isn't required, closing the connection will do it
            nc.close();
            
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    static void usage() {
        System.err.println(usageString);
        System.exit(-1);
    }
}