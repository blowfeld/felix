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

    @Override
    public int hashCode()
    {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (!(obj instanceof ColoredValue))
        {
            return false;
        }

        try
        {
            @SuppressWarnings("unchecked")
            ColoredValue<V, C> other = (ColoredValue<V, C>) obj;

            return value.equals(other.value) && compareTo(other) == 0;
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }
}
