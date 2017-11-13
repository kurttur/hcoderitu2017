package com.hcoderteam.hcoder.Huffman;

public abstract class HuffmanTree implements Comparable<HuffmanTree> {
    // frequency of tree
    public final int freq;

    public HuffmanTree(int f) {
        freq = f;
    }

    // compares on the frequency
    public int compareTo(HuffmanTree tree) {
        return freq - tree.freq;
    }
}