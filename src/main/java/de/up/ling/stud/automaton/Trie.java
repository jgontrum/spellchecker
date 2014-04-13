/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class Trie {
    private Int2ObjectMap<Trie> branches;
    private int id;
    private IntSet usedIDs;
    private boolean finalState;
    private final IDCounter idCounter;
    
    public Trie(int id, IDCounter idCounter, IntSet usedIDs) {
        this.branches = new Int2ObjectOpenHashMap<Trie>();
        this.id = id;
        this.usedIDs = usedIDs;
        this.finalState = false;
        this.idCounter = idCounter;
    }
    
    /**
     * Writes a given key into the Trie and relates it to a given value.
     * @param key
     * @param value
     */
    public int put(int[] key) {
        return put(key, 0);
    }
    
    public void putWithID(int[] key, int id) {
        putWithID(key, id, 0);
    }
    
    // Recursive call for put. 
    private int put(int[] key, int index) {
        if (index == key.length || key[index] == 0) {
            finalState = true;
            return id;
        } else {
            int currentKey = key[index];
            Trie nextTrie = branches.get(currentKey);
            
            // if there is no next trie, we have to create it
            if (nextTrie == null) {
                nextTrie = new Trie(idCounter.getNextID(), idCounter, usedIDs);
                branches.put(currentKey, nextTrie);
            }
            
            // go on recursivly, but move the index pointer to the next value in the key array
            return nextTrie.put(key, index+1);
        }
    }
    
    // Recursive call for put. 
    private void putWithID(int[] key, int id, int index) {
        if (index == key.length || key[index] == 0) {
            this.id = id;
            this.finalState = true;
        } else {
            int currentKey = key[index];
            Trie nextTrie = branches.get(currentKey);

            // if there is no next trie, we have to create it
            if (nextTrie == null) {
                nextTrie = new Trie(idCounter.getNextID(), idCounter, usedIDs);
                branches.put(currentKey, nextTrie);
            }

            // go on recursivly, but move the index pointer to the next value in the key array
            nextTrie.putWithID(key, id, index + 1);
        }
    }
    

    public boolean contains(int[] needle) {
        return contains(needle, 0);
    }
    
    private boolean contains(int[] needle, int index) {
        if (index == needle.length) {
            return isFinal();
        } else {
            int currentKey = needle[index];
            Trie nextTrie = branches.get(currentKey);

            if (nextTrie == null) {
                return false;
            }
            
            return nextTrie.contains(needle, index+1);
        }
    }
    
    public int getID(int[] needle) {
        return getID(needle, 0);
    }

    private int getID(int[] needle, int index) {
        if (index == needle.length) {
            return id;
        } else {
            int currentKey = needle[index];
            Trie nextTrie = branches.get(currentKey);

            if (nextTrie == null) {
                return -1;
            }

            return nextTrie.getID(needle, index + 1);
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
    
    public boolean isFinal() {
        return finalState;
    }

    public int getNextID() {
        return idCounter.getNextID();
    }

    public void saveWordsAndID(int[] currentWord, BufferedWriter bw) throws IOException {
        for (int a: branches.keySet()) {
            int[] ret = new int[currentWord.length+1];
            System.arraycopy(currentWord, 0, ret, 0, currentWord.length);
            ret[currentWord.length] = a;
            branches.get(a).saveWordsAndID(ret, bw);
        }
        if (isFinal()) {
            for (int i = 0; i < currentWord.length; ++i) {
                bw.write(currentWord[i]);
                if (i != currentWord.length-1) {
                    bw.write(",");
                }
            }
            bw.write(":" + id + "\n");
        }
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
        
        if (isFinal()) {
            ret.add(new IntArrayList());
        }
        
        return ret;
    }
    
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Concatenations in Trie:\n");
        for (IntArrayList concat : getAllConcatinations()) {
            buf.append(concat.toString()).append("\n");
        }
        return buf.toString();
    }
    
    
    ////////////////////////////////////////////////////////
    /// Functions for drawing the Trie in graphviz format.
    private String drawTransitions(IntList history, List<String> nodes) {
        StringBuilder ret = new StringBuilder();
        String currentState = "T(" + intListToString(history) + ")";
        for (int a : branches.keySet()) {
            if (branches.get(a) != null) {
                String label;
                if (a == 0) {
                    label = "|";
                } else {
                    label = String.valueOf((char) a);
                }
                IntList newHistory = new IntArrayList(history);
                newHistory.add(a);
                if (branches.get(a).isFinal()) {
                    nodes.add("node [shape = doublecircle, label=\"T(" + intListToString(newHistory) + ")\\n" + branches.get(a).printExtra()
                            + "\", fontsize=12] \"T(" + intListToString(newHistory) + ")\";");
                } else {
                    nodes.add("node [shape = circle, label=\"T(" + intListToString(newHistory) + ")\\n" + branches.get(a).printExtra()
                            + "\", fontsize=12] \"T(" + intListToString(newHistory) + ")\";");
                }
                ret.append("   \"" + currentState + "\" -> \"T(" + intListToString(newHistory) + ")\" [ label = \"" + label + "\" ];\n");
                ret.append(branches.get(a).drawTransitions(newHistory, nodes));
            }
        }

        return ret.toString();
    }

    public String draw() {
        StringBuilder ret = new StringBuilder();
        IntList history = new IntArrayList();
        List<String> nodes = new ArrayList<String>();
        if (isFinal()) {
            nodes.add("node [shape = doublecircle, label=\"T()\\n" + printExtra()
                    + "\", fontsize=12] \"T()\";");
        } else {
            nodes.add("node [shape = circle, label=\"T()\\n" + printExtra()
                    + "\", fontsize=12] \"T()\";");
        }
        String transitions = drawTransitions(history, nodes);

        ret.append("digraph finite_state_machine {\n"
                + "  rankdir=LR;\n"
                + "  size=\"8,5\";\n");

        for (String q : nodes) {
            ret.append("  " + q + "\n");
        }

        ret.append(transitions);

        ret.append("}");
        return ret.toString();
    }

    private String intListToString(IntList list) {
        StringBuilder ret = new StringBuilder();
        for (int s : list) {
            if (s != 0) {
                ret.append(String.valueOf((char) s));
            } else {
                ret.append("|");
            }
        }
        return ret.toString();
    }

    String printExtra() {
        StringBuilder ret = new StringBuilder();
        ret.append("id: " + id);
        return ret.toString();
    }

}
