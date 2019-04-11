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

import java.nio.ByteBuffer;

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
     * Creates a trace message.  Use Not.createTraceMessage instead.
     * @param msg
     */
    TraceMessage(Tracer tracer, Message msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        // Use a byte buffer to deserialize the encoded message.
        // The received message byte array has |carrier|payload|
        byte[] raw = msg.getData();
        ByteBuffer buf = ByteBuffer.allocate(raw.length);
        buf.put(raw);
        buf.rewind();

        // extract the carrier
        spanContext = tracer.extract(Format.Builtin.BINARY, buf);

        // get the payload
        int payloadLen = buf.remaining();
        if (payloadLen > 0) {
            payload = new byte[payloadLen];
            buf.get(payload);
        } else {
            payload = null;
        }

        // Assign the message;
        message = msg;
    }

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
}