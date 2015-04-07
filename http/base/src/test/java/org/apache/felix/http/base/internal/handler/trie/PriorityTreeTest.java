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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class PriorityTreeTest
{
    private static final Node[] NODE_ARRAY = new Node[0];

    private PriorityTrie<String, Integer> emptyTrie;

    @Before
    public void setup()
    {
        emptyTrie = new PriorityTrie<String, Integer>();
    }

    @Test
    public void addAndFindParents()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);

        assertThat(trie.findParents("/a"), contains(nodes("/", null)));
    }

    @Test
    public void addAndFindParentsExactMatch()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 1);

        assertThat(trie.findParents("/a"), contains(nodes("/a", null)));
    }

    @Test
    public void addAndFindParentsWithSiblings()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/aa", "");
        trie = trie.add("/ab", "");

        assertThat(trie.findParents("/aa"), contains(nodes("/aa", "/", null)));
    }

    @Test
    public void addAndFindParentsWithChildrensPresent()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "");
        trie = trie.add("/a0", "");
        trie = trie.add("/a/a", "");

        assertThat(trie.findParents("/a"), contains(nodes("/a", "/", null)));
    }

    @Test
    public void addAndFindParentsWithMultipleBranchesAddedInRandomOrder()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a/a/a", "", 1);
        trie = trie.add("", "", 1);
        trie = trie.add("/bb/a/a", "");
        trie = trie.add("/", "");
        trie = trie.add("/a", "");
        trie = trie.add("/bb", "");

        assertThat(trie.findParents("/a/a/a"), contains(nodes(
            "/a/a/a",
            "/a",
            "/",
            "",
            null)));

        assertThat(trie.findParents("/bb/a/a"), contains(nodes(
            "/bb/a/a",
            "/bb",
            "/",
            "",
            null)));
    }

    @Test
    public void removeLeaf()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "");
        trie = trie.add("/a/a", "");
        trie = trie.add("/a/b", "");

        trie = trie.remove("/a/a", "", null);

        assertThat(trie.findParents("/a/a"), contains(nodes("/a", "/", null)));
        assertThat(trie.findParents("/a/b"), contains(nodes("/a/b", "/a", "/", null)));
    }

    @Test
    public void removeNodeWithChildrenAddsChildrenToParent()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "");
        trie = trie.add("/a/a", "");
        trie = trie.add("/a/b", "");

        trie = trie.remove("/a", "", null);

        assertThat(trie.findParents("/a/a"), contains(nodes("/a/a", "/", null)));
        assertThat(trie.findParents("/a/b"), contains(nodes("/a/b", "/", null)));
    }

    @Test
    public void removeNoneExistingNodeReturnsSameInstance()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "");

        assertSame(trie.remove("/a/a", "", null), trie);
    }

    @Test
    public void removeNoneExistingValueReturnsSameInstance()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 1);
        PriorityTrie<String, Integer> trieAfterRemoval = trie.remove("/a", "", 2);

        assertSame(trie, trieAfterRemoval);
    }

    @Test
    public void removceMinimumValueUpdatesToParentColor()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 3);
        trie = trie.add("/a/a", "", 2);
        trie = trie.add("/a/a", "", 4);

        trie = trie.remove("/a/a", "", 2);

        assertEquals(Integer.valueOf(3), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(3), trie.getColor(node("/a/a")));
    }

    @Test
    public void searchFindsCorrectParent()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "");

        assertEquals(trie.search("/a/a"), node("/a"));
    }

    @Test
    public void searchFindsCorrectParentWithColoring()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "", 1);
        trie = trie.add("/a/a", "", 2);

        assertEquals(trie.search("/a"), node("/a"));
    }

    @Test
    public void searchIgnoresShadowedValues()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 1);
        trie = trie.add("/a/a", "", 2);

        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a")));
        assertEquals(node("/a"), trie.search("/a/a"));
    }

    @Test
    public void nodeColorIsSetToParentIfLarger()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "", 1);
        trie = trie.add("/a/a/a", "", 1);
        trie = trie.add("/a/a", "", 2);

        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a/a")));
    }

    @Test
    public void nodeColorIsSetToParentIfEqual()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "", 1);
        trie = trie.add("/a/a/a", "", 1);
        trie = trie.add("/a/a", "", 1);

        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a/a")));
    }

    @Test
    public void nodeColorIsSetToParentIfSmaller()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 2);
        trie = trie.add("/a", "", 2);
        trie = trie.add("/a/a/a", "", 2);
        trie = trie.add("/a/a", "", 1);

        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a/a")));
    }

    @Test
    public void nodeColorsAreUpdatedAtRemove()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 3);
        trie = trie.add("/a", "", 3);
        trie = trie.add("/a/a", "", 2);
        trie = trie.add("/a/a/a", "", 4);
        trie = trie.add("/a/a/b", "", 1);

        assertEquals(Integer.valueOf(3), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a/b")));

        trie = trie.remove("/a/a", "", 2);

        assertEquals(Integer.valueOf(3), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(3), trie.getColor(node("/a/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a/b")));
    }

    @Test
    public void nodeColorsAreUpdatedAtRemoveRoot()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 3);
        trie = trie.add("/a", "", 3);
        trie = trie.add("/a/a", "", 2);
        trie = trie.add("/a/a/a", "", 4);
        trie = trie.add("/a/a/b", "", 1);

        trie = trie.remove("/", "", 3);

        assertEquals(Integer.valueOf(3), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a/b")));
    }

    @Test
    public void multipleValuesInOneNode()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 2);
        trie = trie.add("/a/a", "", 3);

        assertEquals(Integer.valueOf(2), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a")));

        trie = trie.add("/a/a", "", 1);

        assertEquals(Integer.valueOf(2), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(node("/a/a")));
    }

    @Test
    public void multipleValuesInOneNodeAddedInReverseOrder()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 2);
        trie = trie.add("/a/a", "", 2);

        assertEquals(Integer.valueOf(2), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a")));

        trie = trie.add("/a/a", "", 3);

        assertEquals(Integer.valueOf(2), trie.getColor(node("/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(node("/a/a")));
    }

    @Test
    public void addReturnsNewInstance()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 1);
        PriorityTrie<String, Integer> trieWithMultipleValues = trie.add("/a", "", 2);

        assertNotSame(emptyTrie, trie);
        assertNotSame(trie, trieWithMultipleValues);
    }

    @Test
    public void removeReturnsNewInstance()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "", 1);
        trie = trie.add("/a", "", 2);

        PriorityTrie<String, Integer> trieAfterFirstRemoval = trie.remove("/a", "", 2);
        PriorityTrie<String, Integer> trieAfterSecondRemoval = trieAfterFirstRemoval.remove("/a", "", 1);

        assertNotSame(trie, trieAfterFirstRemoval);
        assertNotSame(trieAfterFirstRemoval, trieAfterSecondRemoval);
    }

    private Node<String, Integer> node(String path)
    {
        return new Node<String, Integer>(path);
    }

    private Node[] nodes(String... nodePaths)
    {
        List<Node> nodes = new ArrayList<Node>();
        for (String nodePath : nodePaths)
        {
            nodes.add(new Node(nodePath));
        }
        return nodes.toArray(NODE_ARRAY);
    }
}
