/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class CutOffEditDistance {
    public static int calcCutOffDistance(String incorrectWord, String correctWord, int error_threshold) {
        int m = incorrectWord.length();
        int n = correctWord.length();
        int t = error_threshold;
        int l = Math.max(1, n-t);
        int u = Math.min(m, n+t);
        
        int minDistance = Integer.MAX_VALUE;
        
        for (int i = l; i <= u; ++i) {
            int currentDistance = EditDistance.calcDistance(incorrectWord.substring(0, i), correctWord);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
            }
        }
        
        return minDistance;
    }
    
    public static int calcCutOffDistance(int[] incorrectWord, int[] correctWord, int error_threshold) {
        int m = incorrectWord.length;
        int n = correctWord.length;
        int t = error_threshold;
        int l = Math.max(1, n - t);
        int u = Math.min(m, n + t);

        int minDistance = Integer.MAX_VALUE;

        for (int i = l; i <= u; ++i) {
            int[] subString = new int[i];
            System.arraycopy(incorrectWord, 0, subString, 0, subString.length);
            int currentDistance = EditDistance.calcDistance(subString, correctWord);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
            }
        }

        return minDistance;
    }
}
