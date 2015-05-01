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

public interface PriorityTree<V extends Comparable<V>, C extends Comparable<C>> extends Iterable<Node<V, C>>
{
    PriorityTree<V, C> add(SearchPath path, V value);

    PriorityTree<V, C> add(SearchPath path, V value, C color);

    PriorityTree<V, C> remove(SearchPath path, V value, C color);

    /**
     * Finds the active parent of the given path.
     * @param path
     * @return
     */
    Node<V, C> search(SearchPath path);

    Node<V, C> getParent(SearchPath path);

    PriorityTree<V, C> getSubtrie(SearchPath path);

    C getColor(Node<V, C> node);

    /**
     * Returns an iterator over all active nodes.
     */
    @Override
    Iterator<Node<V, C>> iterator();

    Collection<V> activeValues();
}