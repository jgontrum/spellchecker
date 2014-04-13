package de.up.ling.stud.automaton;

/**
 * Dummy class to hold a primitive int in an Object, that can be passed as a
 * reference to avoid copying. Used in the LexiconTrie class.
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
class IDCounter {

    int counter;

    public IDCounter(int counter) {
        this.counter = counter;
    }

    int getNextID() {
        ++counter;
        return counter - 1;
    }
}
