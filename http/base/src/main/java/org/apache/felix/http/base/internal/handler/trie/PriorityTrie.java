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

import static java.util.Arrays.asList;
import static org.apache.felix.http.base.internal.handler.trie.CompareUtil.compareSafely;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class PriorityTrie<V, C extends Comparable<C>> implements Iterable<Node<V, C>>, PriorityTree<V, C>
{
    private static final String WILDCARD = "/*";
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("\\Q" + WILDCARD + "\\E[.][^.]+$");
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("[.][^.]+$");

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
        this.root = root;
        this.nodeColoring = coloring;
        this.nodeColoring.put(root, null);
    }

    public PriorityTrie<V, C> add(String path, V value)
    {
        checkNotNull(path);
        return add(path, value, null);
    }

    public PriorityTrie<V, C> add(String path, V value, C color)
    {
        checkNotNull(path);

        List<String> pathComponents = splitPath(path);
        String searchPath = pathComponents.get(0);
        String wildcard = pathComponents.get(1);
        boolean isWildcard = wildcard != null;

        List<Node<V, C>> pathToParent = findParents(searchPath);

        Node<V, C> parent = pathToParent.get(0);
        if (color == null && parent.equals(root))
        {
            throw new IllegalArgumentException("Root nodes must be colored");
        }
        else if (color == null)
        {
            color = getColor(parent);
        }

        if (isWildcard == parent.isWildcard() && searchPath.equals(parent.getPath()))
        {
            return updateNodeAddValue(pathToParent, value, color);
        }
        return addNewNode(pathToParent, searchPath, wildcard, value, color);
    }

    private PriorityTrie<V, C> updateNodeAddValue(List<Node<V, C>> pathToNode, V value, C color)
    {
        Node<V, C> node = pathToNode.get(0);

        Node<V, C> newNode = node.addValue(value, color);
        Node<V, C> newRoot = updateParents(pathToNode, newNode);

        return new PriorityTrie<V, C>(newRoot);
    }

    private PriorityTrie<V, C> addNewNode(List<Node<V, C>> pathToParent, String path, String extension, V value, C color)
    {
        Node<V, C> parent = pathToParent.get(0);
        if (parent.isWildcard() && path.equals(parent.getPath()))
        {
            // TODO could be new root
            parent = pathToParent.remove(0);
            parent = pathToParent.get(0);
        }

        Node<V, C> newParent = addNodeToParent(parent, path, extension, value, color);
        Node<V, C> newRoot = updateParents(pathToParent, newParent);

        return new PriorityTrie<V, C>(newRoot);
    }

    private Node<V, C> addNodeToParent(Node<V, C> parent, String path, String extension, V value, C color)
    {
        TreeSet<Node<V, C>> presentChildren = new TreeSet<Node<V, C>>(parent.getChildren(path));
        TreeSet<Node<V, C>> siblings = new TreeSet<Node<V, C>>(parent.getChildren());
        siblings.removeAll(presentChildren);

        Node<V, C> newNode = new Node<V, C>(presentChildren, path, extension, value, color);

        siblings.add(newNode);
        Node<V, C> newParent = new Node<V, C>(siblings, parent.getPath(), parent.getExtension(), parent.getValues());

        return newParent;
    }

    public PriorityTrie<V, C> remove(String path, V value, C color)
    {
        checkNotNull(path);

        List<String> pathComponents = splitPath(path);
        String searchPath = pathComponents.get(0);
        String wildcard = pathComponents.get(1);
        boolean isWildcard = wildcard != null;

        List<Node<V, C>> pathToNode = findParents(searchPath);
        Node<V, C> node = pathToNode.get(0);

        if (!searchPath.equals(node.getPath()) || isWildcard != node.isWildcard())
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

        return new Node<V, C>(newChildren, parent.getPath(), parent.getExtension(), parent.getValues());
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
            newChild = new Node<V, C>(children, parent.getPath(), parent.getExtension(), parent.getValues());
        }
        return newChild;
    }

