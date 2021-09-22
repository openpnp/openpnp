package org.openpnp.util;

public class PairKey<U, V> {

    private final U first;
    private final V second;

    public PairKey(U first, V second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PairKey<?, ?> pair = (PairKey<?, ?>) o;

        if (!first.equals(pair.first)) {
            return false;
        }
        return second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }

}
