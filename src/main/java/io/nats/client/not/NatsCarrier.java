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
import io.opentracing.propagation.Binary;

/**
  * This class is an internal opentracing carrier for tracing NATS messages.
  */
class NatsCarrier implements Binary {
   
    ByteBuffer buffer = null;

    /**
     * Creates a NatsCarrier for extraction
     * 
     * @param size - initial buffer size, used for extraction.
     */
    public NatsCarrier(byte[] rawCarrierData) {
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
    public NatsCarrier() {}
    
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