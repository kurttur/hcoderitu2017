package com.hcoderteam.hcoder.Huffman;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class Huffman {

    public static FrequencyTable getFrequencyTable(String input) throws IOException{
        FrequencyTable frequencyTable = new FrequencyTable();
        int length = input.length();

        for (int i=0; i<length; i++) {
            frequencyTable.increment((int)input.charAt(i));
        }
        return frequencyTable;
    }

    public static HuffmanTree buildTree(FrequencyTable frequencyTable) {

        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();

        for (int i = 0; i < frequencyTable.length(); i++)
            if (frequencyTable.get(i) > 0)
                trees.offer(new Leaf(frequencyTable.get(i), (char)i));

        while (trees.size() > 1) {
            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();

            trees.offer(new Node(a, b));
        }
        return trees.poll();
    }

    public static HashMap<Character, String> getCodes(HuffmanTree tree) {

        HashMap<Character, String> codes = new HashMap<>();
        DFS(tree, "", codes);
        return codes;
    }

    private static void DFS(HuffmanTree tree, String code,
                            HashMap<Character, String> codes) {

        if (tree instanceof Leaf) {
            Leaf leaf = (Leaf)tree;

            codes.put(leaf.value, code);
        } else if (tree instanceof Node) {
            Node node = (Node)tree;

            // traverse left
            DFS(node.left, code+"0", codes);

            // traverse right
            DFS(node.right, code+"1", codes);
        }
    }

    public static void printCodes(HuffmanTree tree, StringBuffer prefix) {

        if (tree instanceof Leaf) {
            Leaf leaf = (Leaf)tree;

            // print out character, frequency, and code for this leaf (which is just the prefix)
            Log.d("LOGG", leaf.value + "\t" + leaf.freq + "\t" + prefix);

        } else if (tree instanceof Node) {
            Node node = (Node)tree;
            //Log.d("LOGG", "--Node: "+node.freq+"  --"+prefix);

            // traverse left
            prefix.append('0');
            printCodes(node.left, prefix);
            prefix.deleteCharAt(prefix.length()-1);

            // traverse right
            prefix.append('1');
            printCodes(node.right, prefix);
            prefix.deleteCharAt(prefix.length()-1);
        }
    }

    public static String encode(HuffmanTree tree, String input) throws IOException{

        HashMap<Character, String> codes = getCodes(tree);

        StringBuilder stringBuilder = new StringBuilder();

        int length = input.length();

        for (int i=0; i<length; i++)
            stringBuilder.append(codes.get(input.charAt(i)));

        return stringBuilder.toString();
    }

    public static String add_padding(String encoded) {
        int extra_padding = 8 - encoded.length() % 8;

        String padding = "";
        for (int i=0; i<extra_padding; i++)
            padding += "0";

        encoded = encoded + padding;

        String padded_info = String.format("%8s", Integer.toBinaryString(extra_padding)).replace(' ', '0');

        return padded_info + encoded;
    }

    public static String remove_padding(String padded_encoded) {
        String padded_info = padded_encoded.substring(0, 8);
        int extra_padding = Integer.parseInt(padded_info, 2);

        return padded_encoded.substring(8, padded_encoded.length()-extra_padding);
    }

    public static String decode(HuffmanTree tree, String encoded) {

        StringReader stringReader = new StringReader(encoded);
        StringBuilder sb = new StringBuilder();

        int character;
        while ((character = decodeCharacters(tree, stringReader)) > 0) {
            sb.append((char)character);
        }

        return sb.toString();
    }

    private static int decodeCharacters(HuffmanTree tree, StringReader stringReader) {
        if (tree instanceof Leaf) {
            Leaf leaf = (Leaf)tree;

            return leaf.value;
        } else if (tree instanceof Node){
            Node node = (Node)tree;

            int temp = stringReader.next();
            if (temp == '0') {
                return decodeCharacters(node.left, stringReader);
            } else if (temp == '1') {
                return decodeCharacters(node.right, stringReader);
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private static class StringReader {
        String string;
        int index = -1;

        StringReader(String string) {
            this.string = string;
        }

        int next() {
            index++;
            if (index < string.length())
                return (int) string.charAt(index);
            else
                return -1;
        }
    }
}