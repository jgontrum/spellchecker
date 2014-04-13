/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class BackOffModelTrie {
    private int count; // Like probability, but show the real count of it (for further calculation of the probability).
    private Int2ObjectMap<BackOffModelTrie> branches; // The outgoing transitions from this trie. Each symbol (int) maps to another trie.
    private double probability; // The conditioned probability for the context that lead to this trie.
    private final boolean finalState; 
    private final int nGram; // Number of n-grams represented by this _sub_trie
    private final int allNGram; // Number of n-grams, that the complete trie represents. Invariant: In the top trie nGram == allNGram is true.
    private final double backOffFactor;
    private final double backOffFactorLog;
    private boolean locked; // After MLE the trie will be locked, so it will not be changed anymore.
    
    public BackOffModelTrie(int currentNGram, int overallNGram) {
        branches = new Int2ObjectOpenHashMap<BackOffModelTrie>();   
        probability = Double.POSITIVE_INFINITY; // Default value, if the probability is not calculated yet.
        count = 0;  
        nGram = currentNGram;
        allNGram = overallNGram;
        locked = false;
        finalState = nGram != allNGram; // Make sure that the starting state is not final (assertion: every context that is looked up here _must_ pass the first state)
        if (nGram == 0) { // best case
            backOffFactor = 1;
        } else { // the factor gets smaller (worse), the bigger the current nGram value is. The closer a subtrie is to the top, the worse is its factor.
            backOffFactor = 0.5 * (1.0 / nGram);
        }
        backOffFactorLog = Math.log(backOffFactor);
    }
    
    /**
     * Writes a word, that represents a given number of nGrams into the trie.
     * @param key
     * @param nGrams
     */
    public void put(int[] key, int nGrams) {
        if (!locked) {
            put(key, nGrams, 0, 0);
        }
    }
    
    /**
     * Like put(int[], int), but for restoring already counted nGrams.
     * Note: Use this function, when restoring the trie from a file.
     * @param key
     * @param nGrams
     * @param count
     */
    public void putWithCount(int[] key, int nGrams, int count) {
        if (!locked) {
            put(key, nGrams, count, 0);
        }
    }
    
    /**
     * Calculates the Maximum Likelihood Estimation of all nGrams in the trie.
     * This method must be called, after all nGrams are written to the trie
     * and before it is used by any other class. 
     * Once this method is called, content changes are impossible.
     */
    public void calculateMLE() {
        // Perform a dfs in the trie to calculate the MLE for the entries with the biggest n-gram first
        locked = true; // lock this trie
        for (int a : branches.keySet()) {
            // Start the recursion. This function is normaly only called in the root note, representing a starting state
            branches.get(a).calculateMLE(count);
        }
    }
    
    /**
     * Calculates the Maximum Likelihood Estimation of all nGrams in the trie and 
     * saves the logarithmic value of it.
     * This method is like calculateMLE(), but it saves the log value, because in a
     * huge corpus, the normal probabilities could become so small, they would be 0.
     */
    public void calculateMLElog() {
        // Perform a dfs in the trie to calculate the MLE for the entries with the biggest n-gram first
        locked = true; // lock this trie
        for (int a : branches.keySet()) {
            // Start the recursion. This function is normaly only called in the root note, representing a starting state
            branches.get(a).calculateMLElog(count);
        }
    }
    
    /**
     * Returns a probability value for a given word/context.
     * The returned value may no be exactly the cond. prob. for it, because
     * it multiplied with a back-off-factor, resulting in better values the more
     * of the context is found in the trie.
     * @param needle
     * @return
     */
    public double getProbability(int[] needle) {
        return getProbability(needle, 0);
    }
    
    
    public boolean isFinal() {
        return finalState;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Recursive functions
    ////////////////////////////////////////////////////////////////////////////
    
    private void put(int[] key, int nGrams, int count, int index) {
        // increase the counter only, if there is not a given value for count.
        if (this.finalState) {
            if (count == 0) {
                ++this.count;
            } else {
                if (index == key.length) {
                    this.count = count;
                }
            }
        } else {
            if (key.length == this.allNGram) { // do not update the counter, if a word is restored from file
                ++this.count;
            }
        }
        
        nGrams -= 1;
      
        if (index < key.length) {
            int currentKey = key[index];
            BackOffModelTrie nextTrie = branches.get(currentKey);
            // if there is no next trie, we have to create it
            if (nextTrie == null) {
                nextTrie = new BackOffModelTrie(nGrams, allNGram);
                branches.put(currentKey, nextTrie);
            }
            // go on recursivly, but move the index pointer to the next value in the key array
            nextTrie.put(key, nGrams, count, index + 1);
        }
        
    }
    
    
    private void calculateMLE(double lastValidCount) {
        // lastValidCount represents the number of bare counts for the last complete n-gram.
        // If this state is a final one, calculate the conditioned probability for the current context given the shorter context with the count 'lastValidCount'
        this.probability = (count / lastValidCount) * backOffFactor; // Conditional Probability!
        for (int a : branches.keySet()) {
            branches.get(a).calculateMLE(count);
        }

    }
    
    
    private void calculateMLElog(double lastValidCounLog) {
        // lastValidCount represents the number of bare counts for the last complete n-gram.
        // If this state is a final one, calculate the conditioned probability for the current context given the shorter context with the count 'lastValidCount'
        
        // same calculation as in calculateMLE, but for logarithms:
        double countLog = Math.log(count);
        this.probability = countLog - lastValidCounLog + backOffFactorLog;
        for (int a : branches.keySet()) {
            branches.get(a).calculateMLElog(countLog);
        }

    }
    
    private double getProbability(int[] needle, int index) {
        if (index == needle.length) {
            return probability;
        } else {
            int currentKey = needle[index];
            BackOffModelTrie nextTrie = branches.get(currentKey);

            if (nextTrie == null) {
                return probability; // return the back off probablity
            }

            return nextTrie.getProbability(needle, index + 1);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Functions for saving the trie or drawing it in graphviz format
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Writes this (sub)trie into a given Buffer.
     * Note: The function must be called within this package, because it should
     * be called from a bigger output function
     * @param currentWord
     * @param bw
     * @throws IOException
     */
    void saveToFile(int[] currentWord, BufferedWriter bw) throws IOException {
        // Call this function recursivly and write the words to file, when
        // a final state is reached
        for (int a : branches.keySet()) {
            int[] ret = new int[currentWord.length + 1];
            System.arraycopy(currentWord, 0, ret, 0, currentWord.length);
            ret[currentWord.length] = a;
            branches.get(a).saveToFile(ret, bw);
        }
        if (isFinal()) {
            for (int i = 0; i < currentWord.length; ++i) {
                bw.write(String.format("%d", currentWord[i])); // convert the number to string
                bw.write((i != currentWord.length -1 ? "," : "")); //place a comma if needed
            }
            bw.write(":" + count + "\n");
        }
    }
    
    /**
     * Creates a String in dot-format for this trie.
     * Note: This function should only be called in the root node of the trie.
     * @param translator
     * @return
     */
    String draw(StringTrie translator) {
        StringBuilder ret = new StringBuilder();
        IntList history = new IntArrayList();
        List<String> nodes = new ArrayList<String>();
        
        nodes.add("node [shape = " + (isFinal() ? "doublecircle" : "circle") + ", label=\"T()\\n" + printExtra()
                + "\", fontsize=12] \"T()\";"); // add node for this trie

        // Collect all transitions and nodes
        String transitions = drawTransitions(history, "T()", nodes, translator);

        ret.append("digraph finite_state_machine {\n"
                + "  rankdir=LR;\n"
                + "  size=\"8,5\";\n");

        // write nodes
        for (String q : nodes) {
            ret.append("  " + q + "\n");
        }

        // and transitions
        ret.append(transitions);

        ret.append("}");
        return ret.toString();
    }

    private String drawTransitions(IntList history, String historyFormated, List<String> nodes, StringTrie translator) {
        StringBuilder ret = new StringBuilder();
        String currentState = historyFormated;
        for (int a : branches.keySet()) {
            if (branches.get(a) != null) {
                String label = String.format("%d", a);
                IntList newHistory = new IntArrayList(history);
                newHistory.add(a);
                String nextState = "T(" + translator.idsToWordsReadable(newHistory) + ")";
                nodes.add("node [shape = "  + (branches.get(a).isFinal() ? "doublecircle" : "circle") +  ", label=\"" 
                        + nextState +"\\n" + branches.get(a).printExtra()
                        + "\", fontsize=12] \""+ nextState +"\";"); 
                ret.append("   \"" + currentState + "\" -> \""+ nextState +"\" [ label = \"" + label + "\" ];\n");
                ret.append(branches.get(a).drawTransitions(newHistory, nextState, nodes, translator));
            }
        }
        return ret.toString();
    }
    
    private String printExtra() {
        StringBuilder ret = new StringBuilder();
        ret.append("Counts: " + count);
        ret.append("\\n");
        ret.append("Prob: " + probability);
        ret.append("\\n");
        ret.append("nGram: " + nGram + "/" + allNGram);
        ret.append("\\n");
        ret.append("Factor: " + backOffFactor);
        return ret.toString();
    }
}
