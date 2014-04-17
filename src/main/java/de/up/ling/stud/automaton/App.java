package de.up.ling.stud.automaton;

import de.saar.basic.Pair;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Main class that uses arguments from the command line and shows, how the
 * internal classes can be used.
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class App {

    private static String corpus;
    private static String textFile;
    private static String saveTo;
    private static String loadFile;
    private static String resultFile;
    private static String drawLexiconFile;
    private static String drawModelFile;
    private static String encoding;
    private static boolean printInfo;
    private static boolean verbose;
    private static boolean details;
    private static int ngram;
    private static StringTrie data;
    private static int numSuggestions;
    // Define a pattern for the tokenizer, that matches all characters, that are not letters.
    // This includes german umlauts as well. Taken from: http://stackoverflow.com/a/1612015
    private static final Pattern tokenizerPattern = Pattern.compile("[^\\p{L}]");

    /**
     * Takes a lot of arguments to create or load data and to check and correct
     * a textfile.
     *
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        numSuggestions = 5; // The number of wor candidates, that will be printed, when --details is enabled
        if (args.length < 2) {
            printInfo();
            System.exit(1);
        }

        parseArguments(args);

        if (printInfo) {
            printInfo();
        } else {
            if (corpus.equals("")) {
                assert saveTo.equals("");
                verbose("Reading trie from file. This can take a while.");
                data = new StringTrie(loadFile, encoding);
                data.setVerbose(verbose);
                verbose("Done!");
            } else {
                assert !corpus.equals("");
                verbose("Creating a new trie from a corpus. This can take a while.");
                data = new StringTrie(ngram);
                data.setVerbose(verbose);
                data.putFile(corpus, encoding);
                verbose("Done!");
                if (!saveTo.equals("")) {
                    verbose("Saving the trie to a file. This can take a while.");
                    data.saveToFile(saveTo, encoding);
                    verbose("Done!");
                }
            }
            assert data != null;
            data.setVerbose(verbose);

            if (!drawLexiconFile.equals("")) {
                data.drawLexicon(drawLexiconFile);
            }

            if (!drawModelFile.equals("")) {
                data.drawLanguageModel(drawModelFile);
            }

            if (!textFile.equals("")) {
                assert !resultFile.equals("");
                data.postProcessing();
                correctFile(textFile, resultFile);
            }
        }
    }

    /**
     * Corrects a file and writes the result in another file.
     *
     * @param fileIn
     * @param fileOut
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void correctFile(String fileIn, String fileOut) throws FileNotFoundException, IOException {
        InputStream textInputStream = new FileInputStream(new File(fileIn));
        Reader textInReader = new InputStreamReader(textInputStream, encoding);
        BufferedReader textIn = new BufferedReader(textInReader);

        OutputStream textOutputStream = new FileOutputStream(new File(fileOut));
        Writer textOutWriter = new OutputStreamWriter(textOutputStream, encoding);
        BufferedWriter textOut = new BufferedWriter(textOutWriter);

        String currentLine;
        String currentWord;

        Corrector corrector = new Corrector(data);

        // init context window
        String[] window = new String[ngram];
        for (int i = 0; i < ngram; i++) {
            window[i] = "";
        }

        // Tokenize the current line.
        while ((currentLine = textIn.readLine()) != null) {
            String[] tokenized = tokenizerPattern.split(currentLine);
            for (int i = 0; i < tokenized.length; i++) {
                currentWord = tokenized[i];
                if (currentWord.length() > 0) {
                    for (int j = 0; j < ngram - 1; j++) {
                        window[j] = window[j + 1]; // move the window to the left
                    }

                    window[ngram - 1] = currentWord;

                    Iterator<Pair<String, Double>> candidates = corrector.correctWordInContext(window).iterator();

                    if (candidates.hasNext()) {
                        if (details) {
                            String space = "";
                            for (int j = currentWord.length(); j < 30; ++j) {
                                space = space.concat(" ");
                            }

                            verbose("Correcting the word \"" + currentWord + "\" to: ");

                            // Case for the best candidate:
                            Pair<String, Double> currentPair = candidates.next();
                            String currentSuggestion = currentPair.left;
                            if (currentSuggestion.equals(currentWord)) {
                                textOut.write("\u2713 " + currentWord + space + " |Suggestions: ");
                            } else {
                                textOut.write("\u2717 " + currentWord + space + " |Suggestions: ");
                            }

                            verbose(" * " + currentSuggestion + "  \t(" + currentPair.right + ")");
                            textOut.write(currentSuggestion + " (" + currentPair.right + ")");
                            textOut.write((!candidates.hasNext()) ? "" : ", ");

                            // Other candidates
                            for (int j = 1; j < numSuggestions && candidates.hasNext(); j++) {
                                currentPair = candidates.next();
                                currentSuggestion = currentPair.left;
                                verbose(" * " + currentSuggestion + "  \t(" + currentPair.right + ")");
                                textOut.write(currentSuggestion + " (" + currentPair.right + ")");
                                textOut.write((j == numSuggestions - 1 || !candidates.hasNext()) ? "" : ", ");
                            }
                            verbose("");
                            textOut.newLine();
                        } else {
                            String currentSuggestion = candidates.next().left;
                            textOut.write(currentSuggestion + " ");
                            verbose("Correcting the word \"" + currentWord + "\" to \"" + currentSuggestion + "\".");
                        }
                    } else {
                        verbose("No candidate found for \"" + currentWord + "\".");
                        if (details) {
                            String space = "";
                            for (int j = currentWord.length(); j < 30; ++j) {
                                space = space.concat(" ");
                            }

                            textOut.write("\u2717 " + currentWord + space + " |Suggestions: <NONE>\n");
                        } else {
                            textOut.write("<NOTFOUND>");
                        }
                    }
                }
                if (!details && currentWord.length() > 0) {
                    textOut.write("\n");
                }
                textOut.flush();
            }
        }
        textOut.close();
    }

    private static void parseArguments(String[] args) {
        corpus = "";
        textFile = "";
        saveTo = "";
        loadFile = "";
        resultFile = "";
        drawLexiconFile = "";
        drawModelFile = "";
        encoding = "UTF-8";
        printInfo = false;
        verbose = false;
        details = false;
        ngram = 3;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--verbose") || args[i].equals("-v")) {          // Verbose
                verbose = true;
            } else if (args[i].equals("--corpus") || args[i].equals("-c")) {    // Corpus
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    corpus = args[i + 1];
                    if (!fileExists(corpus)) {
                        System.err.println("The corpus file specifiy with --corpus does not exist. Pleas choose a valid one.");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Please specify a correct filename for the input corpus.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--save") || args[i].equals("-s")) {    // Savefile
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    saveTo = args[i + 1];
                } else {
                    System.err.println("Please specify a correct filename to save the created trie.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--ngram") || args[i].equals("-n")) {     // nGram
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    ngram = Integer.parseInt(args[i + 1]);
                } else {
                    System.err.println("Please specify a number of ngrams to train the language model.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--load") || args[i].equals("-l")) {     // load file
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    loadFile = args[i + 1];
                    if (!fileExists(loadFile)) {
                        System.err.println("The data file specifiy with --load does not exist. Pleas choose a valid one.");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Please specify a correct filename to load an existing trie.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--check") || args[i].equals("--correct")) {     // textfile
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    textFile = args[i + 1];
                    if (!fileExists(textFile)) {
                        System.err.println("The textfile specifiy with --correct does not exist. Pleas choose a valid one.");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Please specify a correct filename to an textfile that will be corrected.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--result")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    resultFile = args[i + 1];
                } else {
                    System.err.println("Please specify a correct filename to save the corrected file in.");
                    System.exit(1);
                }
            } else if (args[i].equals("--encoding") || args[i].equals("--enc")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    encoding = args[i + 1];
                } else {
                    System.err.println("Please specify a correct encoding for the corpus.");
                    System.exit(1);
                }
            } else if (args[i].equals("--draw-lexicon")) {     // lexicon -> dot
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    drawLexiconFile = args[i + 1];
                } else {
                    System.err.println("Please specify a correct filename to draw the lexicon in a dot-file.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--draw-model")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    drawModelFile = args[i + 1];
                } else {
                    System.err.println("Please specify a correct filename to draw the language model in a dot-file.\nUse --help to view all commands.");
                    System.exit(1);
                }
            } else if (args[i].equals("--details") || args[i].equals("-d")) {   // result info
                details = true;
            } else if (args[i].equals("--info") || args[i].equals("--help") || args[i].equals("-h")) {   // result info
                printInfo = true;
            }
        }
        // Check if the combinations are valid.

        if (corpus.equals("") && loadFile.equals("")) {
            System.err.println("Your arguments are not valid: Please specify a source for the trie / a corpus.\nUse --help to view all commands.");
            System.exit(1);
        }
        if (!loadFile.equals("") && !saveTo.equals("")) {
            System.err.println("Your arguments are not valid: If you load an existing trie, you should not save it again.\nUse --help to view all commands.");
            System.exit(1);
        }

        if (!textFile.equals("") && resultFile.equals("")) {
            System.err.println("Your arguments are not valid: You have to save the corrected version of your file.\nUse --help to view all commands.");
            System.exit(1);
        }

        if (!resultFile.equals("") && textFile.equals("")) {
            System.err.println("Your arguments are not valid: If you enter a resultfile, you have to specify a source for the text.\nUse --help to view all commands.");
            System.exit(1);
        }

        if (resultFile.equals("") && details == true) {
            System.err.println("Your arguments are not valid: If you cannot use the --details switch if you do not correct a textfile.\nUse --help to view all commands.");
            System.exit(1);
        }

//        if (textFile.equals("") && saveTo.equals("")) {
//            System.err.println("Your arguments are not valid: Please specify a file that should be corrected (--check) and / or where a newly created trie should be saved (--save).\nUse --help to view all commands.");
//            System.exit(1);
//        }

    }

    private static void printInfo() {
        System.err.println("SpellChecker: Corrects the text in a textfile based on a lexicon and language model, that has to be learned from a huge text corpus.\n"
                + "Usage:  java -jar SpellChecker.jar [options]\n"
                + "\n"
                + "Options:\n"
                + "  --check <arg>                 The textfile that should be corrected by the spell checker.\n"
                + "  --corpus, -c <arg>            Creates a new lexicon and language model based on a text corpus\n "
                + "                                that is stored in a single file.\n"
                + "  --correct <arg>               See --check\n"
                + "  --details, -d                 The top 5 candidates for a word will be saved in the output file. \n"
                + "                                This is a great way to understand the accuracy of the program.\n"
                + "  --draw-lexicon <arg>          Saves the lexicon as a trie in graphviz-format. This should only be used, \n"
                + "                                when trained on a very small corpus.\n"
                + "  --draw-model <arg>            Saves the language model as a trie in graphviz-format. This should only be used, \n"
                + "                                when trained on a very small corpus.\n"
                + "  --encoding, --enc,  <arg>     The used encoding for textfile and corpus. Default is UTF-8\n"
                + "  --help, --info                Shows this message.\n"
                + "  --load, -l <arg>              Loads the data, that has been trained using --corpus and saved with --save.\n"
                + "  --ngram <arg>                 The number of ngrams that should be used to learn a language model. The default value is 3.\n"
                + "  --result <arg>                If a textfile is specified by using --check, the result has to be saved in a file.\n"
                + "  --save, -s <arg>              If data is learned from a corpus, it should be saved in a new file.\n"
                + "  --verbose, -v                 Prints additional information.\n"
                + "\n"
                + "Examples:\n"
                + "\n"
                + "Learn data from a corpus and save it to a file:\n"
                + "--corpus /path/to/corpus --enc UTF-8 --ngram 2 --save /path/to/output.spell\n"
                + "\n"
                + "Load learned data and correct a file:\n"
                + "--load /path/to/output.spell --check /path/to/text --result /path/to/corrected.file\n");
    }

    private static void verbose(String text) {
        if (verbose) {
            System.out.println(text);
        }
    }

    private static boolean fileExists(String filename) {
        File f = new File(filename);
        return f.exists();
    }
}
