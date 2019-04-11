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

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Subscription;
import io.nats.client.not.Not;
import io.nats.client.not.TraceMessage;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

public class Subscribe {

    static final String usageString =
            "\nUsage: java Subscribe [server] <subject>\n"
            + "\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
            + "\nUse the URL for user/pass/token authentication.\n";

    public static void main(String args[]) {
        String subject;
        String server;
        
        if (args.length == 1) {
            server = Options.DEFAULT_URL;
            subject = args[0];
        } else {
            usage();
            return;
        }

        try {
            // Build our tracer and a parent span for the received message.
            Tracer tracer = Not.initTracing("NATS OpenTracing Subscriber");

            // Connect to the NATS server and subscribe.
            System.out.printf("\nTrying to connect to %s, and listen to %s.\n\n", server, subject);
            Connection nc = Nats.connect(server);
            Subscription sub = nc.subscribe(subject);
            nc.flush(Duration.ofSeconds(5));

            // Receive the message from the publisher, and create a 
            // trace message from the raw NATS message.
            Message msg = sub.nextMessage(Duration.ofHours(1));
            TraceMessage tm = Not.createTraceMessage(tracer, msg);

            String logMsg = String.format("Received message \"%s\" on subject \"%s\"\n", 
                new String(tm.getData(), StandardCharsets.UTF_8), 
                tm.getSubject());

            // Extract the SpanContext from the trace message.  It will be null
            // if this message is not a trace message.
            SpanContext sc = tm.getSpanContext();
            if (sc != null) {
                Span recvSpan = tracer.buildSpan("Process message").asChildOf(sc).withTag("type", "subscriber").start();
                recvSpan.log(logMsg);
                recvSpan.finish();
            }

            System.out.printf(logMsg);

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