/*
 * Copyright 2017 National Bank of Belgium
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
package internal.desktop;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
public final class PropertyAdapter<X, Y> extends Node.Property<Y> {

    public static <X, Y> Node.Property<Y> of(Object bean, String property, Class<X> source, Class<Y> target, Function<X, Y> forward, Function<Y, X> backward) {
        try {
            return new PropertyAdapter<>(new PropertySupport.Reflection(bean, source, property), target, forward, backward);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final Node.Property<X> source;
    private final Function<X, Y> forward;
    private final Function<Y, X> backward;

    private PropertyAdapter(Node.Property<X> source, Class<Y> target, Function<X, Y> forward, Function<Y, X> backward) {
        super(target);
        this.source = source;
        this.forward = forward;
        this.backward = backward;
    }

    @Override
    public boolean canRead() {
        return source.canRead();
    }

    @Override
    public Y getValue() throws IllegalAccessException, InvocationTargetException {
        return forward.apply(source.getValue());
    }

    @Override
    public boolean canWrite() {
        return source.canWrite();
    }

    @Override
    public void setValue(Y t) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        source.setValue(backward.apply(t));
    }
}
