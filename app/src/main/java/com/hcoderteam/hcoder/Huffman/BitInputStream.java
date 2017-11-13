package com.hcoderteam.hcoder.Huffman;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class BitInputStream {

    // The underlying byte stream to read from (not null).
    private InputStream input;

    // Either in the range [0x00, 0xFF] if bits are available, or -1 if end of stream is reached.
    private int currentByte;

    // Number of remaining bits in the current byte, always between 0 and 7 (inclusive).
    private int numBitsRemaining;

    public BitInputStream(InputStream in) {
        input = in;
        currentByte = 0;
        numBitsRemaining = 0;
    }

    public int read() throws IOException {
        if (currentByte == -1)
            return -1;
        if (numBitsRemaining == 0) {
            currentByte = input.read();
            if (currentByte == -1)
                return -1;
            numBitsRemaining = 8;
        }
        if (numBitsRemaining <= 0)
            throw new AssertionError();
        numBitsRemaining--;
        return (currentByte >>> numBitsRemaining) & 1;
    }

    public int readNoEof() throws IOException {
        int result = read();
        if (result != -1)
            return result;
        else
            throw new EOFException();
    }

    public void close() throws IOException {
        input.close();
        currentByte = -1;
        numBitsRemaining = 0;
    }

}