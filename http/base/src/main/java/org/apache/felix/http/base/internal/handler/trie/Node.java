package org.apache.felix.http.base.internal.handler.trie;

import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;


final class Node<V, C extends Comparable<C>> implements Comparable<Node<V, C>>
{
    private final TreeSet<Node<V, C>> children;
    private final String path;
    private final Collection<ColoredValue<V, C>> values;

    Node(TreeSet<Node<V, C>> children, String path, Collection<ColoredValue<V, C>> values)
    {
        this.children = children;
        this.path = path;
        this.values = values;
    }

    Node(String path)
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
        Collection<ColoredValue<V, C>> newValues = createValues();
        newValues.addAll(values);
        newValues.remove(new ColoredValue<V, C>(value, color));
        return new Node<V, C>(children, path, newValues);
    }

    String getPath()
    {
        return path;
    }

    Collection<ColoredValue<V, C>> getValues()
    {
        return unmodifiableCollection(values);
    }

    boolean isEmpty()
    {
        return values.isEmpty();
    }

    C getValueColor()
    {
        if (isEmpty())
        {
            return null;
        }
        return values.iterator().next().getColor();
    }

    TreeSet<Node<V, C>> getChildren()
    {
        //TODO unmodifiable
        return children;
    }

    SortedSet<Node<V, C>> getChildren(String prefix)
    {
        if (prefix.equals(""))
        {
            return children;
        }

        Node<V, C> startNode = new Node<V, C>(prefix);
        Node<V, C> endNode = new Node<V, C>(incrementPrefix(prefix));
        //TODO unmodifiable
        return children.subSet(startNode, endNode);
    }

    boolean isLeaf()
    {
        return children.isEmpty();
    }

    private String incrementPrefix(String prefix)
    {
        char[] charArray = prefix.toCharArray();
        int lastIndex = charArray.length - 1;
        if (charArray[lastIndex] == Character.MAX_VALUE)
        {
            throw new IllegalArgumentException("Unsupported character in path (Character.MAX_VALUE)");
        }
        charArray[charArray.length - 1] += 1;

        return new String(charArray);
    }

    private Collection<ColoredValue<V, C>> createValues()
    {
        return new TreeSet<ColoredValue<V,C>>();
    }

    @Override
    public int compareTo(Node<V, C> other)
    {
        if (path == null)
        {
            return -1;
        }
        if (other == null)
        {
            return 1;
        }
        return path.compareTo(other.path);
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
        return path.equals(other.path);
    }

    @Override
    public String toString()
    {
        return path;
    }
}