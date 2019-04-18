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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.not.Not;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

public class Request {

    static final String usageString =
            "\nUsage: java Request [server] <subject> <text message>\n"
            + "\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
            + "\nUse the URL for user/pass/token authentication.\n";

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
            Tracer tracer = Not.initTracing("NATS OpenTracing Requestor");

            // Connect to the server
            Connection nc = Nats.connect(server);

            // Create a span context and inject our tracing information.
            Span span = tracer.buildSpan("Request").withTag("type", "requestor").start();
            SpanContext spanContext = span.context();

            // Publish the message using the tracer and spanContext to 
            // encode it.
            Future<Message> replyFuture = nc.request(subject, 
                Not.encode(tracer, spanContext,
                message.getBytes(StandardCharsets.UTF_8)));
            Message reply = replyFuture.get(5, TimeUnit.MINUTES);

            System.out.println();
            String logMsg = String.format(
                "Received reply \"%s\" on subject \"%s\"\n", 
                new String(reply.getData(), StandardCharsets.UTF_8), 
                reply.getSubject());
            
            span.log(logMsg);
            span.finish();

            System.out.printf("\n%s\n", logMsg);

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