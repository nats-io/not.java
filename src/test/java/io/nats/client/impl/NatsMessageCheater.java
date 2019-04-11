package io.nats.client.impl;

public class NatsMessageCheater {
    public static io.nats.client.Message createMessage(String subject, String reply, byte[] payload) {
        NatsMessage nm = new io.nats.client.impl.NatsMessage("sid1", subject, reply, payload.length);
        nm.setData(payload);
        return nm;
    }
}