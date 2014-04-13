/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * 
 * @author gontrum
 */
class PriorityQueueSet<E> extends PriorityQueue<E>{
    private final PriorityQueue<E> queue;
    
    PriorityQueueSet(int initialCapacity, Comparator<? super E> comparator) {
        this.queue = new PriorityQueue<E>(initialCapacity, comparator);
    }

    // Destructive Iterator!
    @Override
    public Iterator<E> iterator() {
        Iterator<E> it = new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public E next() {
                return queue.poll();
            }

            @Override
            public void remove() {
                queue.remove();
            }
        };
        return it;    
    }
    
    @Override
    public boolean offer(E e) {
        if (queue.contains(e)) {
            return false;
        } else {
            return queue.offer(e);
        }
    }
    
    @Override
    public int size() {
        return queue.size();
    }
}
