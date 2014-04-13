/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.regex.Pattern;
import java.util.zip.*;

/**
 * Wrapper class that contains the tries for the lexicon and the language model.
 * It also has the ability to save itself in a gzip compressed file and also to
 * restore itself from it.
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class StringTrie {

    private LexiconTrie actualTrie;
    private BackOffModelTrie contextTrie;
    private final Int2ObjectMap<int[]> idToWordMap;
    private int context;
    private final static int delimiter = 0;
    private boolean verbose = false;
    // Define a pattern for the tokenizer, that matches all characters, that are not letters.
    // This includes german umlauts as well. Taken from: http://stackoverflow.com/a/1612015
    private static final Pattern tokenizerPattern = Pattern.compile("[^\\p{L}]");

    ////////////////////////////////////////////////////////////////////////////
    ///// Constructors
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Initialize the String Trie with a default context of 3. Put words into
     * the trie and call the 'postProcessing()'-method afterwards.
     */
    public StringTrie() {
        idToWordMap = new Int2ObjectOpenHashMap<int[]>();
        init(3);
    }

    /**
     * Create a new String Trie with a given context (nGram) Put words into the
     * trie and call the 'postProcessing()'-method afterwards.
     *
     * @param context
     */
    public StringTrie(int context) {
        idToWordMap = new Int2ObjectOpenHashMap<int[]>();
        init(context);
    }

    /**
     * Restore an already created String Trie from file. It is possible to
     * change the newly created Trie before calling the
     * 'postProcessing()'-method.
     *
     * @param filename
     */
    public StringTrie(String filename, String encoding) {
        // Initializing
        idToWordMap = new Int2ObjectOpenHashMap<int[]>();
        int[] delimiterWord = new int[1];
        delimiterWord[0] = 0;
        idToWordMap.put(0, delimiterWord);

        try {
            // Open file and decode the gzip compressed data on the fly.
            InputStream inZip = new GZIPInputStream(new FileInputStream(filename));
            Reader decoder = new InputStreamReader(inZip, encoding);
            BufferedReader buffer = new BufferedReader(decoder);

            if (buffer.readLine().equals("SpellCheckerDictionary")) {
                if (verbose) {
                    System.err.println("Restoring a trie from the file " + filename);
                }
                restoreConfig(buffer);
                if (verbose) {
                    System.err.println("Basic configuration restored!");
                }
                restoreLexicon(buffer);
                if (verbose) {
                    System.err.println("Lexicon restored!");
                }
                restoreLanguageModel(buffer);
                if (verbose) {
                    System.err.println("Language model restored. \n"
                            + "Done restoring from file.");
                }
            } else {
                System.err.println("Please choose a valid file.");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Modifier
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Reads a textfile and creates the lexicon and the language model from it.
     *
     * @param filename
     * @throws IOException
     */
    public void putFile(String filename, String encoding) throws IOException {
        InputStream textInputStream = new FileInputStream(new File(filename));
        Reader textInReader = new InputStreamReader(textInputStream, encoding);
        BufferedReader buffer = new BufferedReader(textInReader);

        String currentLine;
        String currentWord;
        double currentLineNumber = 0.0;
        int lastOutput = -1;
        int totalNumberOfLines = 0;

        // create the moving window for the context
        // and initialize it
        int[] idWindow = new int[context];
        for (int i = 0; i < context; i++) {
            idWindow[i] = delimiter;
        }
        if (verbose) {
            totalNumberOfLines = countLines(filename); // count lines
            System.err.println("Creating the database from file " + filename);
            System.err.print("Progress: ");
        }

        while ((currentLine = buffer.readLine()) != null) {
            ++currentLineNumber;

            // Tokenize the current line
            String[] tokenized = tokenizerPattern.split(currentLine);
            for (int i = 0; i < tokenized.length; i++) {
                currentWord = tokenized[i];
                if (currentWord.length() > 0) {
                    for (int j = 0; j < context - 1; j++) {
                        idWindow[j] = idWindow[j + 1]; // move the window to the left
                    }

                    int currentID = put(currentWord); // Save word in trie and get id for it
                    idWindow[context - 1] = currentID;

                    putContext(idWindow); // Store the ids for the words in the language model
                }
            }

            if (verbose) {
                int progress = (int) Math.round(currentLineNumber / totalNumberOfLines * 100);
                if (progress != lastOutput) {
                    lastOutput = progress;
                    System.err.print(progress + "% ");
                }
            }
        }

        if (verbose) {
            System.err.println("\nFile read successfully.");
        }
    }

    /**
     * Store a string in the lexicon (does not affect the language model).
     *
     * @param key
     * @return The ID for the entered word.
     */
    public int put(String key) {
        int[] decodedWord = stringToIntArray(key);
        int id = actualTrie.put(decodedWord);
        idToWordMap.put(id, decodedWord);
        return id;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Getter
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Check, if a given String is already in the lexicon
     *
     * @param needle
     * @return
     */
    public boolean contains(String needle) {
        return actualTrie.contains(stringToIntArray(needle));
    }

    /**
     * Check, if a given word as int-array is already in the lexicon
     *
     * @param needle
     * @return
     */
    public boolean contains(int[] needle) {
        return actualTrie.contains(needle);
    }

    /**
     * Return the Backoff-Probability for a given key. The key must have the
     * length of the context of this trie. Each cell of the array must store the
     * ID of a word.
     *
     * @param key
     * @return
     */
    public double getBackOffProbability(int[] key) {
        return contextTrie.getProbability(key);
    }

    /**
     * Returns the lecicon
     *
     * @return
     */
    public LexiconTrie getLexicon() {
        return actualTrie;
    }

    /**
     * Calculates the probabilities in the language model. Note: After calling
     * this method, it can not be changed anymore!
     */
    public void postProcessing() {
//        contextTrie.calculateMLE();
        contextTrie.calculateMLElog();
    }

    /**
     * Returns the number of nGrams
     *
     * @return
     */
    public int getNGram() {
        return context;
    }
    ////////////////////////////////////////////////////////////////////////////
    ///// Helper functions
    ////////////////////////////////////////////////////////////////////////////

    private void init(int context) {
        this.context = context;
        actualTrie = new LexiconTrie(0, new IDCounter(1));
        contextTrie = new BackOffModelTrie(context, context);
        int[] delimiterWord = new int[1];
        delimiterWord[0] = 0;
        idToWordMap.put(0, delimiterWord);
    }

    // Returns the ID for a given word
    public int getWordID(int[] word) {
        return actualTrie.put(word);
    }

    /**
     * Returns the word for a given ID.
     *
     * @param id
     * @return
     */
    public int[] getWordByID(int id) {
        return idToWordMap.get(id);
    }

    // Transforms a list of word-IDs to a String, each word seperatd by 0
    String idsToWordsReadable(IntList ids) {
        StringBuilder ret = new StringBuilder();

        for (int i = 0; i < ids.size(); i++) {
            int[] currentWord = getWordByID(ids.get(i));
            ret.append(intArrayToString(currentWord));
            ret.append((i < ids.size() - 1) ? String.format("%d", delimiter) : "");
        }
        return ret.toString();
    }

    // Writes a context into the language model
    private void putContext(int[] contextWindow) {
        contextTrie.put(contextWindow, this.context);
    }

    /**
     * Converts a String into an int array, that holds the numeric values of the
     * chars.
     *
     * @param word
     * @return
     */
    public static int[] stringToIntArray(String word) {
        int[] ret = new int[word.length()];

        for (int i = 0; i < word.length(); i++) {
            ret[i] = word.charAt(i);
        }
        return ret;
    }

    /**
     * Converts an array of numbers to a String by casting the ints to char.
     *
     * @param word
     * @return
     */
    public static String intArrayToString(int[] word) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < word.length; i++) {
            buf.append(word[i] == 0 ? String.format("%d", delimiter) : (char) word[i]);
        }
        return buf.toString();
    }

    // Converts a list of ints to a String by casting them to char.
    private String intListToString(IntList word) {
        StringBuilder buf = new StringBuilder();
        for (int i = word.size() - 1; i >= 0; i--) {
            buf.append((char) word.getInt(i));
        }
        return buf.toString();
    }

    // Counts the lines of a file in a very fast way.
    // Code taken from: http://stackoverflow.com/a/453067
    private int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }

    // Convert an array that stores the information for a context as characters
    // to an array of the length of context with the word IDs.
    int[] shortenContext(int[] word) {
        int[] ret = new int[context];
        int counter = 0, begin = 0;

        for (int i = 0; i < word.length - 1; ++i) {
            if (word[i] == 0) {
                if (begin - i == 0) { // leading 0
                    ret[counter] = delimiter;
                    ++counter;
                } else {
                    int[] lookUp = new int[i - begin];
                    System.arraycopy(word, begin, lookUp, 0, i - begin);
                    ret[counter] = actualTrie.getID(lookUp);
                    ++counter;
                    begin = i + 1;
                }
            }
        }
        // Case for the last character
        int pos = word.length - 1;
        if (word[pos] != 0) {
            ++pos;
        }
        int[] lookUp = new int[pos - begin];
        System.arraycopy(word, begin, lookUp, 0, pos - begin);
        ret[counter] = actualTrie.getID(lookUp);

        // fill the rest of the array with the delimiter value
        for (int i = counter + 1; i < ret.length; i++) {
            ret[i] = delimiter;
        }

        return ret;
    }

    /**
     * Enables/disables the verbose mode.
     *
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Filemanagement
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Draws the language model as dot-file. Note: This method calls the
     * postProcessing method!
     *
     * @param filename
     */
    public void drawLanguageModel(String filename) {
        postProcessing();
        try {
            File file = new File(filename);
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write(contextTrie.draw(this));
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Draws the lexicon as dot-file
     *
     * @param filename
     */
    public void drawLexicon(String filename) {
        try {
            File file = new File(filename);
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write(actualTrie.draw());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the whole trie in a file.
     *
     * @param filename
     * @throws IOException
     */
    public void saveToFile(String filename, String encoding) throws IOException {
        try {
            GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(filename)));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(zip, encoding));
            if (verbose) {
                System.out.println("Saving the file in " + filename);
                System.out.println("Writing configuration...");
            }
            bw.write("SpellCheckerDictionary\n");
            saveConfig(bw);
            if (verbose) {
                System.out.println("Writing lexicon...");
            }
            bw.write(String.format("%d", actualTrie.getNextID()) + "\n");
            actualTrie.saveWordsAndID(new int[0], bw);
            bw.write("#\n");
            if (verbose) {
                System.out.println("Writing language model...");
            }
            contextTrie.saveToFile(new int[0], bw);
            bw.close();
            if (verbose) {
                System.out.println("Done writing the file.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig(BufferedWriter bw) throws IOException {
        bw.write("ngram : " + context + "\n");
        bw.write("#\n");
        bw.flush();
    }

    private void restoreConfig(BufferedReader br) throws IOException {
        String line = br.readLine();
        context = Integer.parseInt(line.substring("ngram : ".length()));
    }

    private void restoreLexicon(BufferedReader buffer) throws IOException {
        buffer.readLine(); // get rid of the # character
        int oldMaxID = Integer.parseInt(buffer.readLine());
        actualTrie = new LexiconTrie(oldMaxID, new IDCounter(oldMaxID));
        for (String currentLine = buffer.readLine(); !currentLine.equals("#"); currentLine = buffer.readLine()) {
            // Line = 123,123,45,56 : 12
            String[] parts = currentLine.split(":"); // seperate the word from the id
            String[] word = parts[0].split(","); // seperate the characters
            int id = Integer.parseInt(parts[1]); // parse the id to int
            int[] decodedWord = new int[word.length];

            for (int i = 0; i < word.length; i++) {
                decodedWord[i] = word[i].charAt(0); // transform the chars to ints
            }

            // save the word in the trie with the stored id.
            // note that the ids of the final states will be restored from file,
            // while other states maybe get a different id than before.
            actualTrie.putWithID(decodedWord, id);
            idToWordMap.put(id, decodedWord);
        }
    }

    private void restoreLanguageModel(BufferedReader buffer) throws IOException {
        contextTrie = new BackOffModelTrie(context, context);
        for (String currentLine = buffer.readLine(); currentLine != null; currentLine = buffer.readLine()) {
            // line: 0,2,3:9999  (wordIDs:count)
            String[] parts = currentLine.split(":"); // seperate the words from the counts
            String[] words = parts[0].split(","); // seperate the words
            int[] wordIDs = new int[words.length];
            int counts = Integer.parseInt(parts[1]); // parse the counts to int
            for (int i = 0; i < wordIDs.length; i++) {
                wordIDs[i] = Integer.parseInt(words[i]); // convert stings of numbers to integer
            }

            contextTrie.putWithCount(wordIDs, context, counts);

        }
    }

    /**
     * Returns all worlds in the lexicon as Strings.
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Words in Trie:\n");

        for (IntArrayList concatenation : actualTrie.getAllConcatinations()) {

            buf.append(intListToString(concatenation)).append("\n");
        }

        return buf.toString();
    }
}
