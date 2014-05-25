package io.helium.server.protocols.mqtt.decoder;

import io.netty.buffer.ByteBuf;

import java.io.DataInput;
import java.io.IOException;

/**
 * Minimal type conversion, most methods will throw an exception.
 * thus it is kept private...
 */
public class BufferAsDataInput implements DataInput {

    private final ByteBuf stream;

    public BufferAsDataInput(ByteBuf stream) {
        this.stream = stream;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        stream.readBytes(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        stream.readBytes(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte() throws IOException {
        return stream.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return stream.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return stream.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return stream.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readInt() throws IOException {
        return stream.readInt();
    }

    @Override
    public long readLong() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public float readFloat() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException();
    }
}