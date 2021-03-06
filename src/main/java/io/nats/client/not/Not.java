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

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Binary;
import io.opentracing.propagation.Format;

/**
 * This class provides the java NATS open tracing implemention.
 */
public class Not {

    /**
     * The inner Carrier class is a internal opentracing carrier for tracing
     * NATS messages.
     *
     * A carrier is a high level abstraction over the data packaged into
     * requests that propagate span contects between distributed applications. 
     * A carrier allows the open tracing implemention to serialize and
     * deserialized data into a user specific implementation.  In this case, 
     * the carrier is a fairly simple wrapper around a ByteBuffer.
     *
     * More on Open Tracing carriers can be found here:
     * https://opentracing.io/guides/java/inject-extract/
     */
    static class Carrier implements Binary {
   
        ByteBuffer buffer = null;

        /**
         * Creates a NatsCarrier for extraction
         * 
         * @param size - initial buffer size, used for extraction.
         */
        public Carrier(byte[] rawCarrierData) {
            buffer = ByteBuffer.wrap(rawCarrierData);
        }

        /**
         * Helper function to get the remaining bytes from the buffer.
         */
        public byte[] getRemaining() {
            if (buffer == null) {
                return null;
            }
            
            int payloadLen = buffer.remaining();
            if (payloadLen == 0) {
                return null;
            }
        
            byte[] payload = new byte[payloadLen];
            buffer.get(payload);
            return payload;
        }

        /** 
         * Creates a NatsCarrier with an uninitalized buffer for injection
         */
        public Carrier() {}
        
        @Override
        public ByteBuffer injectionBuffer(int length) {
            if (length <= 0) {
                throw new IllegalArgumentException("length must be greater than zero");
            }

            buffer = ByteBuffer.allocate(length);        
            return buffer;
        }
        
        @Override
        public ByteBuffer extractionBuffer() {
            return buffer;
        }  
    }

    /**
     * encode generates a byte array with tracing information and
     * the NATS payload.
     * 
     * @param tracer The OpenTracing tracer
     * @param spanContext The span context of this trace
     * @param payload The payload you want to publish
     * @return a byte array with trace information and the payload
     */
    public static byte[] encode(Tracer tracer, SpanContext spanContext, byte[] payload) {
        if (tracer == null) {
            throw new IllegalArgumentException("tracer cannot be null");
        }

        if (spanContext == null) {
            throw new IllegalArgumentException("spanContext cannot be null");
        }

        Not.Carrier c = new Not.Carrier();
        tracer.inject(spanContext, Format.Builtin.BINARY, c);
        ByteBuffer bb = c.extractionBuffer();

        // Create a buffer to hold the span information and payload.
        // we have to use the position of the carriers buffer to determine
        // the size of the carrier information.
        int len = bb.position();
        byte[] buffer = new byte[len + payload.length];

        // Rewind the carrier buffer, then copy the carrier and payload into
        // our buffer.
        bb.rewind();
        bb.get(buffer, 0, len);
        System.arraycopy(payload, 0, buffer, len, payload.length);
        return buffer;
    }

    /**
     * Decodes a NATS message with trace information.
     * @param tracer the tracer to decode.
     * @param msg the NATS message to decode.
     * @return a TraceMessage
     */
    public static TraceMessage decode(Tracer tracer, io.nats.client.Message msg) {
        return new TraceMessage(tracer, msg);
    }

    /**
     * A helper function to initalize tracing
     * @param serviceName Name of the service.
     * @return an OpenTracing tracer
     */
    public static io.opentracing.Tracer initTracing(String serviceName) {
            SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv().withType("const").withParam(1);
            ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true);
            Configuration config = new Configuration(serviceName).withSampler(samplerConfig).withReporter(reporterConfig);
            return config.getTracer();
    }
}
