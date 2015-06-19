/*
 * Copyright 2015 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.util.list.swing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.swing.AbstractListModel;

/**
 *
 * @author Philippe Charles
 */
public class DefaultListModel<E> extends AbstractListModel<E> implements List<E> {

    private final List<E> delegate;

    public DefaultListModel() {
        this(new ArrayList<E>());
    }

    public DefaultListModel(List<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSize() {
        return delegate.size();
    }

    @Override
    public E getElementAt(int index) {
        return delegate.get(index);
    }

    //<editor-fold defaultstate="collapsed" desc="List methods">
    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new IteratorImpl(delegate.iterator());
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(E e) {
        int index = delegate.size();
        boolean result = delegate.add(e);
        fireIntervalAdded(this, index, index);
        return result;
    }

    @Override
    public boolean remove(Object o) {
        int index = delegate.indexOf(o);
        boolean result = delegate.remove(o);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return false;
        }
        int index = delegate.size();
        boolean result = delegate.addAll(c);
        fireIntervalAdded(this, index, index + c.size() - 1);
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if (c.isEmpty()) {
            return false;
        }
        boolean result = delegate.addAll(index, c);
        fireIntervalAdded(this, index, index + c.size() - 1);
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c.isEmpty()) {
            return false;
        }
        boolean result = delegate.removeAll(c);
        if (result) {
            fireContentsChanged(this, 0, delegate.size() - 1);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        if (delegate.isEmpty()) {
            return;
        }
        int index = delegate.size() - 1;
        delegate.clear();
        fireIntervalRemoved(this, 0, index);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public E set(int index, E element) {
        E result = delegate.set(index, element);
        fireContentsChanged(this, index, index);
        return result;
    }

    @Override
    public void add(int index, E element) {
        delegate.add(index, element);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public E remove(int index) {
        E result = delegate.remove(index);
        fireIntervalRemoved(this, index, index);
        return result;
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ListIteratorImpl(delegate.listIterator(), 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListIteratorImpl(delegate.listIterator(index), index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(delegate.subList(fromIndex, toIndex));
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private final class IteratorImpl implements Iterator<E> {

        private final Iterator<E> iterator;
        private int index;

        public IteratorImpl(Iterator<E> iterator) {
            this.iterator = iterator;
            this.index = -1;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            index++;
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
            fireIntervalRemoved(DefaultListModel.this, index, index);
        }
    }

    private final class ListIteratorImpl implements ListIterator<E> {

        private final ListIterator<E> iterator;
        private int index;

        public ListIteratorImpl(ListIterator<E> iterator, int index) {
            this.iterator = iterator;
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            index++;
            return iterator.next();
        }

        @Override
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        @Override
        public E previous() {
            index--;
            return iterator.previous();
        }

        @Override
        public int nextIndex() {
            return iterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return iterator.previousIndex();
        }

        @Override
        public void remove() {
            iterator.remove();
            fireIntervalRemoved(DefaultListModel.this, index, index);
        }

        @Override
        public void set(E e) {
            iterator.set(e);
            fireContentsChanged(DefaultListModel.this, index, index);
        }

        @Override
        public void add(E e) {
            iterator.add(e);
            fireIntervalAdded(DefaultListModel.this, index, index);
        }
    }
    //</editor-fold>
}
