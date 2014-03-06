/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class NGramTrie {
    private Int2DoubleMap predecessorToProbability;
    private Int2ObjectMap<NGramTrie> branches;
 

    public NGramTrie() {
        predecessorToProbability = new Int2DoubleOpenHashMap();
        predecessorToProbability.defaultReturnValue(0.0);
    }

    public void put(int[] key, int predecessorHash, double probability) {
        put(key, predecessorHash, probability, 0);
    }
    
    private void put(int[] key, int predecessorHash, double probability, int index) {
        if (index == key.length) {
            predecessorToProbability.put(predecessorHash, probability);
        } else {
            int currentKey = key[index];
            NGramTrie nextTrie = branches.get(currentKey);

            // if there is no next trie, we have to create it
            if (nextTrie == null) {
                nextTrie = new NGramTrie();
                branches.put(currentKey, nextTrie);
            }

            // go on recursivly, but move the index pointer to the next value in the key array
            nextTrie.put(key, predecessorHash, probability, index + 1);
        }
    }
    
    public double getProbability(int[] needle, int predecessorHash) {
        return getProbability(needle, predecessorHash, 0);
    }

    private double getProbability(int[] needle, int predecessorHash, int index) {
        if (index == needle.length) {
            return predecessorToProbability.get(predecessorHash);
        } else {
            int currentKey = needle[index];
            NGramTrie nextTrie = branches.get(currentKey);

            if (nextTrie == null) {
                return 0.0;
            }

            return nextTrie.getProbability(needle, predecessorHash, index + 1);
        }
    }    
}
