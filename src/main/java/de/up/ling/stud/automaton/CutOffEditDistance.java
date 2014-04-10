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
    private final EditDistance distance;
    int minDistance;

    
    public CutOffEditDistance(EditDistance distance) {
        this.distance = distance;
    }
    
    
    public int calcCutOffDistance(String incorrectWord, String correctWord, int error_threshold) {
        minDistance = Integer.MAX_VALUE; // highes possible value
        int m = incorrectWord.length();
        int n = correctWord.length();
        int t = error_threshold;
        int l = Math.max(1, n-t);
        int u = Math.min(m, n+t);
                
        for (int i = l; i <= u; ++i) {
            int currentDistance = distance.calcDistance(incorrectWord.substring(0, i), correctWord);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
            }
        }
        
        return minDistance;
    }
    
    public int calcCutOffDistance(int[] incorrectWord, int[] candidate, int error_threshold) {
        int m = incorrectWord.length;
        int n = candidate.length;
        int l = Math.max(1, n - error_threshold);
        int u = Math.min(m, n + error_threshold);

        for (int i = l; i <= u; ++i) {
            int currentDistance = distance.calcDistance(incorrectWord, 0, i, candidate, 0, n);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
            }
        }

        return minDistance;
    }
}
