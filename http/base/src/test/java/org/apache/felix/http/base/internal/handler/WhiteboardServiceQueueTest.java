/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.handler;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.WhiteboardServiceQueue.Update;
import org.junit.Before;
import org.junit.Test;

public class WhiteboardServiceQueueTest
{
    private WhiteboardServiceQueue<String, TestHandler> serviceQueue;

    @Before
    public void setup()
    {
        serviceQueue = new WhiteboardServiceQueue<String, TestHandler>();
    }

    @Test
    public void addedInfoIsUsed() throws ServletException
    {
        TestHandler info = TestHandler.create(asList("a"), 0);
        Update<String, TestHandler> update = serviceQueue.add(info.getKeys(), info);

        assertEquals(1, serviceQueue.size());
        assertTrue(serviceQueue.isActive(info));

        assertEquals(1, update.getActivated().size());
        assertThat(update.getActivated(), hasEntry("a", info));
        assertTrue(update.getDeactivated().isEmpty());

        assertEquals(1, update.getInit().size());
        assertThat(update.getInit(), contains(info));
        assertTrue(update.getDestroy().isEmpty());
    }

    @Test
    public void highestPriorityServiceIsUsed() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a"), 1);
        TestHandler lower = TestHandler.create(asList("a"), 0);

        serviceQueue.add(lower.getKeys(), lower);

        Update<String, TestHandler> updateAddingHigher = serviceQueue.add(higher.getKeys(), higher);

        assertTrue(serviceQueue.isActive(higher));
        assertFalse(serviceQueue.isActive(lower));

        assertEquals(1, updateAddingHigher.getActivated().size());
        assertThat(updateAddingHigher.getActivated(), hasEntry("a", higher));
        assertEquals(1, updateAddingHigher.getDeactivated().size());
        assertThat(updateAddingHigher.getDeactivated(), hasEntry("a", lower));

