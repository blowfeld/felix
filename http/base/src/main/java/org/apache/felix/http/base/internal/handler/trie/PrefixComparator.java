package org.apache.felix.http.base.internal.handler.trie;

import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.EQUAL;
import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.LARGER;
import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.PREFIX;
import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.PREFIXED;
import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.SMALLER;
import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.WILDCARD_1;
import static org.apache.felix.http.base.internal.handler.trie.PrefixComparator.PrefixOrder.WILDCARD_2;

import java.util.Comparator;

import org.apache.felix.http.base.internal.handler.trie.PrefixComparator.Path;

public enum PrefixComparator implements Comparator<Path>
{
    INSTANCE;

    private static final String WILDCARD_POSTFIX = "*";

    enum PrefixOrder
    {
        SMALLER, PREFIXED, WILDCARD_2, EQUAL, WILDCARD_1, PREFIX, LARGER;

        int getComparisonValue()
        {
            return this.ordinal() - 3;
        }
    }

    @Override
    public int compare(Path o1, Path o2)
    {
        int pathComparison = comparePath(o1, o2);
        if (pathComparison != 0)
        {
            return pathComparison;
        }
        return Long.compare(o2.rank, o1.rank);
    }

    private int comparePath(Path o1, Path o2)
    {
        String o1CompletePath = o1.getCompletePath();
        String o2CompletePath = o2.getCompletePath();

        PrefixOrder pathCompare = comparePrefix(o1CompletePath, o2CompletePath);
        boolean priorityContext = o1.isPriority && o2.isPriority;
        if (pathCompare.getComparisonValue() != 0 || priorityContext)
        {
            return pathCompare.getComparisonValue();
        }

        if (o1.isPriority)
        {
            return -1;
        }

        if (o2.isPriority)
        {
            return 1;
        }

        if (o1.contextPath.length() == o2.contextPath.length())
        {
            return pathCompare.getComparisonValue();
        }

        return Integer.compare(o1.contextPath.length(), o2.contextPath.length());
    }

    private PrefixOrder comparePrefix(String o1, String o2)
    {
        while (o1.length() != 0 || o2.length() != 0)
        {
            if (WILDCARD_POSTFIX.equals(o1) && WILDCARD_POSTFIX.equals(o2))
            {
                return EQUAL;
            }

            if (WILDCARD_POSTFIX.equals(o1))
            {
                return WILDCARD_1;
            }

            if (WILDCARD_POSTFIX.equals(o2))
            {
                return WILDCARD_2;
            }

            if (o1.length() == 0)
            {
                return PREFIX;
            }

            if (o2.length() == 0)
            {
                return PREFIXED;
            }

            char o1FirstChar = o1.charAt(0);
            char o2FirstChar = o2.charAt(0);
            if (o1FirstChar != o2FirstChar)
            {
                return Character.compare(o1FirstChar, o2FirstChar) > 0 ? LARGER : SMALLER;
            }
            o1 = o1.substring(1);
            o2 = o2.substring(1);
        }

        return EQUAL;
    }

    static class Path
    {
        String contextPath;
        String path;
        boolean isPriority;
        long rank;

        Path(String contextPath, String path, boolean isPriority, long rank)
        {
            this.contextPath = contextPath;
            this.path = path;
            this.isPriority = isPriority;
            this.rank = rank;
        }

        String getCompletePath()
        {
            return contextPath + path;
        }
    }
}
