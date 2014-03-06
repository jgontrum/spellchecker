/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class Trie {
    protected Int2ObjectMap<Trie> branches;
    protected int wordCount;
    
    public Trie() {
        branches = new Int2ObjectOpenHashMap<Trie>();
        wordCount = 0;
    }
    
    /**
     * Writes a given key into the Trie and relates it to a given value.
     * @param key
     * @param value
     */
    public void put(int[] key) {
        put(key, 0);
    }
    
    // Recursive call for put. 
    protected void put(int[] key,  int index) {
        if (index == key.length) {
            ++wordCount;
        } else {
            int currentKey = key[index];
            Trie nextTrie = branches.get(currentKey);
            
            // if there is no next trie, we have to create it
            if (nextTrie == null) {
                nextTrie = new Trie();
                branches.put(currentKey, nextTrie);
            }
            
            // go on recursivly, but move the index pointer to the next value in the key array
            nextTrie.put(key, index+1);
        }
    }
    

    public boolean contains(int[] needle) {
        return contains(needle, 0);
    }
    
    protected boolean contains(int[] needle, int index) {
        if (index == needle.length) {
            return wordCount > 0;
        } else {
            int currentKey = needle[index];
            Trie nextTrie = branches.get(currentKey);

            if (nextTrie == null) {
                return false;
            }
            
            return nextTrie.contains(needle, index+1);
        }
    }
    
    /**
     * Returns a Subtrie of this Trie, that starts at the transition
     * with a given symbol.
     * @param symbol
     * @return
     */
    public Trie getSubtrieByTransitionSymbol(int symbol) {
        return branches.get(symbol);
    }
    
    public IntSet getAllTransitions() {
        return branches.keySet();
    }
    
    public int numOfTransitions() {
        assert branches.keySet().size() == getAllTransitions().size();
        return branches.keySet().size();
    }
    
    public Set<IntArrayList> getAllConcatinations() {
        Set<IntArrayList> ret = new HashSet<IntArrayList>();
        
//        System.err.println("Branches:" + branches.keySet());
        
        for (int a : branches.keySet()) {
//            System.err.println("a " + (char) a);
            Set<IntArrayList> concatenations = branches.get(a).getAllConcatinations();
            if (concatenations.size() > 0) {
                for (IntArrayList subConcatanation : concatenations) {
                    subConcatanation.add(a);
                    ret.add(subConcatanation);
                }
            } else {
                IntArrayList insert = new IntArrayList();
                insert.add(a);
                ret.add(insert);
            }
        }
        
        if (wordCount > 0) {
            ret.add(new IntArrayList());
        }
        
        return ret;
    }
    
    
    public boolean isFinal() {
        return wordCount > 0;
    }
    
    public int getWordCount() {
        return wordCount;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Concatenations in Trie:\n");
        for (IntArrayList concat : getAllConcatinations()) {
            buf.append(concat.toString()).append("\n");
        }
        return buf.toString();
    }
    
    
    
}
