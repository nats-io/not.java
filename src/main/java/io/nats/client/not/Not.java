package io.nats.client.not;

import java.nio.ByteBuffer;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Tracer;

/**
 * This class provides the java NATS open tracing implemention.
 */
public class Not {

    /**
     * This function creates a byte array for publishing from a ByteBuffer
     * (Open Tracing carrier) and a payload.
     *  
     * @param carrier The open tracing carrier that has been injected and unaltered, including position.
     * @param carrierLen Length of the carrier data (position after injected)
     * @param payload The message payload.
     * @return A byte array of the carrier and the payload.
     */
    public static byte[] createTracePayload(ByteBuffer carrier, byte[] payload) {
        if (carrier == null) {
            throw new IllegalArgumentException("Carrier cannot be null");
        }

        // Allocate a buffer, then copy carrier and payload into the
        // buffer to return.
        int len = carrier.position();
        byte[] buffer = new byte[len + payload.length];

        // We cannot modify the carrier, including the position, so
        // we'll copy the carrier bytes out.
        byte[] cbytes = new byte[len];
        carrier.rewind();
        carrier.get(cbytes);

        // copy the carrier and payload into our array and return
        System.arraycopy(cbytes, 0, buffer, 0, cbytes.length);
        System.arraycopy(payload, 0, buffer, cbytes.length, payload.length);
        return buffer;
    }

    public static TraceMessage createTraceMessage(Tracer tracer, io.nats.client.Message msg) {
        return new TraceMessage(tracer, msg);
    }

    public static io.opentracing.Tracer initTracing(String serviceName) {
            SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv().withType("const").withParam(1);
            ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true);
            Configuration config = new Configuration(serviceName).withSampler(samplerConfig).withReporter(reporterConfig);
            return config.getTracer();
    }
}
