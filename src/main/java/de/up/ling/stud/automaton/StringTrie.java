/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class StringTrie {
    private Trie actualTrie;
    private NGramTrie contextTrie;
    private int context;
    
    public StringTrie() {
        actualTrie = new Trie();
        contextTrie = new NGramTrie();
        this.context = 3;
    }
    
    public StringTrie(int context) {
        actualTrie = new Trie();
        contextTrie = new NGramTrie();
        this.context = context;
    }
    
    public void put(Reader reader) throws IOException {
        BufferedReader buffer = new BufferedReader(reader);
        String currentLine;
        while ((currentLine = buffer.readLine()) != null) {
            StringTokenizer tokenizer = new StringTokenizer(currentLine," .,-=;:<>/\\+()*&^%$#@!~`{}[]\n");
            String[] contextWindow = new String[context];
            for (int i = 0; i < contextWindow.length; i++) {
                contextWindow[i] = "";
            }
            while (tokenizer.hasMoreElements()) {
                String currentWord = tokenizer.nextToken();
                for (int i = 0; i < context - 1; i++) {
                    contextWindow[i] = contextWindow[i+1];
                }
                contextWindow[context-1] = currentWord;
                
                put(currentWord);
                putContext(contextWindow);
            }
        }
    }
    
    public void put(String key) {
        actualTrie.put(stringToIntArray(key));
    }
    
    public boolean contains(String needle) {
        return actualTrie.contains(stringToIntArray(needle));
    }
    
    private void putContext(String[] contextWindow) {
        int contextLength = 0;
        for (int i = 0; i < contextWindow.length; i++) {
            contextLength += contextWindow[i].length();
        }
        contextLength += context - 1;
        
        int[] key = new int[contextLength];
        int index = 0;
        
        for (int i = 0; i < contextWindow.length; i++) {
            String currentWord = contextWindow[i];
            int wordLength = currentWord.length();
            System.arraycopy(stringToIntArray(currentWord), 0, key, index, wordLength);
            index += wordLength + 1;
            if (index < contextLength) {
                key[index - 1] = 0;
            }
        }
        
        contextTrie.put(key, index, context);
    }
    
    public static int[] stringToIntArray(String word) {
        int[] ret = new int[word.length()];
        
        for (int i = 0; i < word.length(); i++) {
            ret[i] = word.charAt(i);
        }
        return ret;
    }
    
    public static String intArrayToString(int[] word) {
        StringBuilder buf = new StringBuilder();
        
        for (int i = 0; i < word.length; i++) {
            buf.append((char) word[i]);
        }
        
        return buf.toString();
    }
    
    private String intListToString(IntList word) {
        StringBuilder buf = new StringBuilder();
        
        for (int i = word.size()-1; i >= 0; i--) {
            buf.append((char) word.getInt(i));
        }
        
        return buf.toString();
    }
    
    public Trie getTrie() {
        return actualTrie;
    }
        
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Words in Trie:\n");
        
        for (IntArrayList concatenation : actualTrie.getAllConcatinations()) {
            
            buf.append(intListToString(concatenation) + "\n");
        }
        
        return buf.toString();
    }
    
    
}
