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
package org.apache.felix.http.base.internal.handler.trie;

import static java.util.Collections.unmodifiableSortedSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;


public final class TrieNode<V extends Comparable<V>, C extends Comparable<C>> implements Comparable<TrieNode<V, C>>
{
    private final TreeSet<TrieNode<V, C>> children;
    private final SearchPath path;
    private final Collection<ColoredValue<V, C>> values;

    TrieNode(TreeSet<TrieNode<V, C>> children, SearchPath path, Collection<ColoredValue<V, C>> values)
    {
        this.children = children;
        this.path = path;
        this.values = values;
    }

    TrieNode(TreeSet<TrieNode<V, C>> children, SearchPath  path, V value, C color)
    {
        this(children, path, asList(new ColoredValue<V, C>(value, color)));
    }

    TrieNode(SearchPath path)
    {
        this(new TreeSet<TrieNode<V, C>>(), path, Collections.<ColoredValue<V, C>>emptyList());
    }

    TrieNode<V, C> addValue(V value, C color)
    {
        Collection<ColoredValue<V, C>> newValues = createValuesCollection();
        newValues.addAll(values);
        newValues.add(new ColoredValue<V, C>(value, color));
        return new TrieNode<V, C>(children, path, newValues);
    }

    TrieNode<V, C> removeValue(V value, C color)
    {
        ColoredValue<V, C> coloredValue = new ColoredValue<V, C>(value, color);
        if (!values.contains(coloredValue))
        {
            return this;
        }
        Collection<ColoredValue<V, C>> newValues = createValuesCollection();
        newValues.addAll(values);
        newValues.remove(coloredValue);
        return new TrieNode<V, C>(children, path, newValues);
    }

    public SearchPath getPath()
    {
        return path;
    }

    public Collection<V> getValues()
    {
        ArrayList<V> result = new ArrayList<V>();
        for (ColoredValue<V, C> coloredValue : values)
        {
            result.add(coloredValue.getValue());
        }
        return result;
    }

    Collection<ColoredValue<V, C>> copyOfValues()
    {
        Collection<ColoredValue<V, C>> result = createValuesCollection();
        result.addAll(values);
        return result;
    }

    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    public C getValueColor()
    {
        if (isEmpty())
        {
            return null;
        }
        return values.iterator().next().getColor();
    }

    boolean hasDominantColor()
    {
        return path.hasDominantColor();
    }

    public V firstValue()
    {
        if (isEmpty())
        {
            return null;
        }
        return values.iterator().next().getValue();
    }

    SortedSet<TrieNode<V, C>> getChildren()
    {
        return unmodifiableSortedSet(children);
    }

    SortedSet<TrieNode<V, C>> getChildren(SearchPath prefix)
    {
        TrieNode<V, C> startNode = new TrieNode<V, C>(prefix);
        TrieNode<V, C> endNode = new TrieNode<V, C>(prefix.childBound());
        return unmodifiableSortedSet(children.subSet(startNode, endNode));
    }

    TrieNode<V, C> getFloorChild(SearchPath path)
    {
        return children.floor(new TrieNode<V, C>(path));
    }

    public boolean isLeaf()
    {
        return children.isEmpty();
    }

    private Collection<ColoredValue<V, C>> createValuesCollection()
    {
        return new PriorityQueue<ColoredValue<V,C>>(1);
    }

    private static <V extends Comparable<V>, C extends Comparable<C>> Collection<ColoredValue<V, C>> asList(ColoredValue<V, C> coloredValue)
    {
        ArrayList<ColoredValue<V, C>> list = new ArrayList<ColoredValue<V,C>>(1);
        list.add(coloredValue);
        return list;
    }

    @Override
    public int compareTo(TrieNode<V, C> other)
    {
        int pathCompare;
        if (path == null)
        {
            pathCompare = other.path == null ? 0 : -1;
        }
        else
        {
            pathCompare = path.compareTo(other.path);
        }

        if (pathCompare != 0)
        {
            return pathCompare;
        }
        return 0;
    }

    @Override
    public int hashCode()
    {
        if (path == null)
        {
            return 0;
        }
        return path.hashCode();
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
        if (!(obj instanceof TrieNode))
        {
            return false;
        }

        @SuppressWarnings("rawtypes")
        TrieNode other = (TrieNode) obj;

        if (path == null)
        {
            return other.path == null;
        }
        else
        {
            return path.equals(other.path);
        }
    }

    @Override
    public String toString()
    {
        return String.valueOf(path);
    }

}