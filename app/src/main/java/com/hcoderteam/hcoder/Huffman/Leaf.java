package com.hcoderteam.hcoder.Huffman;

class Leaf extends HuffmanTree {
    // leaf's character
    public final char value;

    public Leaf(int freq, char val) {
        super(freq);
        value = val;
    }
}