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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public final class PriorityTrie<V extends Comparable<V>, C extends Comparable<C>> implements Iterable<Node<V, C>>, PriorityTree<V, C>
{
    private final Node<V, C> root;
    private final Map<Node<V, C>, C> nodeColoring;

    public PriorityTrie()
    {
        this(new Node<V, C>(null));
    }

    private PriorityTrie(Node<V, C> root)
    {
        this(root, new HashMap<Node<V,C>, C>());
    }

    /**
     * Only for use in subtries!
     */
    private PriorityTrie(Node<V, C> root, Map<Node<V, C>, C> coloring)
    {
        checkNotNull(root);
        checkNotNull(coloring);

        this.root = root;
        this.nodeColoring = coloring;
    }

    public PriorityTrie<V, C> add(SearchPath path, V value)
    {
        checkNotNull(path);
        return add(path, value, null);
    }

    public PriorityTrie<V, C> add(SearchPath path, V value, C color)
    {
        checkNotNull(path);

        List<Node<V, C>> pathToParent = findParents(path);

        Node<V, C> parent = pathToParent.get(0);
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

    private PriorityTrie<V, C> updateNodeAddValue(List<Node<V, C>> pathToNode, V value, C color)
    {
        Node<V, C> node = pathToNode.get(0);

        Node<V, C> newNode = node.addValue(value, color);
        Node<V, C> newRoot = updateParents(pathToNode, newNode);

        return new PriorityTrie<V, C>(newRoot);
    }

    private PriorityTrie<V, C> addNewNode(List<Node<V, C>> pathToParent, SearchPath path, V value, C color)
    {
        Node<V, C> parent = pathToParent.get(0);
        Node<V, C> newParent = addNodeToParent(parent, path, value, color);
        Node<V, C> newRoot = updateParents(pathToParent, newParent);

        return new PriorityTrie<V, C>(newRoot);
    }

    private Node<V, C> addNodeToParent(Node<V, C> parent, SearchPath path, V value, C color)
    {
        TreeSet<Node<V, C>> presentChildren = new TreeSet<Node<V, C>>(parent.getChildren(path));
        TreeSet<Node<V, C>> siblings = new TreeSet<Node<V, C>>(parent.getChildren());
        siblings.removeAll(presentChildren);

        Node<V, C> newNode = new Node<V, C>(presentChildren, path, value, color);

        siblings.add(newNode);
        Node<V, C> newParent = new Node<V, C>(siblings, parent.getPath(), parent.getValues());

        return newParent;
    }

    public PriorityTrie<V, C> remove(SearchPath path, V value, C color)
    {
        checkNotNull(path);

        List<Node<V, C>> pathToNode = findParents(path);
        Node<V, C> node = pathToNode.get(0);

        if (!path.equals(node.getPath()))
        {
            return this;
        }

        if (color == null)
        {
            color = getColor(node);
        }

        Node<V, C> newNode = node.removeValue(value, color);
        if (newNode == node)
        {
            return this;
        }

        if (newNode.isEmpty())
        {
            return removeEmptyNode(pathToNode);
        }
        Node<V, C> newRoot = updateParents(pathToNode, newNode);
        return new PriorityTrie<V, C>(newRoot);
    }

    private PriorityTrie<V, C> removeEmptyNode(List<Node<V, C>> pathToNode)
    {
        Node<V, C> node = pathToNode.get(0);
        Node<V, C> parent = pathToNode.get(1);

        Node<V, C> newParent = removeNode(parent, node);
        Node<V, C> newRoot = updateParents(pathToNode.subList(1, pathToNode.size()), newParent);

        return new PriorityTrie<V, C>(newRoot);
    }

    private Node<V, C> removeNode(Node<V, C> parent, Node<V, C> node)
    {
        TreeSet<Node<V, C>> newChildren = new TreeSet<Node<V, C>>(parent.getChildren());
        newChildren.remove(node);
        newChildren.addAll(node.getChildren());

        return new Node<V, C>(newChildren, parent.getPath(), parent.getValues());
    }

    private Node<V, C> updateParents(List<Node<V, C>> pathToParent, Node<V, C> newNode)
    {
        Node<V, C> currentChild = pathToParent.get(0);
        Node<V, C> newChild = newNode;
        for (Node<V, C> parent : pathToParent.subList(1, pathToParent.size()))
        {
            TreeSet<Node<V, C>> children = new TreeSet<Node<V, C>>(parent.getChildren());
            children.remove(currentChild);
            children.add(newChild);

            currentChild = parent;
            newChild = new Node<V, C>(children, parent.getPath(), parent.getValues());
        }
        return newChild;
    }

    public Node<V, C> getParent(SearchPath path)
    {
        List<Node<V, C>> parents = findParents(path);
        return parents.isEmpty() ? null : parents.get(0);
    }

    public Node<V, C> search(SearchPath path)
    {
        checkNotNull(path);

        List<Node<V, C>> pathToParent = findParents(path);
        for(Node<V, C> node : pathToParent)
        {
            if (isActive(node) && node.getPath().matches(path))
            {
                return node;
            }
        }

        return null;
    }

    List<Node<V, C>> findParents(SearchPath path)
    {
        checkNotNull(path);

        List<Node<V, C>> pathToParent = new ArrayList<Node<V, C>>();
        Node<V, C> current = root;

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

    private boolean isParent(Node<V, C> current, SearchPath path)
    {
        if (current == null)
        {
            return false;
        }
        return current.getPath() == null || current.getPath().isParentOf(path);
    }

    public PriorityTrie<V, C> getSubtrie(SearchPath path)
    {
        checkNotNull(path);

        Node<V, C> subtrieRoot = getParent(path);
        cacheColors(subtrieRoot);
        Map<Node<V, C>, C> coloring = new HashMap<Node<V, C>, C>(nodeColoring);

        if (!path.equals(subtrieRoot.getPath()))
        {
            TreeSet<Node<V, C>> matchingChildren = new TreeSet<Node<V, C>>(subtrieRoot.getChildren(path));
            subtrieRoot = new Node<V, C>(matchingChildren, null, Collections.<ColoredValue<V, C>>emptyList());
            coloring.put(subtrieRoot, null);
        }

        return new PriorityTrie<V, C>(subtrieRoot, coloring);
    }

    private void cacheColors(Node<V, C> subtrieRoot)
    {
        Iterator<Node<V, C>> iterator = createIterator(subtrieRoot);
        while (iterator.hasNext())
        {
            iterator.next();
        }
    }

    public C getColor(Node<V, C> node)
    {
        if (node == null || node.getPath() == null)
        {
            return null;
        }

        if (!nodeColoring.containsKey(node))
        {
            //cache node coloring
            findParents(node.getPath());
        }
        return nodeColoring.get(node);
    }

    private void calculateColors(List<Node<V, C>> nodes)
    {
        Node<V, C> root = nodes.get(nodes.size() - 1);
        C parentColor = calculateColor(root, nodeColoring.get(root));
        nodeColoring.put(root, parentColor);
        for (int i = nodes.size() - 2; i >= 0; i--)
        {
            Node<V, C> currentNode = nodes.get(i);
            C color = nodeColoring.get(currentNode);
            if (color == null)
            {
                color = calculateColor(currentNode, parentColor);
                nodeColoring.put(currentNode, color);
            }

            if (currentNode.hasDominantColor())
            {
                parentColor = color;
            }
        }
    }

    private C calculateColor(Node<V, C> node, C cachedColor)
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
        Iterator<Node<V, C>> iterator = iterator();
        while (iterator.hasNext())
        {
            values.add(iterator.next().firstValue());
        }
        return values;
    }

    @Override
    public Iterator<Node<V, C>> iterator()
    {
        return createIterator(root);
    }

    private Iterator<Node<V,C>> createIterator(final Node<V, C> root)
    {
        return new Iterator<Node<V,C>>()
        {
            private final Deque<Node<V, C>> queue = new ArrayDeque<Node<V,C>>();
            private final Set<Node<V, C>> visited = new HashSet<Node<V,C>>();
            private Node<V, C> next;

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
            public Node<V, C> next()
            {
                Node<V, C> returnValue = next;
                next = findNext();
                return returnValue;
            }

            private Node<V, C> findNext()
            {
                Node<V, C> nextNode = findNextNode();
                while (nextNode != null && !isActive(nextNode))
                {
                    nextNode = findNextNode();
                }
                return nextNode;
            }

            private Node<V,C> findNextNode()
            {
                if (queue.isEmpty())
                {
                    return null;
                }

                Node<V, C> head = queue.peekFirst();
                while (!head.isLeaf() && !visited.contains(head))
                {
                    Set<Node<V, C>> children = head.getChildren();
                    for (Node<V, C> node : children)
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

    private boolean isActive(Node<V, C> node)
    {
        return compareSafely(node.getValueColor(), getColor(node)) <= 0 && node.getPath() != null;
    }

    /**
     * Print a representation of the {@code PriorityTrie} displaying the path names for debugging.
     */
    @Override
    public String toString()
    {
        Set<Node<V, C>> visited = new HashSet<Node<V, C>>();
        StringBuilder result = new StringBuilder();
        Deque<Node<V, C>> queue = new ArrayDeque<Node<V, C>>();
        queue.add(root);
        int indent = 0;
        while(!queue.isEmpty())
        {
            Node<V, C> current = queue.removeLast();
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
