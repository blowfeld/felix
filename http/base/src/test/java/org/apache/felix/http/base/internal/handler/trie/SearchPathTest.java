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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SearchPathTest
{
    @Test
    public void createFromSimplePattern()
    {
        SearchPath path = SearchPath.forPattern("/testurl");

        assertEquals("/testurl", path.getPath());
        assertFalse(path.isWildcard());
        assertNull(path.getExtension());
    }

    @Test
    public void createFromWildcardPattern()
    {
        SearchPath path = SearchPath.forPattern("/testurl/*");

        assertEquals("/testurl", path.getPath());
        assertTrue(path.isWildcard());
        assertNull(path.getExtension());
    }

    @Test
    public void createFromExtensionPattern()
    {
        SearchPath path = SearchPath.forPattern("/testcontext/*.exe");

        assertEquals("/testcontext", path.getPath());
        assertTrue(path.isWildcard());
        assertEquals("exe", path.getExtension());

    }

    @Test
    public void createSimple()
    {
        SearchPath path = SearchPath.forPath("/testurl.html");

        assertEquals("/testurl.html", path.getPath());
        assertFalse(path.isWildcard());
        assertNull(path.getExtension());
    }

    @Test
    public void createForExtensionSearch()
    {
        SearchPath path = SearchPath.forExtensionPath("/testurl/test.exe");

        assertEquals("/testurl/test", path.getPath());
        assertFalse(path.isWildcard());
        assertEquals("exe", path.getExtension());

        assertNull(SearchPath.forExtensionPath("/testcontext/exe"));
    }

    @Test
    public void exactPrefixIsParentOfPath()
    {
        SearchPath prefix = SearchPath.forPattern("/testpath");
        SearchPath path = SearchPath.forPattern("/testpath/test");

        assertTrue(prefix.isPrefix(path));
        assertFalse(path.isPrefix(prefix));
    }

    @Test
    public void wildcardPrefixIsParentOfPath()
    {
        SearchPath prefix = SearchPath.forPattern("/testpath/*");
        SearchPath path = SearchPath.forPattern("/testpath/test");

        assertTrue(prefix.isPrefix(path));
        assertFalse(path.isPrefix(prefix));
    }

    @Test
    public void wildcardIsParentOfExactPath()
    {
        SearchPath wildcard = SearchPath.forPattern("/testpath/*");
        SearchPath exact = SearchPath.forPattern("/testpath/");

        assertTrue(wildcard.isPrefix(exact));
        assertFalse(exact.isPrefix(wildcard));
    }

    @Test
    public void wildcardIsNotParentOfExtensionPath()
    {
        SearchPath wildcard = SearchPath.forPattern("/testpath/*");
        SearchPath extension = SearchPath.forPattern("/testpath/*.exe");
        SearchPath prefixedExtension = SearchPath.forPattern("/testpath/test/*.exe");

        assertFalse(wildcard.isPrefix(extension));
        assertFalse(extension.isPrefix(wildcard));

        assertFalse(wildcard.isPrefix(prefixedExtension));
        assertFalse(prefixedExtension.isPrefix(wildcard));
    }

    @Test
    public void exactIsNotParentOfExtensionPath()
    {
        SearchPath exact = SearchPath.forPattern("/testpath");
        SearchPath extension = SearchPath.forPattern("/testpath/*.exe");
        SearchPath prefixedExtension = SearchPath.forPattern("/testpath/test/*.exe");

        assertFalse(exact.isPrefix(extension));
        assertFalse(extension.isPrefix(exact));

        assertFalse(exact.isPrefix(prefixedExtension));
        assertFalse(prefixedExtension.isPrefix(exact));
    }

    @Test
    public void wildcardIsParentOfItself()
    {
        SearchPath wildcard = SearchPath.forPattern("/testpath/*");

        assertTrue(wildcard.isPrefix(wildcard));
    }

    @Test
    public void exactIsParentOfItself()
    {
        SearchPath exact = SearchPath.forPattern("/testpath");

        assertTrue(exact.isPrefix(exact));
    }

    @Test
    public void extensionIsParentOfItself()
    {
        SearchPath extension = SearchPath.forPattern("/testpath/*.html");

        assertTrue(extension.isPrefix(extension));
    }

    @Test
    public void matchForExactPath()
    {
        SearchPath path = SearchPath.forPattern("/testcontext/test");

        // SearchPaths created from urls
        assertTrue(path.matches(SearchPath.forPath("/testcontext/test")));

        assertFalse(path.matches(SearchPath.forPath("/testcontext/tester")));
        assertFalse(path.matches(SearchPath.forPath("/testcontext/tes")));

        // SearchPaths created from patterns
        assertTrue(path.matches(SearchPath.forPattern("/testcontext/test")));

        assertFalse(path.matches(SearchPath.forPattern("/testcontext/tester")));
        assertFalse(path.matches(SearchPath.forPattern("/testcontext/tes")));

        // SearchPaths created for extension search
        assertFalse(path.matches(SearchPath.forExtensionPath("/testcontext/test.exe")));
    }

    @Test
    public void wildcardMatchesChildren()
    {
        SearchPath path = SearchPath.forPattern("/testcontext/*");

        // SearchPaths created from urls
        assertTrue(path.matches(SearchPath.forPath("/testcontext/test.exe")));
        assertTrue(path.matches(SearchPath.forPath("/testcontext")));

        assertFalse(path.matches(SearchPath.forPath("/testcontex")));

        // SearchPaths created from pattern
        assertTrue(path.matches(SearchPath.forPattern("/testcontext/*")));
        assertTrue(path.matches(SearchPath.forPattern("/testcontext/test.exe")));

        assertFalse(path.matches(SearchPath.forPattern("/testcontext/*.exe")));
        assertFalse(path.matches(SearchPath.forPattern("/testcontex")));

        // SearchPaths created for extension search
        assertFalse(path.matches(SearchPath.forExtensionPath("/testcontext/test.exe")));
    }

    @Test
    public void extensionPathMatchesChildren()
    {
        SearchPath path = SearchPath.forPattern("/testcontext/*.exe");

        // SearchPaths created from urls
        assertFalse(path.matches(SearchPath.forPath("/testcontext/test.exe")));
        assertFalse(path.matches(SearchPath.forPath("/testcontext")));
        assertFalse(path.matches(SearchPath.forPath("/testcontex")));

        // SearchPaths created from pattern
        assertTrue(path.matches(SearchPath.forPattern("/testcontext/*.exe")));

        assertFalse(path.matches(SearchPath.forPattern("/testcontext/*")));
        assertFalse(path.matches(SearchPath.forPattern("/testcontext/test.exe")));
        assertFalse(path.matches(SearchPath.forPattern("/testcontex")));

        // SearchPaths created for extension search
        assertTrue(path.matches(SearchPath.forExtensionPath("/testcontext/test.exe")));
    }

    @Test
    public void wildcardMatchesOnlyFullPathSegments()
    {
        SearchPath path = SearchPath.forPattern("/testcontext/*");

        // SearchPaths created from urls
        assertTrue(path.matches(SearchPath.forPath("/testcontext/foo")));
        assertFalse(path.matches(SearchPath.forPath("/testcontextfoo")));
    }

}
