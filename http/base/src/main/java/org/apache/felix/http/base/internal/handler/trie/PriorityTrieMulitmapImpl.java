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

import static org.apache.felix.http.base.internal.util.CompareUtil.compareSafely;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public final class PriorityTrieMulitmapImpl<V extends Comparable<V>, C extends Comparable<C>> implements Iterable<TrieNode<V, C>>, PriorityTrieMultimap<V, C>
{
    private final TrieNode<V, C> root;
    private final ConcurrentMap<TrieNode<V, C>, C> nodeColoring;

    public PriorityTrieMulitmapImpl()
    {
        this(new TrieNode<V, C>(null));
    }

    private PriorityTrieMulitmapImpl(TrieNode<V, C> root)
    {
        this(root, new ConcurrentHashMap<TrieNode<V,C>, C>());
    }

    /**
     * Only for use in subtries!
     */
    private PriorityTrieMulitmapImpl(TrieNode<V, C> root, ConcurrentMap<TrieNode<V, C>, C> coloring)
    {
        checkNotNull(root);
        checkNotNull(coloring);

        this.root = root;
        this.nodeColoring = coloring;
    }

    public PriorityTrieMulitmapImpl<V, C> add(SearchPath path, V value)
    {
        checkNotNull(path);
        return add(path, value, null);
    }

    @Override
    public PriorityTrieMulitmapImpl<V, C> add(SearchPath path, V value, C color)
    {
        checkNotNull(path);

        List<TrieNode<V, C>> pathToParent = findParents(path);

        TrieNode<V, C> parent = pathToParent.get(0);
        if (color == null && parent.equals(root))
        {
            throw new IllegalArgumentException("Root nodes must be colored");
        }
        else if (color == null)
        {
            color = getColor(parent);
        }

        if (path.equals(parent.getPath()))
        {
            return updateNodeAddValue(pathToParent, value, color);
        }
        return addNewNode(pathToParent, path, value, color);
    }

    private PriorityTrieMulitmapImpl<V, C> updateNodeAddValue(List<TrieNode<V, C>> pathToNode, V value, C color)
    {
        TrieNode<V, C> node = pathToNode.get(0);

        TrieNode<V, C> newNode = node.addValue(value, color);
        TrieNode<V, C> newRoot = updateParents(pathToNode, newNode);

        return new PriorityTrieMulitmapImpl<V, C>(newRoot);
    }

    private PriorityTrieMulitmapImpl<V, C> addNewNode(List<TrieNode<V, C>> pathToParent, SearchPath path, V value, C color)
    {
        TrieNode<V, C> parent = pathToParent.get(0);
        TrieNode<V, C> newParent = addNodeToParent(parent, path, value, color);
        TrieNode<V, C> newRoot = updateParents(pathToParent, newParent);

        return new PriorityTrieMulitmapImpl<V, C>(newRoot);
    }

    private TrieNode<V, C> addNodeToParent(TrieNode<V, C> parent, SearchPath path, V value, C color)
    {
        TreeSet<TrieNode<V, C>> presentChildren = new TreeSet<TrieNode<V, C>>(parent.getChildren(path));
        TreeSet<TrieNode<V, C>> siblings = new TreeSet<TrieNode<V, C>>(parent.getChildren());
        siblings.removeAll(presentChildren);

        TrieNode<V, C> newNode = new TrieNode<V, C>(presentChildren, path, value, color);

        siblings.add(newNode);
        TrieNode<V, C> newParent = new TrieNode<V, C>(siblings, parent.getPath(), parent.copyOfValues());

        return newParent;
    }

    @Override
    public PriorityTrieMulitmapImpl<V, C> remove(SearchPath path, V value, C color)
    {
        checkNotNull(path);

        List<TrieNode<V, C>> pathToNode = findParents(path);
        TrieNode<V, C> node = pathToNode.get(0);

        if (!path.equals(node.getPath()))
        {
            return this;
        }

        if (color == null)
        {
            color = getColor(node);
        }

        TrieNode<V, C> newNode = node.removeValue(value, color);
        if (newNode == node)
        {
            return this;
        }

        if (newNode.isEmpty())
        {
            return removeEmptyNode(pathToNode);
        }
        TrieNode<V, C> newRoot = updateParents(pathToNode, newNode);
        return new PriorityTrieMulitmapImpl<V, C>(newRoot);
    }

    private PriorityTrieMulitmapImpl<V, C> removeEmptyNode(List<TrieNode<V, C>> pathToNode)
    {
        TrieNode<V, C> node = pathToNode.get(0);
        TrieNode<V, C> parent = pathToNode.get(1);

        TrieNode<V, C> newParent = removeNode(parent, node);
        TrieNode<V, C> newRoot = updateParents(pathToNode.subList(1, pathToNode.size()), newParent);

        return new PriorityTrieMulitmapImpl<V, C>(newRoot);
    }

    private TrieNode<V, C> removeNode(TrieNode<V, C> parent, TrieNode<V, C> node)
    {
        TreeSet<TrieNode<V, C>> newChildren = new TreeSet<TrieNode<V, C>>(parent.getChildren());
        newChildren.remove(node);
        newChildren.addAll(node.getChildren());

        return new TrieNode<V, C>(newChildren, parent.getPath(), parent.copyOfValues());
    }

    private TrieNode<V, C> updateParents(List<TrieNode<V, C>> pathToParent, TrieNode<V, C> newNode)
    {
        TrieNode<V, C> currentChild = pathToParent.get(0);
        TrieNode<V, C> newChild = newNode;
        for (TrieNode<V, C> parent : pathToParent.subList(1, pathToParent.size()))
        {
            TreeSet<TrieNode<V, C>> children = new TreeSet<TrieNode<V, C>>(parent.getChildren());
            children.remove(currentChild);
            children.add(newChild);

            currentChild = parent;
            newChild = new TrieNode<V, C>(children, parent.getPath(), parent.copyOfValues());
        }
        return newChild;
    }

    @Override
    public TrieNode<V, C> getPrefix(SearchPath path)
    {
        List<TrieNode<V, C>> parents = findParents(path);
        return parents.isEmpty() ? null : parents.get(0);
    }

    @Override
    public TrieNode<V, C> search(SearchPath path)
    {
        checkNotNull(path);

        List<TrieNode<V, C>> pathToParent = findParents(path);
        for(TrieNode<V, C> node : pathToParent)
        {
            if (isActive(node) && node.getPath().matches(path))
            {
                return node;
            }
        }

        return null;
    }

    List<TrieNode<V, C>> findParents(SearchPath path)
    {
        checkNotNull(path);

        List<TrieNode<V, C>> pathToParent = new ArrayList<TrieNode<V, C>>();
        TrieNode<V, C> current = root;

        while (isParent(current, path))
        {
            pathToParent.add(current);
            current = current.getFloorChild(path);
        }

        Collections.reverse(pathToParent);
        if (!nodeColoring.containsKey(pathToParent.get(0)))
        {
            calculateColors(pathToParent);
        }

        return pathToParent;
    }

    private boolean isParent(TrieNode<V, C> current, SearchPath path)
    {
        if (current == null)
        {
            return false;
        }
        return isBareRoot(current) || current.getPath().isParentOf(path);
    }

    @Override
    public PriorityTrieMulitmapImpl<V, C> getSubtrie(SearchPath path)
    {
        checkNotNull(path);

        TrieNode<V, C> subtrieRoot = getPrefix(path);
        cacheColors(subtrieRoot);

        if (!path.equals(subtrieRoot.getPath()))
        {
            TreeSet<TrieNode<V, C>> matchingChildren = new TreeSet<TrieNode<V, C>>(subtrieRoot.getChildren(path));
            subtrieRoot = new TrieNode<V, C>(matchingChildren, null, Collections.<ColoredValue<V, C>>emptyList());
        }

        return new PriorityTrieMulitmapImpl<V, C>(subtrieRoot, nodeColoring);
    }

    private void cacheColors(TrieNode<V, C> subtrieRoot)
    {
        Iterator<TrieNode<V, C>> iterator = createDepthFirstIterator(subtrieRoot);
        while (iterator.hasNext())
        {
            iterator.next();
        }
    }

    @Override
    public C getColor(TrieNode<V, C> node)
    {
        if (node == null || isBareRoot(node))
        {
            return null;
        }

        if (node.equals(root))
        {
            return root.getValueColor();
        }

        if (!nodeColoring.containsKey(node))
        {
            //cache node coloring
            findParents(node.getPath());
        }
        return nodeColoring.get(node);
    }

    private void calculateColors(List<TrieNode<V, C>> nodes)
    {
        TrieNode<V, C> root = nodes.get(nodes.size() - 1);
        C parentColor = calculateColor(root, getColor(root));
        for (int i = nodes.size() - 2; i >= 0; i--)
        {
            TrieNode<V, C> currentNode = nodes.get(i);
            C color = nodeColoring.get(currentNode);
            if (color == null)
            {
                color = calculateColor(currentNode, parentColor);
                nodeColoring.put(currentNode, color);
            }

            if (currentNode.hasDominantColor() || isBareRoot(currentNode))
            {
                parentColor = color;
            }
        }
    }

    private C calculateColor(TrieNode<V, C> node, C cachedColor)
    {
        C valueColor = node.getValueColor();
        if (cachedColor == null){
            return valueColor;
        }

        if (valueColor == null){
            return cachedColor;
        }

        return min(valueColor, cachedColor);
    }

    private C min(C color1, C color2)
    {
        return compareSafely(color1, color2) <= 0 ? color1 : color2;
    }

    private <T> T checkNotNull(T value)
    {
        if (value == null)
        {
            throw new NullPointerException("Argument must not be null");
        }
        return value;
    }

    @Override
    public Collection<V> activeValues()
    {
        List<V> values = new ArrayList<V>();
        Iterator<TrieNode<V, C>> iterator = iterator();
        while (iterator.hasNext())
        {
            values.add(iterator.next().firstValue());
        }
        return values;
    }

    @Override
    public Iterator<TrieNode<V, C>> iterator()
    {
        return createDepthFirstIterator(root);
    }

    private Iterator<TrieNode<V,C>> createDepthFirstIterator(final TrieNode<V, C> root)
    {
        return new Iterator<TrieNode<V,C>>()
        {
            private final Deque<TrieNode<V, C>> queue = new ArrayDeque<TrieNode<V,C>>();
            private final Set<TrieNode<V, C>> visited = new HashSet<TrieNode<V,C>>();
            private TrieNode<V, C> next;

            {
                queue.add(root);
                next = findNext();
            }

            @Override
            public boolean hasNext()
            {
                return next != null;
            }

            @Override
            public TrieNode<V, C> next()
            {
                TrieNode<V, C> returnValue = next;
                next = findNext();
                return returnValue;
            }

            private TrieNode<V, C> findNext()
            {
                TrieNode<V, C> nextNode = findNextNode();
                while (nextNode != null && !isActive(nextNode))
                {
                    nextNode = findNextNode();
                }
                return nextNode;
            }

            private TrieNode<V,C> findNextNode()
            {
                if (queue.isEmpty())
                {
                    return null;
                }

                TrieNode<V, C> head = queue.peekFirst();
                while (!head.isLeaf() && !visited.contains(head))
                {
                    Set<TrieNode<V, C>> children = head.getChildren();
                    for (TrieNode<V, C> node : children)
                    {
                        queue.addFirst(node);
                    }
                    visited.add(head);
                    head = queue.peekFirst();
                }
                return queue.pop();
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    private boolean isActive(TrieNode<V, C> node)
    {
        return compareSafely(node.getValueColor(), getColor(node)) <= 0 && !isBareRoot(node);
    }

    private boolean isBareRoot(TrieNode<V, C> current)
    {
        return current.getPath() == null;
    }

    /**
     * Print a representation of the {@code PriorityTrie} displaying the path names for debugging.
     */
    @Override
    public String toString()
    {
        Set<TrieNode<V, C>> visited = new HashSet<TrieNode<V, C>>();
        StringBuilder result = new StringBuilder();
        Deque<TrieNode<V, C>> queue = new ArrayDeque<TrieNode<V, C>>();
        queue.add(root);
        int indent = 0;
        while(!queue.isEmpty())
        {
            TrieNode<V, C> current = queue.removeLast();
            if (!visited.contains(current))
            {
                for (int i = 0; i < indent; i++)
                {
                    result.append("  ");
                }
                result.append("|--");
                result.append(current);
                result.append(" [size: " + current.getValues().size() + " node:" + getColor(current) + ", value:" + current.getValueColor() + "]");
                result.append("\n");

                if (!current.getChildren().isEmpty())
                {
                    queue.add(current);
                    indent++;
                }
                queue.addAll(current.getChildren());
                visited.add(current);
            }
            else
            {
                indent--;
            }
        }
        return result.toString();
    }
}
