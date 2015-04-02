package org.apache.felix.http.base.internal.handler.trie;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
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
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");

        assertThat(trie.findParents("/a"), contains(nodes("/", null)));
    }

    @Test
    public void addAndFindParentsExactMatch()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a", "");

        assertThat(trie.findParents("/a"), contains(nodes("/a", null)));
    }

    @Test
    public void addAndFindParentsWithSiblings()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");
        trie = trie.add("/aa", "");
        trie = trie.add("/ab", "");

        assertThat(trie.findParents("/aa"), contains(nodes("/aa", "/", null)));
    }

    @Test
    public void addAndFindParentsWithChildrensPresent()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");
        trie = trie.add("/a", "");
        trie = trie.add("/a0", "");
        trie = trie.add("/a/a", "");

        assertThat(trie.findParents("/a"), contains(nodes("/a", "/", null)));
    }

    @Test
    public void addAndFindParentsWithMultipleBranchesAddedInRandomOrder()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/a/a/a", "");
        trie = trie.add("", "");
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
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");
        trie = trie.add("/a", "");
        trie = trie.add("/a/a", "");
        trie = trie.add("/a/b", "");

        trie = trie.remove("/a/a", "", null);

        assertThat(trie.findParents("/a/a"), contains(nodes("/a", "/", null)));
        assertThat(trie.findParents("/a/b"), contains(nodes("/a/b", "/a", "/", null)));
    }

    @Test
    public void removeNodeWithChildren()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");
        trie = trie.add("/a", "");
        trie = trie.add("/a/a", "");
        trie = trie.add("/a/b", "");

        trie = trie.remove("/a", "", null);

        assertThat(trie.findParents("/a/a"), contains(nodes("/a/a", "/", null)));
        assertThat(trie.findParents("/a/b"), contains(nodes("/a/b", "/", null)));
    }

    @Test
    public void removeNoneExistingNode()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");
        trie = trie.add("/a", "");

        assertSame(trie.remove("/a/a", "", null), trie);
    }

    @Test
    public void searchFindsCorrectParent()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "");
        trie = trie.add("/a", "");

        assertEquals(trie.search("/a/a"), new Node<String, Integer>("/a"));
    }

    @Test
    public void searchFindsCorrectParentWithColoring()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "", 1);
        trie = trie.add("/a/a", "", 2);

        assertEquals(trie.search("/a"), new Node<String, Integer>("/a"));
    }

    @Test
    public void nodeColorIsSetToParentIfLarger()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "", 1);
        trie = trie.add("/a/a/a", "", 1);
        trie = trie.add("/a/a", "", 2);

        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a/a")));
    }

    @Test
    public void nodeColorIsSetToParentIfEqual()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 1);
        trie = trie.add("/a", "", 1);
        trie = trie.add("/a/a/a", "", 1);
        trie = trie.add("/a/a", "", 1);

        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a/a")));
    }

    @Test
    public void nodeColorIsSetToParentIfSmaller()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 2);
        trie = trie.add("/a", "", 2);
        trie = trie.add("/a/a/a", "", 2);
        trie = trie.add("/a/a", "", 1);

        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a/a")));
    }

    @Test
    public void nodeColorIsUpdatedAtRemove()
    {
        PriorityTrie<String, Integer> trie = emptyTrie.add("/", "", 3);
        trie = trie.add("/a", "", 3);
        trie = trie.add("/a/a", "", 2);
        trie = trie.add("/a/a/a", "", 4);
        trie = trie.add("/a/a/b", "", 1);

        assertEquals(Integer.valueOf(3), trie.getColor(new Node<String, Integer>("/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(new Node<String, Integer>("/a/a")));
        assertEquals(Integer.valueOf(2), trie.getColor(new Node<String, Integer>("/a/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a/b")));

        trie = trie.remove("/a/a", "", 2);
        System.err.println(trie);

        assertEquals(Integer.valueOf(3), trie.getColor(new Node<String, Integer>("/a")));
        assertEquals(Integer.valueOf(3), trie.getColor(new Node<String, Integer>("/a/a/a")));
        assertEquals(Integer.valueOf(1), trie.getColor(new Node<String, Integer>("/a/a/b")));
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
