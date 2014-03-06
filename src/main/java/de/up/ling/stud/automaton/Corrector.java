/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import com.google.common.base.Function;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import com.google.common.collect.Iterables;
import java.util.Iterator;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class Corrector {
    private StringTrie lexicon;     
    private Comparator<weightedWord> compareWeigtedWords;
    
    
    public Corrector(StringTrie lexicon) {
        this.lexicon = lexicon;
        
        compareWeigtedWords = new Comparator<weightedWord>() {
            public int compare(weightedWord elem1, weightedWord elem2) {
                return (elem1.getWord().equals(elem2.getWord()))
                        ? 0 // elements are equal
                        : (elem1.getWeight() < elem2.getWeight()
                        ? 1 // elem1 is smaller 
                        : -1);		// elem1 is greater
            }
        };
        
    }
    
    public PriorityQueue<weightedWord> correctWord(String misspelledWord, int errorThreshold) {
        int t = errorThreshold;
        int[] misWord = StringTrie.stringToIntArray(misspelledWord);
        Stack<AgendaItem> agenda = new Stack<AgendaItem>();
        
        PriorityQueue<weightedWord> ret = new PriorityQueue<weightedWord>(20, compareWeigtedWords);
        
        agenda.add(new AgendaItem(new int[0], lexicon.getTrie()));
        while (!agenda.empty()) {
            AgendaItem currentItem = agenda.pop();
//            System.err.println("Current item:\n" + currentItem);
            int[] currentConcatenation = currentItem.getConcatenation();
            Trie currentTrie = currentItem.getTrie();
            int currentN = currentConcatenation.length;

            IntSet transitionSymbols = currentTrie.getAllTransitions();
            for (int transitionSymbol : transitionSymbols) {
//                System.err.println("TransitionSymbol: " + (char) transitionSymbol);
                int[] newCandidate = new int[currentN + 1];
                System.arraycopy(   currentConcatenation, 
                                    0,
                                    newCandidate, 
                                    0, 
                                    currentN);
                newCandidate[currentN] = transitionSymbol;
                if (CutOffEditDistance.calcCutOffDistance(misWord, newCandidate, errorThreshold) <= errorThreshold) {
                    agenda.push(new AgendaItem(newCandidate, currentTrie.getSubtrieByTransitionSymbol(transitionSymbol)));
                }
                
            }
            if (EditDistance.calcDistance(misWord, currentConcatenation) <= t &&
                        currentTrie.isFinal()) {
                    System.err.println("Correcting " + misspelledWord + " to " + 
                            StringTrie.intArrayToString(currentConcatenation) +
                            " with an distance of " + 
                            EditDistance.calcDistance(misWord, currentConcatenation) +
                            " and a count of " + 
                            currentTrie.getWordCount());
                    
                    ret.add(new weightedWord(StringTrie.intArrayToString(currentConcatenation), currentTrie.getWordCount()));
            }
        }
        
        return ret;
    }
    
    public Iterable<String> correctWordDynamicly(String misspelledWord) {
        PriorityQueue<weightedWord> ret = new PriorityQueue<weightedWord>(50, compareWeigtedWords);
        for (int threshold = 1; ret.isEmpty(); ++threshold) {
            ret.addAll(correctWord(misspelledWord, threshold));
        }
        return Iterables.transform(ret, 
                new Function<weightedWord, String>() {
                    public String apply(weightedWord word) {
                        return word.getWord();
                    }
                });
    }
    
    private class AgendaItem {
        private final int[] concatenation;
        private final Trie subTrie;

        public AgendaItem(int[] concatenation, Trie subTrie) {
            this.concatenation = concatenation;
            this.subTrie = subTrie;
        }
        
        public int[] getConcatenation() {
            return concatenation;
        }
        
        public Trie getTrie() {
            return subTrie;
        }

        @Override
        public String toString() {
            return "AgendaItem{" + "concatenation=" + Arrays.toString(concatenation) + ", subTrie=" + subTrie.hashCode() + '}';
        }
    }
    
    private class weightedWord {
        private final String word;
        private final int weight;

        public weightedWord(String word, int weight) {
            this.word = word;
            this.weight = weight;
        }

        public String getWord() {
            return word;
        }

        public int getWeight() {
            return weight;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.word != null ? this.word.hashCode() : 0);
            hash = 29 * hash + this.weight;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final weightedWord other = (weightedWord) obj;
            if ((this.word == null) ? (other.word != null) : !this.word.equals(other.word)) {
                return false;
            }
            if (this.weight != other.weight) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "weightedWord{" + "word=" + word + ", weight=" + weight + '}';
        }

    }
}

