package org.apache.felix.http.base.internal.handler.trie;



final class CompareUtil
{
    static <V extends Comparable<V>> int compareSafely(V value, V other)
    {
        if (value == null && other == null)
        {
            return 0;
        }

        if (value == null || other == null)
        {
            return value == null ? -1 : 1;
        }

        return value.compareTo(other);
    }

    static <V  extends Comparable<V>> V min(V valueOne, V valueTwo)
    {
        return compareSafely(valueOne, valueTwo) >= 0 ? valueOne : valueTwo;
    }
}
