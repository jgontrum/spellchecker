package de.up.ling.stud.automaton;

/**
 * Calculator for the CutOffEditDistance.
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class CutOffEditDistance {

    private final EditDistance distance;
    int minDistance;

    /**
     * Initializes the calculator with an EditDistance object, that will be the
     * base for all further computations.
     *
     * @param distance
     */
    public CutOffEditDistance(EditDistance distance) {
        this.distance = distance;
    }

    /**
     * String version to calculate the cutOffSitance between two words in the
     * range of an error threshold (slower).
     *
     * @param incorrectWord
     * @param correctWord
     * @param error_threshold
     * @return
     */
    public int calcCutOffDistance(String incorrectWord, String correctWord, int error_threshold) {
        minDistance = Integer.MAX_VALUE; // highes possible value
        int m = incorrectWord.length();
        int n = correctWord.length();
        int t = error_threshold;
        int l = Math.max(1, n - t);
        int u = Math.min(m, n + t);

        for (int i = l; i <= u; ++i) {
            int currentDistance = distance.calcDistance(incorrectWord.substring(0, i), correctWord);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
            }
        }

        return minDistance;
    }

    /**
     * Faster way to compute the cutOffSitance between two words.
     *
     * @param incorrectWord
     * @param candidate
     * @param error_threshold
     * @return
     */
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
