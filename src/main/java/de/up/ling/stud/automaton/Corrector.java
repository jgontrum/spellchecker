package de.up.ling.stud.automaton;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import de.saar.basic.Pair;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Analyzes a given misspelled word that can be in a context and delivers a
 * sorted iterator over possible candidates.
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class Corrector {

    private StringTrie data;
    private final EditDistance editDistance;
    private final CutOffEditDistance cutOffEditDistance;
    private final Comparator<WeightedWord> compareWeigtedWords;
    /**
     * The maximum error threshold for candidates.
     */
    private final int maxThreshold;
    /**
     * The minimal number of candidates that should be found.
     */
    private final int minCandidates;

    /**
     * Creates a new corrector based on a StringTrie, that has to contain some
     * data.
     *
     * @param data
     */
    public Corrector(StringTrie data) {
        this.data = data;
        this.maxThreshold = 5;
        this.minCandidates = 5;
        this.editDistance = new EditDistance();
        this.cutOffEditDistance = new CutOffEditDistance(editDistance);

        // define comparator for the queue
        compareWeigtedWords = new Comparator<WeightedWord>() {
            public int compare(WeightedWord elem1, WeightedWord elem2) {
                return (elem1.getWordID() == elem2.getWordID())
                        ? 0 // elements are equal
                        : (elem1.getWeight() > elem2.getWeight()
                        ? 1 // elem1 is greater
                        : -1);		// elem1 is smaller
            }
        };
    }

    /*

     *
     * @param context =
     * @return
     */
    /**
     * Corrects the last word in a given context and returns a sorted iterator
     * over tupels of possible candidates and their weight. Latter is returned
     * only for debug reasons.
     *
     * @param context [PrevWord1, PrevWord2, MisspelledWord[
     * @return Iterable over word-weight tuples
     */
    public Iterable<Pair<String, Double>> correctWordInContext(String[] context) {
        assert data != null;
        int nGram = context.length;
        int[] wordIDs = new int[nGram];
        int[] misspelledWord = StringTrie.stringToIntArray(context[nGram - 1]);
        int localMaxThreshold = maxThreshold; // The maximum of editdistances that we consider.

        // create the context for the language model:
        // Convert every context word to an int array and get the wordID of it.
        // As soon as a context word is not found, stop the process.
        // In the new array, the context is reversed so that the misspelled word is
        // at position 0. This is neccecary for an efficient lookup in the language model.
        for (int i = 1; i < nGram; i++) {
            int[] wordAsIntArray = StringTrie.stringToIntArray(context[nGram - i - 1]);
            wordIDs[i] = data.getWordID(wordAsIntArray);
        }
        wordIDs[0] = -1; // Set a dummy value for the cell, where the candidates will be placed.

        // Check first, if the word is in the lexicon (must not be the correct one though)
        boolean inLexicon = data.contains(misspelledWord);
        // If the current word is in the lexicon, we calculate only the distances from 1 to 2,
        // to check if the word is maybe very unplausible in the given context.
        if (inLexicon) {
            localMaxThreshold = 1;
        }

        // Create a PriorityQueueSet, that has a special, destructive iterator (needed for the output -
        // the default iterator ignores the order in the queue)
        // and that makes sure, that there are no dublicted elements in the queue.
        PriorityQueueSet<WeightedWord> candidates = new PriorityQueueSet<WeightedWord>(50, compareWeigtedWords);

        for (int i = 0; i <= localMaxThreshold && candidates.size() < minCandidates; ++i) {
            candidates.addAll(correctWord(misspelledWord, wordIDs, i));
        }

        // Transform the items of the queue to strings only when needed
        return Iterables.transform(candidates,
                new Function<WeightedWord, Pair<String, Double>>() {
            public Pair<String, Double> apply(WeightedWord word) {
                int[] wordAsIntArray = data.getWordByID(word.getWordID());
                String wordAsString = StringTrie.intArrayToString(wordAsIntArray);

                return new Pair<String, Double>(wordAsString, word.getWeight());
            }
        });
    }

    /**
     * Delivers possible candidates for a word.
     *
     * @param misspelledWord
     * @return Iterable over word-weight tuples
     */
    public Iterable<Pair<String, Double>> correctWord(String misspelledWord) {
        String[] tempArray = new String[1];
        tempArray[0] = misspelledWord;
        return correctWordInContext(tempArray);
    }

    //
    /**
     * This method generates a priority queue for candidates the given word can
     * be corrected to within a given error threshold.
     *
     * @param misspelledWord
     * @param context
     * @param errorThreshold
     * @return PriorityQueue with all candidates for the misspelledWord.
     */
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
        LexiconTrie currentTrie;
        int currentLength;
        IntIterator symbolIt;
        int coDistance;
        int edDistance;

        // Add a starting item: An empyy word and the whole trie (=> starting state)
        agenda.add(new AgendaItem(new int[0], data.getLexicon()));
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
                context[0] = data.getWordID(currentConcatenation);

                double backOffDistance = data.getBackOffProbability(context);

                // Make sure, the context is found in the model
                if (backOffDistance < Double.POSITIVE_INFINITY) {
                    // This is not the best way to weight the edit distance and the probability, but at least
                    // it is way...
                    double p;
                    if (backOffDistance == 0) { // Probaility of 1
                        p = edDistance; //Avoid divistion by 0
                    } else {
                        p = edDistance - (1 / backOffDistance);
                    }
//                    System.err.println("b=" + backOffDistance);
//                    System.err.println("w=" + StringTrie.intArrayToString(currentConcatenation));
//                    System.err.println("W=" + StringTrie.intArrayToString(misspelledWord));
//                    System.err.println("e=" + edDistance);
//                    System.err.println("p=" + p + "\n");
                    WeightedWord word = new WeightedWord(context[0], p);
                    if (!candidates.contains(word)) {
                        candidates.add(word);
                    }
                }
            }
        }

        return candidates;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Privated classes
    /**
     * Items for the agenda in the private correctWord function. Contains a
     * concatenation and a subtrie (like the state in an automaton) for it.
     */
    private class AgendaItem {

        private final int[] concatenation;
        private final LexiconTrie subTrie;

        public AgendaItem(int[] concatenation, LexiconTrie subTrie) {
            this.concatenation = concatenation;
            this.subTrie = subTrie;
        }

        public int[] getConcatenation() {
            return concatenation;
        }

        public LexiconTrie getTrie() {
            return subTrie;
        }

        @Override
        public String toString() {
            return "AgendaItem{" + "concatenation=" + Arrays.toString(concatenation) + ", subTrie=" + subTrie.hashCode() + '}';
        }
    }

    /**
     * Holds a word as a wordID and its weight. Used for sorting in a
     * PriorityQueue.
     */
    private class WeightedWord {

        private final int wordID;
        private final double weight;

        public WeightedWord(int wordID, double weight) {
            this.wordID = wordID;
            this.weight = weight;
        }

        public int getWordID() {
            return wordID;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + this.wordID;
            hash = 97 * hash + (int) (Double.doubleToLongBits(this.weight) ^ (Double.doubleToLongBits(this.weight) >>> 32));
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
            if (this.wordID != other.wordID) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "weightedWord{" + "wordID=" + wordID + ", weight=" + weight + '}';
        }
    }
}
