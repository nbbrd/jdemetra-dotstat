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
package be.nbb.sdmx.facade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Parameter that defines the dimension values of the data to be returned.
 *
 * @author Philippe Charles
 */
@Immutable
public final class Key {

    private static final String WILDCARD = "";

    public static final Key ALL = new Key(new String[]{WILDCARD});

    private final String[] items;

    private Key(@Nonnull String[] items) {
        this.items = items;
    }

    @Nonnegative
    public int getSize() {
        return items.length;
    }

    @Nonnull
    public String getItem(@Nonnegative int index) throws IndexOutOfBoundsException {
        return items[index];
    }

    public boolean isWildcard(@Nonnegative int index) throws IndexOutOfBoundsException {
        return WILDCARD.equals(items[index]);
    }

    public boolean contains(@Nonnull Key input) {
        for (int i = 0; i < getSize(); i++) {
            if (!isWildcard(i) && !getItem(i).equals(input.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (items.length == 1 && isWildcard(0)) {
            return "all";
        }
        StringBuilder result = new StringBuilder();
        result.append(items[0]);
        for (int i = 1; i < items.length; i++) {
            result.append('.').append(items[i]);
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Key && equals((Key) obj));
    }

    private boolean equals(Key that) {
        return Arrays.equals(this.items, that.items);
    }

    @Nonnull
    public static Key parse(@Nonnull String input) {
        return "all".equals(input.trim()) ? ALL : valueOf(input.split("\\.", -1));
    }

    @Nonnull
    public static Key valueOf(@Nonnull String... input) {
        if (input.length == 0) {
            return ALL;
        }
        String[] result = new String[input.length];
        for (int i = 0; i < result.length; i++) {
            String item = input[i];
            if (item == null) {
                item = WILDCARD;
            } else {
                item = item.trim();
                switch (item) {
                    case "*":
                    case "+":
                        item = WILDCARD;
                }
            }
            result[i] = item;
        }
        return new Key(result);
    }

    @Nonnull
    public static Builder builder(@Nonnull DataStructure dfs) {
        Map<String, Integer> index = new HashMap<>();
        for (Dimension o : dfs.getDimensions()) {
            index.put(o.getId(), o.getPosition() - 1);
        }
        return new Builder(index);
    }

    @Nonnull
    public static Builder builder(@Nonnull String... dimensions) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < dimensions.length; i++) {
            index.put(dimensions[i], i);
        }
        return new Builder(index);
    }

    public static final class Builder {

        private final Map<String, Integer> index;
        private final String[] items;

        private Builder(@Nonnull Map<String, Integer> index) {
            this.index = index;
            this.items = new String[index.size()];
        }

        @Nonnull
        public Builder put(@Nullable String id, @Nullable String value) {
            if (id != null) {
                Integer position = index.get(id);
                if (position != null) {
                    items[position] = value;
                }
            }
            return this;
        }

        @Nonnull
        public Builder clear() {
            Arrays.fill(items, null);
            return this;
        }

        @Nullable
        public String getItem(int index) throws IndexOutOfBoundsException {
            return items[index];
        }

        @Nonnull
        public Key build() {
            return Key.valueOf(items);
        }
    }
}
