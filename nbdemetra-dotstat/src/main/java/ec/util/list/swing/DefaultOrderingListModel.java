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
import java.util.List;

/**
 *
 * @author Philippe Charles
 */
public class DefaultOrderingListModel<E> extends DefaultListModel<E> implements OrderingListModel<E> {

    public DefaultOrderingListModel() {
        this(new ArrayList<E>());
    }

    public DefaultOrderingListModel(List<E> delegate) {
        super(delegate);
    }

    @Override
    public void move(int[] source, int destination) {
        List<E> reversedItems = new ArrayList<>(source.length);
        for (int i = source.length - 1; i >= 0; i--) {
            reversedItems.add(remove(source[i]));
        }
        addAll(destination, reversedItems);
    }
}
