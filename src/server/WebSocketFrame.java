package server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * RFC 6455 WebSocket Framing & Handshake utility.
 * Implements standard WebSocket framing in pure standard Java without any external dependencies.
 */
public class WebSocketFrame {
    public static final String WEBSOCKET_MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static final int OPCODE_CONTINUATION = 0x0;
    public static final int OPCODE_TEXT = 0x1;
    public static final int OPCODE_BINARY = 0x2;
    public static final int OPCODE_CLOSE = 0x8;
    public static final int OPCODE_PING = 0x9;
    public static final int OPCODE_PONG = 0xA;

    public static class Frame {
        public final int opcode;
        public final boolean fin;
        public final byte[] payload;

        public Frame(int opcode, boolean fin, byte[] payload) {
            this.opcode = opcode;
            this.fin = fin;
            this.payload = payload;
        }

        public String getAsText() {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }

    /**
     * Computes the Sec-WebSocket-Accept response token according to RFC 6455.
     */
    public static String computeAcceptKey(String clientKey) {
        if (clientKey == null) return null;
        try {
            String combined = clientKey.trim() + WEBSOCKET_MAGIC_GUID;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Reads and decodes a single RFC 6455 frame from the input stream.
     */
    public static Frame readFrame(InputStream inputStream) throws IOException {
        DataInputStream dis = (inputStream instanceof DataInputStream) ?
                (DataInputStream) inputStream : new DataInputStream(inputStream);

        int b0 = dis.readUnsignedByte();
        boolean fin = (b0 & 0x80) != 0;
        int opcode = b0 & 0x0F;

        int b1 = dis.readUnsignedByte();
        boolean isMasked = (b1 & 0x80) != 0;
        int lengthIndicator = b1 & 0x7F;

        long payloadLength;
        if (lengthIndicator == 126) {
            payloadLength = dis.readUnsignedShort();
        } else if (lengthIndicator == 127) {
            payloadLength = dis.readLong();
        } else {
            payloadLength = lengthIndicator;
        }

        if (payloadLength < 0 || payloadLength > 10 * 1024 * 1024) { // 10MB safety cap
            throw new IOException("Frame payload length exceeds maximum allowed size: " + payloadLength);
        }

        byte[] mask = new byte[4];
        if (isMasked) {
            dis.readFully(mask);
        }

        byte[] payload = new byte[(int) payloadLength];
        dis.readFully(payload);

        // Client frames MUST be masked per RFC 6455 Section 5.3
        if (isMasked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
        }

        return new Frame(opcode, fin, payload);
    }

    /**
     * Encodes a server-to-client text frame (unmasked per RFC 6455).
     */
    public static byte[] encodeTextFrame(String text) {
        byte[] payload = (text != null) ? text.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return encodeFrame(OPCODE_TEXT, payload);
    }

    /**
     * Encodes a Pong frame echoing ping payload.
     */
    public static byte[] encodePongFrame(byte[] pingPayload) {
        return encodeFrame(OPCODE_PONG, pingPayload != null ? pingPayload : new byte[0]);
    }

    /**
     * Encodes a standard Close frame (status code 1000 - normal closure).
     */
    public static byte[] encodeCloseFrame() {
        byte[] payload = new byte[]{ (byte) 0x03, (byte) 0xE8 }; // 1000
        return encodeFrame(OPCODE_CLOSE, payload);
    }

    private static byte[] encodeFrame(int opcode, byte[] payload) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b0 = 0x80 | (opcode & 0x0F); // FIN=1 + opcode
        baos.write(b0);

        int len = payload.length;
        if (len < 126) {
            baos.write(len);
        } else if (len <= 65535) {
            baos.write(126);
            baos.write((len >> 8) & 0xFF);
            baos.write(len & 0xFF);
        } else {
            baos.write(127);
            for (int i = 7; i >= 0; i--) {
                baos.write((int) ((len >> (8 * i)) & 0xFF));
            }
        }

        baos.write(payload, 0, payload.length);
        return baos.toByteArray();
    }
}
