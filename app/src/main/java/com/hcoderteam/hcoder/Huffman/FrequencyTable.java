package com.hcoderteam.hcoder.Huffman;

public class FrequencyTable {

    private int[] frequencies;

    public FrequencyTable() {
        frequencies = new int[256];
    }

    private void checkSymbol(int symbol) {
        if (symbol < 0 || symbol >= frequencies.length)
            throw new IllegalArgumentException("Symbol out of range");
    }

    public int get(int symbol) {
        checkSymbol(symbol);
        return frequencies[symbol];
    }

    public void set(int symbol, int freq) {
        checkSymbol(symbol);
        if (freq < 0)
            throw new IllegalArgumentException("Negative frequency");
        frequencies[symbol] = freq;
    }

    public void increment(int symbol) {
        checkSymbol(symbol);
        if (frequencies[symbol] == Integer.MAX_VALUE)
            throw new IllegalStateException("Maximum frequency reached");
        frequencies[symbol]++;
    }

    public int length() {
        return frequencies.length;
    }
}