//    public Node<V, C> search(String path)
//    {
//        return searchPath(path).get(0);
//    }

    public List<Node<V, C>> searchPath(String path)
    {
        Matcher matcher = EXTENSION_PATTERN.matcher(path);
        matcher.matches();
//        String wildcard = matcher.group(1);
        String searchPath = path;
        List<Node<V, C>> pathToParent = findParents(searchPath);
        if (!nodeColoring.containsKey(pathToParent.get(0)))
        {
            calculateColors(pathToParent);
        }

        Iterator<Node<V, C>> iterator = pathToParent.iterator();
        while (iterator.hasNext())
        {
            Node<V, C> node = iterator.next();
            //remove shadowed and empty root
            if (!isActive(node, nodeColoring))
            {
                iterator.remove();
            }
        }
        return pathToParent;
    }

    public Node<V, C> search(String path)
    {
        checkNotNull(path);

        Matcher matcher = EXTENSION_PATTERN.matcher(path);
        matcher.matches();
//        String wildcard = matcher.group(1);
        String searchPath = path;

        List<Node<V, C>> pathToParent = findParents(searchPath);
        Node<V, C> parent = pathToParent.get(0);
        if (path.equals(parent.getPath()))
        {
            if (isActive(parent, nodeColoring) && !parent.isWildcard())
            {
                return parent;
            }
            else if (parent.isWildcard())
            {
                Node<V, C> grandParent = pathToParent.get(1);
                if (isActive(grandParent, nodeColoring) && path.equals(grandParent.getPath()))
                {
                    return grandParent;
                }
            }
        }

        for(Node<V, C> node : pathToParent)
        {
            if (isActive(node, nodeColoring) && node.isWildcard())
            {
                return node;
            }
        }

        return null;
    }

    List<Node<V, C>> findParents(String path)
    {
        checkNotNull(path);

        List<Node<V, C>> pathToParent = new ArrayList<Node<V, C>>();
        Node<V, C> current = root;

        while (isPrefix(current, path))
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

    private boolean isPrefix(Node<V, C> current, String path)
    {
        if (current == null)
        {
            return false;
        }
        return current.getPath() == null || path.startsWith(current.getPath());
    }

    private List<String> splitPath(String path)
    {
        int wildcardLength = WILDCARD.length();
        if (path.endsWith(WILDCARD))
        {
            return asList(path.substring(0, path.length() - wildcardLength), "");
        }
        Matcher matcher = WILDCARD_PATTERN.matcher(path);
        if (!matcher.matches())
        {
            return asList(path, null);
        }
        String extension = matcher.group(0);
        String searchPath = path.substring(0, extension.length() + wildcardLength);
        return asList(searchPath, extension);
    }

    public PriorityTrie<V, C> getSubtrie(String path)
    {
        return new PriorityTrie<V, C>(search(path), new HashMap<Node<V, C>, C>(nodeColoring));
    }

    public C getColor(Node<V, C> node)
    {
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

            parentColor = color;
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
    public Iterator<Node<V, C>> iterator()
    {
        return createIterator(root, nodeColoring);
    }

    private Iterator<Node<V,C>> createIterator(final Node<V, C> root,
        final Map<Node<V, C>, C> coloring)
    {
        return new Iterator<Node<V,C>>()
        {
            private final Deque<Node<V, C>> queue = new ArrayDeque<Node<V,C>>();
            private final Set<Node<V, C>> visited = new HashSet<Node<V,C>>();
            private Node<V, C> next;

            {
                queue.add(root);
                next = findNextActive();
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
                next = findNextActive();
                return returnValue;
            }

            private Node<V, C> findNextActive()
            {
                Node<V, C> nextNode = findNextNode();
                while (nextNode != null && !isActive(nextNode, coloring))
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

    private boolean isActive(Node<V, C> node, Map<Node<V, C>, C> nodeColoring)
    {
        return compareSafely(node.getValueColor(), getColor(node)) <= 0 && node.getPath() != null;
    }

    /*
     * Print a representation of the {@code PriorityTrie} displaying the path names for debugging.
     *
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
                result.append(" [n:" + getColor(current) + ", v:" + current.getValueColor() + "]");
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
