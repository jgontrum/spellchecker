package de.up.ling.stud.automaton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws FileNotFoundException, IOException {
        System.err.println("CutOffEditdistance is " + CutOffEditDistance.calcCutOffDistance("Kooperatio", "Kooperation", 2));
        System.err.println("Editdistance is " + EditDistance.calcDistance("Kooperatio", "Kooperation"));
        
        StringTrie testTrie = new StringTrie();
               
        testTrie.put(new FileReader(new File("/Users/gontrum/Development/SpellingCorrection/data/174pcc.orig.utf8.txt")));
        
        Corrector cor = new Corrector(testTrie);
//        System.err.println(testTrie.contains("Kooperatio"));
        String misspelledWord = "Hase";
        
        for (String offer : cor.correctWordDynamicly(misspelledWord)) {
            System.err.println("Maybe you ment '" + offer + "'?");
        }
        
    }
}
