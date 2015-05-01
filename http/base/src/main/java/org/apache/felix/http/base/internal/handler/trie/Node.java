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

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSortedSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;


public final class Node<V extends Comparable<V>, C extends Comparable<C>> implements Comparable<Node<V, C>>
{
    private final TreeSet<Node<V, C>> children;
    private final SearchPath path;
    private final Collection<ColoredValue<V, C>> values;

    Node(TreeSet<Node<V, C>> children, SearchPath path, Collection<ColoredValue<V, C>> values)
    {
        this.children = children;
        this.path = path;
        this.values = values;
    }

    Node(TreeSet<Node<V, C>> children, SearchPath  path, V value, C color)
    {
        this(children, path, asList(new ColoredValue<V, C>(value, color)));
    }

    Node(SearchPath path)
    {
        this(new TreeSet<Node<V, C>>(), path, Collections.<ColoredValue<V, C>>emptyList());
    }

    Node<V, C> addValue(V value, C color)
    {
        Collection<ColoredValue<V, C>> newValues = createValues();
        newValues.addAll(values);
        newValues.add(new ColoredValue<V, C>(value, color));
        return new Node<V, C>(children, path, newValues);
    }

    Node<V, C> removeValue(V value, C color)
    {
        ColoredValue<V, C> coloredValue = new ColoredValue<V, C>(value, color);
        if (!values.contains(coloredValue))
        {
            return this;
        }
        Collection<ColoredValue<V, C>> newValues = createValues();
        newValues.addAll(values);
        newValues.remove(coloredValue);
        return new Node<V, C>(children, path, newValues);
    }

    public SearchPath getPath()
    {
        return path;
    }

    public Collection<ColoredValue<V, C>> getValues()
    {
        return unmodifiableCollection(values);
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

    SortedSet<Node<V, C>> getChildren()
    {
        return unmodifiableSortedSet(children);
    }

    SortedSet<Node<V, C>> getChildren(SearchPath prefix)
    {
        Node<V, C> startNode = new Node<V, C>(prefix);
        Node<V, C> endNode = new Node<V, C>(prefix.upperBound());
        return unmodifiableSortedSet(children.subSet(startNode, endNode));
    }

    Node<V, C> getFloorChild(SearchPath path)
    {
        return children.floor(new Node<V, C>(path));
    }

    public boolean isLeaf()
    {
        return children.isEmpty();
    }

    private Collection<ColoredValue<V, C>> createValues()
    {
        return new TreeSet<ColoredValue<V,C>>();
    }

    private static <V extends Comparable<V>, C extends Comparable<C>> Collection<ColoredValue<V, C>> asList(ColoredValue<V, C> coloredValue)
    {
        ArrayList<ColoredValue<V, C>> list = new ArrayList<ColoredValue<V,C>>(1);
        list.add(coloredValue);
        return list;
    }

    @Override
    public int compareTo(Node<V, C> other)
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
        if (!(obj instanceof Node))
        {
            return false;
        }

        @SuppressWarnings("rawtypes")
        Node other = (Node) obj;

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