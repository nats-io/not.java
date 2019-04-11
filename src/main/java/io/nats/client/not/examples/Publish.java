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

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.not.Not;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Publish {

    static final String usageString =
            "\nUsage: java NatsPub [server] <subject> <text message>\n"
                    + "\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
                    + "\nSet the environment variable NATS_NKEY to use challenge response authentication by setting a file containing your private key.\n"
                    + "\nSet the environment variable NATS_CREDS to use JWT/NKey authentication by setting a file containing your user creds.\n"
                    + "\nUse the URL for user/pass/token authentication.\n";

    public static void traceResult(byte[] message) {
        io.opentracing.Tracer tracer = Not.initTracing("NATS Sample Publisher Test Subscriber");
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("Receive");

        java.nio.ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        try {
            SpanContext spanContext = tracer.extract(Format.Builtin.BINARY, byteBuffer);
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext);
            }
        } catch (Exception e) {
            spanBuilder.withTag("Error", "Extract from request exception: " + e.getMessage());
        }
    }

    public static void main(String args[]) {
        String subject;
        String message;
        String server;

        if (args.length == 3) {
            server = args[0];
            subject = args[1];
            message = args[2];
        } else if (args.length == 2) {
            server = Options.DEFAULT_URL;
            subject = args[0];
            message = args[1];
        } else {
            usage();
            return;
        }

        try {

            // Initialize our tracer
            Tracer tracer = Not.initTracing("NATS OpenTracing Publisher");

            // Connect to the NATS server
            System.out.printf("\n\nSending %s on %s, server is %s\n\n", message, subject, server);
            Connection nc = Nats.connect(server);
                        
            // Create a span context and inject our tracing information.
            Span span = tracer.buildSpan("Publish").withTag("type", "publisher").start();
            SpanContext spanContext = span.context();

            // Create a carrier to inject the span information.
            ByteBuffer carrier = ByteBuffer.allocate(128);
            tracer.inject(spanContext, Format.Builtin.BINARY, carrier);

            // Publish the message, createTracePayload passing in the carrier.
            nc.publish(subject, Not.createTracePayload(carrier, message.getBytes(StandardCharsets.UTF_8)));
            span.log("Published message.");
            span.finish();

            nc.flush(Duration.ofSeconds(5));
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
