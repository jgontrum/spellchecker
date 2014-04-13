/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.automaton;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Subclass for a PriorityQueue that allows to define a destructive iterator,
 * that returns items in the order of the queue and that implements a set-like
 * behavior to make sure, that each element exists only once in the queue.
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
class PriorityQueueSet<E> extends PriorityQueue<E> {

    private final PriorityQueue<E> queue;

    /**
     * Initialize this queue like a normal PriorityQueue with a comparator and
     * an initial capacity.
     *
     * @param initialCapacity
     * @param comparator
     */
    PriorityQueueSet(int initialCapacity, Comparator<? super E> comparator) {
        this.queue = new PriorityQueue<E>(initialCapacity, comparator);
    }

    /**
     * Returns a destructive iterator, that iterates over the queue in the
     * defined order.
     *
     * @return Destructive iterator.
     */
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

    /**
     * Overwrites the normal offer method to check first, if the element e does
     * already exist in the queue.
     *
     * @param e
     * @return False, if e is already in the queue.
     */
    @Override
    public boolean offer(E e) {
        if (queue.contains(e)) {
            return false;
        } else {
            return queue.offer(e);
        }
    }

    /**
     * Returns the size of the queue.
     *
     * @return Size of the queue.
     */
    @Override
    public int size() {
        return queue.size();
    }
}
