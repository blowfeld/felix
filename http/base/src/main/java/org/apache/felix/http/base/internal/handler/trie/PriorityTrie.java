package org.apache.felix.http.base.internal.handler.trie;

import static org.apache.felix.http.base.internal.handler.trie.CompareUtil.compareSafely;
import static org.apache.felix.http.base.internal.handler.trie.CompareUtil.min;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public final class PriorityTrie<V, C extends Comparable<C>>
{
    private final Node<V, C> root;
    private final Map<Node<V, C>, C> nodeColoring;

    public PriorityTrie()
    {
        this(new Node<V, C>(null), new HashMap<Node<V, C>, C>());
    }

    private PriorityTrie(Node<V, C> root, Map<Node<V, C>, C> nodeColoring)
    {
        this.root = root;
        this.nodeColoring = nodeColoring;
    }

    public PriorityTrie<V, C> add(String path, V value)
    {
        checkNotNull(path);
        return add(path, value, null);
    }

    public PriorityTrie<V, C> add(String path, V value, C color)
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
            color = nodeColoring.get(parent);
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

        Map<Node<V, C>, C> newColoring = nodeColoring;
        C currentColor = nodeColoring.get(node);
        if (currentColor == null || currentColor.compareTo(color) > 0)
        {
            newColoring = updateColoring(newNode, color);
        }

        return new PriorityTrie<V, C>(newRoot, newColoring);
    }

    private PriorityTrie<V, C> addNewNode(List<Node<V, C>> pathToParent, String path, V value, C color)
    {
        Node<V, C> parent = pathToParent.get(0);

        NodePair<V, C> newNodes = addNodeToParent(parent, path, value, color);
        Node<V, C> newParent = newNodes.parent;
        Node<V, C> newNode = newNodes.node;

        Node<V, C> newRoot = updateParents(pathToParent, newParent);

        Map<Node<V, C>, C> newColoring = nodeColoring;
        C parentColor = nodeColoring.get(parent);
        if (parentColor == null || parentColor.compareTo(color) > 0)
        {
            newColoring = updateColoring(newNode, color);
        }
        else
        {
            newColoring = new HashMap<Node<V, C>, C>(nodeColoring);
            newColoring.put(newNode, parentColor);
        }

        return new PriorityTrie<V, C>(newRoot, newColoring);
    }

    private NodePair<V, C> addNodeToParent(Node<V, C> parent, String path, V value, C color)
    {
        TreeSet<Node<V, C>> presentChildren = new TreeSet<Node<V, C>>(parent.getChildren(path));
        TreeSet<Node<V, C>> siblings = new TreeSet<Node<V, C>>(parent.getChildren());
        siblings.removeAll(presentChildren);

        Node<V, C> newNode = new Node<V, C>(presentChildren, path, value, color);

        siblings.add(newNode);
        Node<V, C> newParent = new Node<V, C>(siblings, parent.getPath(), parent.getValues());

        return new NodePair<V, C>(newNode, newParent) ;
    }

    public PriorityTrie<V, C> remove(String path, V value, C color)
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
            color = nodeColoring.get(node);
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
        return updateNodeRemoveValue(pathToNode, newNode, color);
    }

    private PriorityTrie<V, C> updateNodeRemoveValue(List<Node<V, C>> pathToNode, Node<V, C> newNode, C color)
    {
        Node<V, C> node = pathToNode.get(0);
        Node<V, C> parent = pathToNode.get(1);

        Node<V, C> newRoot = updateParents(pathToNode, newNode);

        Map<Node<V, C>, C> newColoring = nodeColoring;
        C currentColor = nodeColoring.get(node);
        C parentColor = nodeColoring.get(parent);
        if (compareSafely(currentColor, color) == 0 && compareSafely(parentColor, color) != 0)
        {
            C newColor = min(parentColor, newNode.getValueColor());
            newColoring = updateChildColoring(newNode, currentColor, newColor);
            newColoring.put(newNode, newColor);
        }

        return new PriorityTrie<V, C>(newRoot, newColoring);
    }

    private PriorityTrie<V, C> removeEmptyNode(List<Node<V, C>> pathToNode)
    {
        Node<V, C> node = pathToNode.get(0);
        Node<V, C> parent = pathToNode.get(1);

        Node<V, C> newParent = removeNode(parent, node);
        Node<V, C> newRoot = updateParents(pathToNode.subList(1, pathToNode.size()), newParent);

        Map<Node<V, C>, C> newColoring = nodeColoring;

        C currentColor = nodeColoring.get(node);
        C parentColor = nodeColoring.get(parent);
        if (compareSafely(currentColor, parentColor) != 0)
        {
            newColoring = updateChildColoring(newParent, currentColor, parentColor);
        }
        newColoring.remove(node);

        return new PriorityTrie<V, C>(newRoot, newColoring);
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

    // TODO combine updateColoring methods ?
    private Map<Node<V, C>, C> updateColoring(Node<V, C> node, C newColor)
    {
        Map<Node<V, C>, C> coloring = new HashMap<Node<V, C>, C>(nodeColoring);

        Deque<Node<V, C>> queue = new ArrayDeque<Node<V, C>>();
        queue.add(node);
        while(!queue.isEmpty())
        {
            Node<V, C> current = queue.removeLast();
            C currentColor = coloring.get(current);
            if (currentColor == null || currentColor.compareTo(newColor) > 0)
            {
                coloring.put(current, newColor);
                queue.addAll(current.getChildren());
            }
        }
        return coloring;
    }

    private Map<Node<V, C>, C> updateChildColoring(Node<V, C> node, C oldColor, C newColor)
    {
        Map<Node<V, C>, C> coloring = new HashMap<Node<V, C>, C>(nodeColoring);
        Set<Node<V, C>> children = node.getChildren();
        if (newColor == null)
        {
            for (Node<V, C> child : children)
            {
                C childColor = child.getValueColor();
                coloring.put(child, childColor);
                coloring.putAll(updateChildColoring(child, oldColor, childColor));
            }
            return coloring;
        }

        Deque<Node<V, C>> queue = new ArrayDeque<Node<V, C>>();
        queue.addAll(node.getChildren());
        while(!queue.isEmpty())
        {
            Node<V, C> current = queue.removeLast();
            C currentColor = coloring.get(current);

            boolean colorIsChanged = currentColor == null ||
                currentColor.compareTo(oldColor) == 0 ||
                currentColor.compareTo(newColor) > 0;

            if (colorIsChanged)
            {
                coloring.put(current, newColor);
                queue.addAll(current.getChildren());
            }
        }
        return coloring;
    }

    public Node<V, C> search(String path)
    {
        checkNotNull(path);

        List<Node<V, C>> pathToParent = findParents(path);
        for (Node<V, C> node : pathToParent)
        {
            if (compareSafely(node.getValueColor(), nodeColoring.get(node)) <= 0)
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

    public C getColor(Node<V, C> node)
    {
        return nodeColoring.get(node);
    }

    private <T> T checkNotNull(T value)
    {
        if (value == null)
        {
            throw new NullPointerException("Argument must not be null");
        }
        return value;
    }

    private static class NodePair<V, C extends Comparable<C>>
    {
        final Node<V, C> node;
        final Node<V, C> parent;

        NodePair(Node<V, C> node, Node<V, C> parent)
        {
            this.node = node;
            this.parent = parent;
        }
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
        ArrayDeque<Node<V, C>> queue = new ArrayDeque<Node<V, C>>();
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
                result.append(" [n:" + nodeColoring.get(current) + ", v:" + current.getValueColor() + "]");
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
