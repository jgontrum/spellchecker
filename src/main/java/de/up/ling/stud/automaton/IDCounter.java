/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

/**
 *
 * @author johannes
 */
    
class IDCounter {
    int counter;

    public IDCounter(int counter) {
        this.counter = counter;
    }

    int getNextID() {
        ++counter;
        return counter-1;
    }
}