        assertEquals(1, updateAddingHigher.getInit().size());
        assertThat(updateAddingHigher.getInit(), contains(higher));
        assertEquals(1, updateAddingHigher.getDestroy().size());
        assertThat(updateAddingHigher.getDestroy(), contains(lower));
    }

    @Test
    public void removeHighestPriorityService() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a"), 1);
        TestHandler lower = TestHandler.create(asList("a"), 0);

        serviceQueue.add(lower.getKeys(), lower);
        serviceQueue.add(higher.getKeys(), higher);

        Update<String, TestHandler> update = serviceQueue.remove(higher.getKeys(), higher);

        assertFalse(serviceQueue.isActive(higher));
        assertTrue(serviceQueue.isActive(lower));

        assertEquals(1, update.getActivated().size());
        assertThat(update.getActivated(), hasEntry("a", lower));
        assertEquals(1, update.getDeactivated().size());
        assertThat(update.getDeactivated(), hasEntry("a", higher));

        assertEquals(1, update.getInit().size());
        assertThat(update.getInit(), contains(lower));
        assertEquals(1, update.getDestroy().size());
        assertThat(update.getDestroy(), contains(higher));
    }

    @Test
    public void removeLowerPriorityService() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a"), 1);
        TestHandler lower = TestHandler.create(asList("a"), 0);

        serviceQueue.add(lower.getKeys(), higher);
        serviceQueue.add(higher.getKeys(), lower);

        Update<String, TestHandler> update = serviceQueue.remove(lower.getKeys(), lower);

        assertTrue(serviceQueue.isActive(higher));
        assertFalse(serviceQueue.isActive(lower));

        assertTrue(update.getActivated().isEmpty());
        assertTrue(update.getDeactivated().isEmpty());

        assertTrue(update.getInit().isEmpty());
        assertTrue(update.getDestroy().isEmpty());
    }

    @Test
    public void addServiceWithMultipleKeys() throws ServletException
    {
        TestHandler info = TestHandler.create(asList("a", "b"), 0);

        Update<String, TestHandler> update = serviceQueue.add(info.getKeys(), info);

        assertTrue(serviceQueue.isActive(info));

        assertEquals(2, update.getActivated().size());
        assertThat(update.getActivated(), hasEntry("a", info));
        assertThat(update.getActivated(), hasEntry("b", info));
        assertTrue(update.getDeactivated().isEmpty());

        assertEquals(1, update.getInit().size());
        assertThat(update.getInit(), contains(info));
    }

    @Test
    public void addServiceWithMultipleKeysShadowsAllKeys() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler lower = TestHandler.create(asList("a", "b"), 0);

        serviceQueue.add(lower.getKeys(), lower);

        Update<String, TestHandler> updateWithHigher = serviceQueue.add(higher.getKeys(), higher);

        assertTrue(serviceQueue.isActive(higher));
        assertFalse(serviceQueue.isActive(lower));

        assertEquals(3, updateWithHigher.getActivated().size());
        assertThat(updateWithHigher.getActivated(), hasEntry("a", higher));
        assertThat(updateWithHigher.getActivated(), hasEntry("b", higher));
        assertThat(updateWithHigher.getActivated(), hasEntry("c", higher));
        assertEquals(2, updateWithHigher.getDeactivated().size());
        assertThat(updateWithHigher.getDeactivated(), hasEntry("a", lower));
        assertThat(updateWithHigher.getDeactivated(), hasEntry("b", lower));

        assertEquals(1, updateWithHigher.getInit().size());
        assertThat(updateWithHigher.getInit(), contains(higher));
        assertEquals(1, updateWithHigher.getDestroy().size());
        assertThat(updateWithHigher.getDestroy(), contains(lower));
    }

    @Test
    public void addServiceWithMultipleKeysShadowsPartially() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a", "c"), 1);
        TestHandler lower = TestHandler.create(asList("a", "b"), 0);

        serviceQueue.add(lower.getKeys(), lower);

        Update<String, TestHandler> updateWithHigher = serviceQueue.add(higher.getKeys(), higher);

        assertTrue(serviceQueue.isActive(higher));
        assertTrue(serviceQueue.isActive(lower));

        assertEquals(2, updateWithHigher.getActivated().size());
        assertThat(updateWithHigher.getActivated(), hasEntry("a", higher));
        assertThat(updateWithHigher.getActivated(), hasEntry("c", higher));
        assertEquals(1, updateWithHigher.getDeactivated().size());
        assertThat(updateWithHigher.getDeactivated(), hasEntry("a", lower));

        assertEquals(1, updateWithHigher.getInit().size());
        assertThat(updateWithHigher.getInit(), contains(higher));
        assertTrue(updateWithHigher.getDestroy().isEmpty());
    }

    @Test
    public void addServiceWithMultipleKeysIsCompletelyShadowed() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler lower = TestHandler.create(asList("a", "b"), 0);

        serviceQueue.add(higher.getKeys(), higher);

        Update<String, TestHandler> updateWithLower = serviceQueue.add(lower.getKeys(), lower);

        assertTrue(serviceQueue.isActive(higher));
        assertFalse(serviceQueue.isActive(lower));

        assertTrue(updateWithLower.getActivated().isEmpty());
        assertTrue(updateWithLower.getDeactivated().isEmpty());

        assertTrue(updateWithLower.getInit().isEmpty());
        assertTrue(updateWithLower.getDestroy().isEmpty());
    }

    @Test
    public void sizeReturnsAllEntries() throws ServletException
    {
        TestHandler higher = TestHandler.create(asList("a", "b", "c"), 1);
        TestHandler lower = TestHandler.create(asList("a", "b"), 0);
        TestHandler third = TestHandler.create(asList("d"), 3);

        assertEquals(0 , serviceQueue.size());

        serviceQueue.add(lower.getKeys(), lower);

        assertEquals(1, serviceQueue.size());

        serviceQueue.add(higher.getKeys(), higher);

        assertEquals(2, serviceQueue.size());

        serviceQueue.add(third.getKeys(), third);

        assertEquals(3, serviceQueue.size());

        serviceQueue.remove(lower.getKeys(), lower);

        assertEquals(2, serviceQueue.size());

        serviceQueue.remove(higher.getKeys(), higher);

        assertEquals(1, serviceQueue.size());

        serviceQueue.remove(third.getKeys(), third);

        assertEquals(0, serviceQueue.size());
    }

    private static abstract class TestHandler extends AbstractHandler<TestHandler>
    {
        static int idCount = 0;

        TestHandler(List<String> keys, int ranking)
        {
            super(null, null, null);
        }

        static TestHandler create(List<String> keys, int ranking)
        {
            TestHandler testHandler = mock(TestHandler.class);
            when(testHandler.getId()).thenReturn(++idCount);
            when(testHandler.getRanking()).thenReturn(ranking);
            when(testHandler.getKeys()).thenReturn(keys);
            when(testHandler.compareTo(any(TestHandler.class))).thenCallRealMethod();
            return testHandler;
        }

        @Override
        public int compareTo(TestHandler o)
        {
            int rankCompare = Integer.compare(o.getRanking(), getRanking());
            return rankCompare != 0 ? rankCompare : Integer.compare(getId(), o.getId());
        }

        abstract int getRanking();

        abstract int getId();

        abstract List<String> getKeys();
    }
}
