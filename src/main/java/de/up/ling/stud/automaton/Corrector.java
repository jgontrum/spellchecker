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
import com.sun.crypto.provider.AESWrapCipher;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class Corrector {
    private StringTrie lexicon;
    private final Comparator<WeightedWord> compareWeigtedWords;
    private final int maxThreshold;
    private final double inLexiconFactor;
    private final EditDistance editDistance;
    private final CutOffEditDistance cutOffEditDistance;
    
    public Corrector(StringTrie lexicon) {
        this.lexicon = lexicon;
        this.maxThreshold = 2;
        this.inLexiconFactor = 100;
        this.editDistance = new EditDistance();
        this.cutOffEditDistance = new CutOffEditDistance(editDistance);
        
        // define comparator for the queue
        compareWeigtedWords = new Comparator<WeightedWord>() {
            public int compare(WeightedWord elem1, WeightedWord elem2) {
                return (elem1.getWord().equals(elem2.getWord()))
                        ? 0 // elements are equal
                        : (elem1.getWeight() < elem2.getWeight()
                        ? 1 // elem1 is smaller 
                        : -1);		// elem1 is greater
            }
        };
    }
    
    /**
     * 
     * @param context = [PrevWord1, PrevWord2, MisspelledWord[
     * @return
     */
    public Iterable<String> correctWordInContext(String[] context) {
        assert lexicon != null;
        int nGram = context.length;
        int[] wordIDs = new int[nGram];
        int[] misspelledWord = StringTrie.stringToIntArray(context[nGram-1]);
        int localMaxThreshold = maxThreshold; // The maximum of editdistances that we consider.
        // create the context for the language model:
        // Convert every context word to an int array and get the wordID of it.
        // As soon as a context word is not found, stop the process.
        // In the new array, the context is reversed so that the misspelled word is
        // at position 0. This is neccecary for an efficient lookup in the language model.
        for (int i = 1; i < nGram; i++) {
            int[] wordAsIntArray = StringTrie.stringToIntArray(context[nGram - i - 1]);
            wordIDs[i] = lexicon.getWordID(wordAsIntArray);
        }
        wordIDs[0] = -1; // Set a dummy value for the cell, where the candidates will be placed.
                
        // Check first, if the word is in the lexicon (must not be the correct one though)
        boolean inLexicon = lexicon.contains(misspelledWord);
        System.err.println("in? " + inLexicon);
        // If the current word is in the lexicon, we calculate only the distances from 1 to 2,
        // to check if the word is maybe very unplausible in the given context.
        if (inLexicon) {
            localMaxThreshold = 1;
        }
        
        // find all candidates
        PriorityQueue<WeightedWord> candidates = new PriorityQueue<WeightedWord>(15, compareWeigtedWords);
        candidates.addAll(correctWord(misspelledWord, wordIDs, localMaxThreshold));
        
        
        return Iterables.transform(candidates,
                new Function<WeightedWord, String>() {
            public String apply(WeightedWord word) {
//                System.err.println(word.getWord() + " \t " + word.getWeight());
                return word.getWord();
            }
        });
    }
    
    /**
     *
     * @param misspelledWord
     * @return
     */
    public Iterable<String> correctWord(String misspelledWord) {
        String[] tempArray = new String[1];
        tempArray[0] = misspelledWord;
        return correctWordInContext(tempArray);
    }
    
    public void correctText(String filename) throws FileNotFoundException, IOException {
        BufferedReader buffer = new BufferedReader(new FileReader(new File(filename)));
        String currentLine;
        String currentWord;

        Corrector cor = new Corrector(lexicon);


        String[] window = new String[lexicon.getNGram()];
        for (int i = 0; i < lexicon.getNGram(); i++) {
            window[i] = "";
        }

        while ((currentLine = buffer.readLine()) != null) {
            StringTokenizer tokenizer = new StringTokenizer(currentLine, " .,\"'-=;:<>/\\+()*!?&^%$#@!~`{}[]\n«»");
            while (tokenizer.hasMoreElements()) {
                currentWord = tokenizer.nextToken();
                for (int i = 0; i < lexicon.getNGram() - 1; i++) {
                    window[i] = window[i + 1]; // move the window to the left
                }

                window[lexicon.getNGram() - 1] = currentWord;
                System.err.println("Current Word: " + currentWord);
                Iterator<String> candidates = cor.correctWordInContext(window).iterator();

                for (int i = 0; i < 5; i++) {
                    if (candidates.hasNext()) {
                        System.err.println("What about      " + candidates.next());
                    }
                }

            }
        }
    }
    
    
    // This method generates a priority queue for candidates the given word can
    //  be corrected to within a given error threshold.
    private PriorityQueue<WeightedWord> correctWord(int[] misspelledWord, int[] context, int errorThreshold) {        
        // This is nearly a direct implementation of the algorithm of Oflazar.
        // It is agenda-driven (it hold unfinished concatenations of symbols 
        // and a reference to the subtrie - a subtrie of 'lexicon'. 
        // This reference is equivalent to the states that Oflazar uses).
        Stack<AgendaItem> agenda = new Stack<AgendaItem>();
        // All possible candidates that the misspelled Word can be corrected to
        // will be stored in this sorted queue, so that the word with the highest
        // probability is on the top.
        PriorityQueue<WeightedWord> candidates = new PriorityQueue<WeightedWord>(20, compareWeigtedWords);

        // Initializing variables:
        Trie currentTrie;
        int currentLength;
        IntIterator symbolIt;
        int coDistance;
        int edDistance;
        
        // Add a starting item: An empyy word and the whole trie (=> starting state)
        agenda.add(new AgendaItem(new int[0], lexicon.getLexicon()));
        while (!agenda.empty()) {
            AgendaItem currentItem = agenda.pop();
            int[] currentConcatenation = currentItem.getConcatenation();// current word
            currentTrie = currentItem.getTrie();                   // current 'state'
            currentLength = currentConcatenation.length;

            // Iterate over all outgoing transitions
            symbolIt = currentTrie.getAllTransitions().iterator();
            while (symbolIt.hasNext()) { 
                int transitionSymbol = symbolIt.next();
//                System.err.println("TransitionSymbol: " + (char) transitionSymbol);
                // Creat the array for the new candidate. This candidate is like the 
                // current one, but with another symbol appended.
                int[] newCandidate = new int[currentLength + 1];
                // Copy the old concatenation to the new one and add the current symbol.
                System.arraycopy(currentConcatenation, 0, newCandidate, 0, currentLength);
                newCandidate[currentLength] = transitionSymbol;
                
                // Now calculate the cutoff-edit distance
                coDistance = cutOffEditDistance.calcCutOffDistance(misspelledWord, newCandidate, errorThreshold);
                if (coDistance <= errorThreshold) {
                    // If it is below the threshold, add it to the agenda.
                    agenda.push(new AgendaItem(newCandidate, currentTrie.getSubtrieByTransitionSymbol(transitionSymbol)));
                }
                
            }
            // Also, if the state of the current candidate is final and the edit distance is ok, it is a valid cadidate.
            edDistance = editDistance.calcDistance(misspelledWord, currentConcatenation);
            if (edDistance <= errorThreshold && currentTrie.isFinal()) {
                // Retrive the wordid of the candidate from the lexicon and add it to the first cell of the context array.
                context[0] = lexicon.getWordID(currentConcatenation);
                double p;
                                
                if (edDistance == 0) { // correct word should have a huge advantage!
//                    System.err.println("W already exists with p=" + lexicon.getBackOffProbability(context));
                    p = lexicon.getBackOffProbability(context) + 1000;
//                    System.err.println("Calculated: " + p);
                } else {
                    // Now we lookup the back-off-probability in the language model. 
                    p = lexicon.getBackOffProbability(context) + Math.log(Math.pow((1.0) / edDistance + 1, 20));
                }
                
                
                candidates.add(new WeightedWord(StringTrie.intArrayToString(currentConcatenation), p));
            }
        }
        
        return candidates;
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
    
    private class WeightedWord {
        private final String word;
        private final double weight;

        public WeightedWord(String word, double weight) {
            this.word = word;
            this.weight = weight;
        }

        public String getWord() {
            return word;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + Objects.hashCode(this.word);
            hash = 41 * hash + (int) (Double.doubleToLongBits(this.weight) ^ (Double.doubleToLongBits(this.weight) >>> 32));
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
            final WeightedWord other = (WeightedWord) obj;
            if (!Objects.equals(this.word, other.word)) {
                return false;
            }
            if (Double.doubleToLongBits(this.weight) != Double.doubleToLongBits(other.weight)) {
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

