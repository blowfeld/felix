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

import java.util.Collection;
import java.util.Iterator;


/**
 * {@code PriorityTrieMultimap} is a colored trie structure for retrieval of highest priority elements.
 * <p>
 * The {@code PriorityTrieMultimap} provides storage of values by {@link SearchPath}
 * keys which represent a path in the trie. Depending on the used
 * {@code SearchPath}s, lookup matches the key with the longest matching
 * sub-path (prefix).
 * <p>
 * Each key can store multiple values, where each value has a priority and can
 * be associated with a color. The color resulting from the values stored in a
 * node is the color of the highest priority associated with those values.
 * The color of a node in the trie is determined by the following rules:
 * <ul>
 *  <li>a node inherits the color from its parent if that is of higher priority
 *      than the color resulting from its values</li>
 *  <li>if the parent has no color or the parent color is of lower priority,
 *      the color of the node is the color resulting from its values</li>
 * </ul>
 * {@code SearchPath} keys can define to be ignored in the color inheritance.
 * In that case they pass on the color of their parent.
 * <p>
 * A node in the trie is considered active if the color resulting from its
 * values has the same priority than the color of the node.
 * <p>
 * Value retrieval from a node provides the value that has the highest
 * priority among the values associated with the highest priority color of the
 * values stored in the node.
 * <p>
 * The priority of colors and values is determined from their natural ordering
 * and the <em>least</em> element with respect to this ordering has the highest
 * priority.
 *
 * @param <V> the value type to be stored in the {@code PriorityTrieMultimap}.
 * @param <C> the color type
 */
public interface PriorityTrieMultimap<V extends Comparable<V>, C extends Comparable<C>> extends Iterable<Node<V, C>>
{
    /**
     * Returns a copy of this trie with the value added associated with the given path.
     * <p>
     * If the trie does not contain a parent for the given path the color must
     * not be null.
     *
     * @param path the {@link SearchPath} key to store against
     * @param value  the value to add to the collection at the path
     * @param color the color associated with the value
     *
     * @return a copy of this trie with the new value added associated at the
     *          given path
     */
    PriorityTrieMultimap<V, C> add(SearchPath path, V value, C color);

    /**
     * Returns a copy of this trie with the value association removed from the given path.
     * <p>
     *
     * @param path the {@link SearchPath} key to remove from
     * @param value  the value to add to the collection at the path
     * @param color the color associated with the value
     *
     * @return a copy of this trie with the value association removed from the
     *          given path, or this trie if there was no such association
     */
    PriorityTrieMultimap<V, C> remove(SearchPath path, V value, C color);

    /**
     * Finds the active matching node for the given path.
     * <p>
     * The method returns the node that matches the given path and has the
     * longest sub-path (prefix) among the nodes stored in the trie. Nodes
     * that are not active, i.e. the color resulting from the associated values
     * has lower priority than the node color, are ignored by this search.
     *
     * @param path the {@link SearchPath} key to search for
     *
     * @return the active matching node with the longest prefix
     */
    Node<V, C> search(SearchPath path);

    /**
     * Returns the node with the longest matching sub-path (prefix) for the given path.
     * <p>
     * The method returns the node that has the longest sub-path (prefix) for
     * the given path in the trie. Opposite to {@link #search(SearchPath)} also
     * inactive or non matching nodes are taken into account.
     *
     * @param path the {@link SearchPath} key to search for
     *
     * @return the node with the longest prefix from the trie
     */
    Node<V, C> getPrefix(SearchPath path);

    /**
     * Returns the color associated with the given node.
     * <p>
     * The color of a node in the trie is determined by the following rules:
     * <ul>
     *  <li>a node inherits the color from its parent if that is of higher priority
     *      than the highest priority color associated with the values stored in
     *      this node</li>
     *  <li>if the parent has no color or the parent color is of lower priority,
     *      the color of the node is the highest priority color associated with
     *      the values stored in this node</li>
     * </ul>
     * {@code SearchPath} keys can define to be ignored in the color inheritance.
     * In that case they pass on the color of their parent.
     *
     * @param path the {@link SearchPath} key to search for
     *
     * @return the node with the longest prefix from the trie
     */
    C getColor(Node<V, C> node);

    /**
     * Returns the sub-trie starting from the given path.
     * <p>
     * The sub-trie contains all nodes from this tree for with the given path
     * is a prefix. If the root node for the sub-trie is not contained in this
     * trie, and inactive, empty root node is generated.
     *
     * @param path the starting {@link SearchPath} key for the sub-trie
     *
     * @return a sub-trie containing all nodes from this trie for with the given
     *          path is a prefix
     */
    PriorityTrieMultimap<V, C> getSubtrie(SearchPath path);

    /**
     * Returns the highest priority values from the active nodes in this trie.
     *
     * @return a {@link Collection} containing the highest priority values from
     *          the active nodes in this trie
     */
    Collection<V> activeValues();

    /**
     * Returns an iterator over all active nodes in this trie.
     * <p>
     * The nodes are sorted according to the ordering implied by the
     * {@link SearchPath} keys.
     *
     * @return an iterator over all active nodes in this trie
     */
    @Override
    Iterator<Node<V, C>> iterator();

}