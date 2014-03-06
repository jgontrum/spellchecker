/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import java.util.Arrays;

/**
 * Computes the edit-distance between two Strings, based on the Levenshtein-Distance.
 * For further information, see http://en.wikipedia.org/wiki/Levenshtein_distance 
 * or Jurafsky & Martin: Introduction to Speech and Language Processing (2nd International Edition) (pp. 108)
 * 
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class EditDistance {
    private static int[][] matrix;
    private static int m;   // len(u)
    private static int n;   // len(v)

    
    public static int calcDistance(String firstWord, String secondWord) {
        String wordU = firstWord;
        String wordV = secondWord;
        m = firstWord.length();
        n = secondWord.length();
        
        
        initializeMatrix();
        
        // Iterate over rows and columns to calculate the value for each cell
        for (int x = 1; x < m + 1; ++x) {
            for (int y = 1; y < n + 1; ++y) {
                // Copy the value from the predecessor cell, if the letters match
                if (wordU.charAt(x-1) == wordV.charAt(y-1)) {
                    matrix[x][y] = matrix[x-1][y-1];
                } else {
                    // If not, compute values for possible deletion, insertion and substitution.
                    int deletion = matrix[x-1][y] + 1;
                    int insertion = matrix[x][y-1] + 1;
                    int substitution = matrix[x-1][y-1] + 1;
                    
                    matrix[x][y] = min(deletion, insertion, substitution);
                }
            }
        }
//        System.err.println("w1 = " + firstWord + "  \tw2 = " + secondWord + " = " + matrix[m][n]);

        
        return matrix[m][n];
    }
    
    public static int calcDistance(int[] firstWord, int[] secondWord) {
        m = firstWord.length;
        n = secondWord.length;


        initializeMatrix();

        // Iterate over rows and columns to calculate the value for each cell
        for (int x = 1; x < m + 1; ++x) {
            for (int y = 1; y < n + 1; ++y) {
                // Copy the value from the predecessor cell, if the letters match
                if (firstWord[x-1] == secondWord[y-1]) {
                    matrix[x][y] = matrix[x - 1][y - 1];
                } else {
                    // If not, compute values for possible deletion, insertion and substitution.
                    int deletion = matrix[x - 1][y] + 1;
                    int insertion = matrix[x][y - 1] + 1;
                    int substitution = matrix[x - 1][y - 1] + 1;

                    matrix[x][y] = min(deletion, insertion, substitution);
                }
            }
        }
//        System.err.println("w1 = " + Arrays.toString(firstWord) + "  \tw2 = " + Arrays.toString(secondWord) + " = " + matrix[m][n]);


        return matrix[m][n];
    }
    
    // Build the matrix and fill it with default values.
    private static void initializeMatrix() {
        matrix = new int[m+1][n+1];
        // fill the first entry of each row with a number i, 0 < i < m+1
        for (int x = 0; x < m+1; ++x) { 
            matrix[x][0] = x;
        }
        // do the same with the columns
        for (int y = 0; y < n+1; ++y) {
            matrix[0][y] = y;
        }
        // fill the rest of it with zeros (not neccacary, but safer)
        for (int x = 1; x < m+1; ++x) {
            for (int y = 1; y < n+1; ++y) {
                matrix[x][y] = 0;
            }
        }
    }
    
    // Returns a < b < c
    private static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }
    
    // Returns a human readeble version of the matrix.
    private static String matrixToString(String wordU, String wordV) {
        StringBuilder buf = new StringBuilder("  " + Arrays.toString(" ".concat(wordV).toCharArray()) + "\n");
        String word_u_buf = " ".concat(wordU);
        for (int x = 0; x < matrix.length; ++x) {
            buf.append(word_u_buf.charAt(x)).append(" ").append(Arrays.toString(matrix[x])).append("\n");
        }
        return buf.toString();
    }
}
