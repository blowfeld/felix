package org.apache.felix.http.base.internal.runtime;

import static java.lang.Integer.signum;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AbstractInfoOrderingTest
{
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Expected value must be non-negative
                // negative values are tested by symmetry

                // same service id (note: rank must be identical)
                { 0, 0, 0, 1, 1 },
                { 0, 1, 1, 0, 0 },
                { 0, -1, -1, -1, -1 },
                // rank has priority
                { 1, 1, 0, 1, 0 },
                { 1, 1, 0, -1, 0 },
                { 1, 0, -1, 1, 0 },
                { 1, 0, -1, -1, 0 },
                // same rank
                { 1, 1, 1, 1, 2 },
                { 1, -1, -1, 1, -1 },
                { 1, 0, 0, 1, 2 },
                { 1, 0, 0, 0, 1 },
                { 1, 0, 0, 0, -1 },
                { 1, 0, 0, 1, -1 },
                { 1, 0, 0, 1, -2 },
                { 1, 0, 0, 2, -1 },
                { 1, 0, 0, 1, -1 },
                { 1, 0, 0, -1, -2 }
           });
    }

    private final int expected;
    private final TestInfo testInfo;
    private final TestInfo other;

    public AbstractInfoOrderingTest(int expected,
            int testRank, int otherRank, long testId, long otherId)
    {
        if (expected < 0)
        {
            throw new IllegalArgumentException("Expected values must be non-negative.");
        }
        this.expected = expected;
        testInfo = new TestInfo(testRank, testId);
        other = new TestInfo(otherRank, otherId);
    }

    @Test
    public void ordering()
    {
        assertEquals(expected, signum(testInfo.compareTo(other)));
    }

    @Test
    public void orderingSymetry()
    {
        assertTrue(signum(testInfo.compareTo(other)) == -signum(other.compareTo(testInfo)));
    }

    @Test
    public void orderingTransitivity()
    {
        assertTrue(testInfo.compareTo(other) >= 0);

        TestInfo three = new TestInfo(0, 0);

        // three falls in between the two other points
        if (testInfo.compareTo(three) >= 0 && three.compareTo(other) >= 0)
        {
            assertTrue(testInfo.compareTo(other) >= 0);
        }
        // three falls below the two other points
        else if (testInfo.compareTo(other) >= 0 && other.compareTo(three) >= 0)
        {
            assertTrue(testInfo.compareTo(three) >= 0);
        }
        // three falls above the two other points
        else if (three.compareTo(testInfo) >= 0 && testInfo.compareTo(other) >= 0)
        {
            assertTrue(three.compareTo(other) >= 0);
        }
        else
        {
            fail("Since testInfo >= other, one of the above cases must match");
        }
    }

    private static class TestInfo extends AbstractInfo<TestInfo>
    {
        public TestInfo(int ranking, long serviceId)
        {
            super(ranking, serviceId);
        }
    }
}
