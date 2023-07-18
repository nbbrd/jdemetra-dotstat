package internal.jd3;

import ec.nbdemetra.ui.properties.IBeanEditor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.beans.IntrospectionException;
import java.util.function.Consumer;

public interface BeanEditor3 extends IBeanEditor {

    default boolean editBean(@NonNull Object bean, @NonNull Consumer<? super IntrospectionException> onError) {
        try {
            return editBean(bean);
        } catch (IntrospectionException ex) {
            onError.accept(ex);
            return false;
        }
    }
}
