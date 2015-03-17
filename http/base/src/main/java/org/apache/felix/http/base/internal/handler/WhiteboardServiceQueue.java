/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.base.internal.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


final class WhiteboardServiceQueue<K, V extends AbstractHandler<V>>
{
    private final Map<V, Integer> useCounts = new TreeMap<V, Integer>();
    private final Map<K, PriorityQueue<V>> keyMap;

    private int size = 0;

    WhiteboardServiceQueue()
    {
        keyMap = new HashMap<K, PriorityQueue<V>>();
    }

    WhiteboardServiceQueue(Comparator<K> keyComparator)
    {
        keyMap = new TreeMap<K, PriorityQueue<V>>(keyComparator);
    }

    boolean isActive(V handler)
    {
        return useCounts.containsKey(handler);
    }

    Update<K, V> add(Collection<K> keys, V handler)
    {
        Map<K, V> activate = new TreeMap<K, V>();
        Map<K, V> deactivate = new TreeMap<K, V>();
        Set<V> destroy = new TreeSet<V>();
        for (K key : keys)
        {
            PriorityQueue<V> queue = getQueue(key);

            if (queue.isEmpty() || queue.peek().compareTo(handler) > 0)
            {
                activateEntry(key, handler, activate, null);

                if (!queue.isEmpty())
                {
                    V currentHead = queue.peek();
                    deactivateEntry(key, currentHead, deactivate, destroy);
                }
            }

            queue.add(handler);
        }

        size += 1;

        return Update.forAdd(activate, deactivate, destroy);
    }

    Update<K, V> remove(Collection<K> keys, V handler)
    {
        Map<K, V> activate = new TreeMap<K, V>();
        Map<K, V> deactivate = new TreeMap<K, V>();
        Set<V> init = new TreeSet<V>();
        for (K key : keys)
        {
            PriorityQueue<V> queue = getQueue(key);

            boolean isDeactivate = !queue.isEmpty() && queue.peek().compareTo(handler) == 0;
            queue.remove(handler);

            if (isDeactivate)
            {
                deactivateEntry(key, handler, deactivate, null);

                if (!queue.isEmpty())
                {
                    V newHead = queue.peek();
                    activateEntry(key, newHead, activate, init);
                }
            }

            if (queue.isEmpty())
            {
                keyMap.remove(key);
            }
        }

        size -= 1;

        return Update.forRemove(activate, deactivate, init);
    }

    private PriorityQueue<V> getQueue(K key)
    {
        PriorityQueue<V> queue = keyMap.get(key);
        if (queue == null)
        {
            queue = new PriorityQueue<V>();
            keyMap.put(key, queue);
        }
        return queue;
    }

    private void activateEntry(K key, V handler, Map<K, V> activate, Set<V> init)
    {
        activate.put(key, handler);
        if (incrementUseCount(handler) == 1 && init != null)
        {
            init.add(handler);
        };
    }

    private void deactivateEntry(K key, V handler, Map<K, V> deactivate, Set<V> destroy)
    {
        deactivate.put(key, handler);
        if (decrementUseCount(handler) == 0 && destroy != null)
        {
            destroy.add(handler);
        }
    }

    private int incrementUseCount(V handler)
    {
        Integer currentCount = useCounts.get(handler);
        Integer newCount = currentCount == null ? 1 : currentCount + 1;

        useCounts.put(handler, newCount);

        return newCount;
    }

    private int decrementUseCount(V handler)
    {
        int currentCount = useCounts.get(handler);
        if (currentCount == 1)
        {
            useCounts.remove(handler);
            return 0;
        }

        int newCount = currentCount - 1;
        useCounts.put(handler, newCount);
        return newCount;
    }

    void clear()
    {
        keyMap.clear();
        useCounts.clear();
    }

    Collection<V> values()
    {
        return null;
    }

    int size()
    {
        return size;
    }

    static final class Update<K, V extends AbstractHandler<?>>
    {
        private final Map<K, V> activate;
        private final Map<K, V> deactivate;
        private final Collection<V> init;
        private final Collection<V> destroy;

        Update(Map<K, V> activate, Map<K, V> deactivate, Collection<V> init, Collection<V> destroy)
        {
            this.activate = activate;
            this.deactivate = deactivate;
            this.init = init;
            this.destroy = destroy;
        }

        private static <K, V extends AbstractHandler<?>> Update<K, V> forAdd(Map<K, V> activate,
                Map<K, V> deactivate,
                Collection<V> destroy)
        {
            // activate contains at most one value, mapped to multiple keys
            Collection<V> init = valueAsCollection(activate);
            return new Update<K, V>(activate, deactivate, init, destroy);
        }

        private static <K, V extends AbstractHandler<?>> Update<K, V> forRemove(Map<K, V> activate,
                Map<K, V> deactivate,
                Collection<V> init)
        {
            // deactivate contains at most one value, mapped to multiple keys
            Collection<V> destroy = valueAsCollection(deactivate);
            return new Update<K, V>(activate, deactivate, init, destroy);
        }

        private static <V extends AbstractHandler<?>, K> Collection<V> valueAsCollection(Map<K, V> valueMap)
        {
            if (valueMap.isEmpty())
            {
                return Collections.emptyList();
            }

            Collection<V> valueSet = new ArrayList<V>(1);
            valueSet.add(valueMap.values().iterator().next());
            return valueSet;
        }

        Map<K, V> getActivated()
        {
            return activate;
        }

        Map<K, V> getDeactivated()
        {
            return deactivate;
        }

        Collection<V> getInit()
        {
            return init;
        }

        Collection<V> getDestroy()
        {
            return destroy;
        }
    }
}
