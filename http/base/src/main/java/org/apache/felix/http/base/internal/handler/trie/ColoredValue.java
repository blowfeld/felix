package org.apache.felix.http.base.internal.handler.trie;

import static org.apache.felix.http.base.internal.handler.trie.CompareUtil.compareSafely;

final class ColoredValue<V, C extends Comparable<C>> implements Comparable<ColoredValue<V, C>>
{
    private final C color;
    private final V value;

    ColoredValue(V value, C color)
    {
        this.color = color;
        this.value = value;
    }

    C getColor()
    {
        return color;
    }

    V getValue()
    {
        return value;
    }

    @Override
    public int compareTo(ColoredValue<V, C> other)
    {
        return compareSafely(color, other.color);
    }
}
