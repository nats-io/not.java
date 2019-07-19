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

import io.nats.client.Message;
import io.nats.client.Subscription;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class TraceMessage implements io.nats.client.Message {
    private Message message;
    private byte[] payload;
    private SpanContext spanContext;

    /**
     * Creates a trace message from a NATS message payload an tracer.
     * 
     * @param tracer
     * @param rawPayload
     */
    TraceMessage(Tracer tracer, byte[] rawPayload) {
        if (rawPayload == null) {
            spanContext = null;
            payload = null;
        }

        // Use a carrier to deserialize the encoded message.
        // The received message byte array has |carrier|payload|
        Not.Carrier c = new Not.Carrier(rawPayload);
        spanContext = tracer.extract(Format.Builtin.BINARY, c);
        if (spanContext != null) {
            payload = c.getRemaining();
        } else {
            // There's no trace data in this message
            payload = rawPayload;
        }
    }

    /**
     * Creates a trace message.  Use Not.decode instead.
     * @param tracer The OpenTracing tracer
     * @param msg a NATS message payload
     */
    TraceMessage(Tracer tracer, Message msg) {
        this(tracer, msg.getData());
        message = msg;
    }

    /**
     * Gets the span context of a trace message.
     * @return a span context, null if message did not contain trace
     * inforamtion.
     */
    public SpanContext getSpanContext() {
        return spanContext;
    }

    /*
     *  NATS Message overrides
     */
    @Override
    public String getSubject() {
        return message.getSubject();
    }

    @Override
    public String getReplyTo() {
        return message.getReplyTo();
    }

    @Override
    public byte[] getData() {
        return payload;
    }

    @Override
    public Subscription getSubscription() {
        return message.getSubscription();
    }

	@Override
	public String getSID() {
		return message.getSID();
	}
}