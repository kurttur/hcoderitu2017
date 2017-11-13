package com.hcoderteam.hcoder.Huffman;

class Node extends HuffmanTree {
    // left and right nodes
    public final HuffmanTree left, right;

    public Node(HuffmanTree l, HuffmanTree r) {
        super(l.freq + r.freq);
        left = l;
        right = r;
    }
}