package org.apache.felix.http.base.internal.handler.trie;

import static org.junit.Assert.assertTrue;

import java.util.Comparator;

import org.apache.felix.http.base.internal.handler.trie.PrefixComparator;
import org.apache.felix.http.base.internal.handler.trie.PrefixComparator.Path;
import org.junit.Test;

public class PrefixComparatorTest
{
    private final Comparator<Path> comparator = PrefixComparator.INSTANCE;

    @Test
    public void samePathEquals()
    {
        Path pathOne = new Path("/test", "/test", false, 0);
        Path pathTwo = new Path("/test", "/test", false, 0);

        assertEqual(pathOne, pathTwo);

        pathOne = new Path("/test", "/test", true, 0);
        pathTwo = new Path("/test", "/test", true, 0);

        assertEqual(pathOne, pathTwo);
    }

    @Test
    public void samePathWithHigherRankIsSmaller()
    {
        Path pathOne = new Path("/test", "/test", false, 1);
        Path pathTwo = new Path("/test", "/test", false, 0);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void pathWithPriorityIsSmallerForEqualPaths()
    {
        Path pathOne = new Path("/test", "/test", true, 0);
        Path pathTwo = new Path("/test", "/test", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void pathWithPriorityIsSmallerForDifferentContexts()
    {
        Path pathOne = new Path("", "/test/test", true, 0);
        Path pathTwo = new Path("/test/test", "", false, 1);

        assertSmaller(pathOne, pathTwo);

        pathOne = new Path("/test", "/test", true, 0);
        pathTwo = new Path("/test/test", "", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void pathWithLongerMatchingContextIsSmaller()
    {
        Path pathOne = new Path("/test", "/test/test", false, 0);
        Path pathTwo = new Path("/test/test", "/test", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void wildcardMatchIsLarger()
    {
        Path pathOne = new Path("/test", "/test/test", false, 0);
        Path pathTwo = new Path("/test", "/test/*", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void wildcardMatchIsSmallerThanParent()
    {
        Path pathOne = new Path("/test", "/test/*", false, 0);
        Path pathTwo = new Path("/test", "/test", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void shorterPathPrefixOfLongerIsLarger()
    {
        Path pathOne = new Path("/test", "/test/test", false, 0);
        Path pathTwo = new Path("/test", "/test", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void smallerContextPathIsSmaller()
    {
        Path pathOne = new Path("/a", "/test/test", false, 0);
        Path pathTwo = new Path("/b", "/test", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void smallerPathIsSmaller()
    {
        Path pathOne = new Path("/test", "/a", false, 0);
        Path pathTwo = new Path("/test", "/b", false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    @Test
    public void wildcardIsLargest()
    {
        Path pathOne = new Path("/test", "/" + Character.MAX_VALUE, false, 0);
        Path pathTwo = new Path("/test", "/*" , false, 1);

        assertSmaller(pathOne, pathTwo);
    }

    private void assertEqual(Path one, Path two)
    {
        assertTrue(comparator.compare(one, two) == 0);
        assertTrue(comparator.compare(two, one) == 0);
    }

    private void assertSmaller(Path one, Path two)
    {
        assertTrue(comparator.compare(one, two) < 0);
        assertTrue(comparator.compare(two, one) > 0);
    }
}
