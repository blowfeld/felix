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
import static org.apache.felix.http.base.internal.util.CompareUtil.compareSafely;
import static org.apache.felix.http.base.internal.util.CompareUtil.equalSafely;

import java.util.List;
import java.util.regex.Pattern;


/**
 * {@code SearchPath} represents a path in {@code PriorityTrieMultimap}.
 * <p>
 * {@code SearchPath} are based on path names and support different wildcard
 * mechanisms. {@code SearchPath}s can be defined in three ways:
 * <ul>
 *      <li>as exact path
 *      <li>as wildcard path (path mapping)
 *      <li>as extension wildcard path (extension mapping)
 * </ul>
 * <p>
 * For searching in a {@code PriorityTrieMultimap} a {@code SearchPath}s
 * defines a matching relationship with other {@code SearchPath}s. An exact
 * path matches only paths with exactly the same path segments, wildcard paths
 * match paths that start with the same path segments and extension wildcard
 * paths match paths that start with the same path segments and have equal
 * extension.
 * <p>
 * To define the tree structure of the {@code PriorityTrieMultimap},
 * {@code SearchPath}s define a prefix relationship to other {@code SearchPath}s.
 * To determine if a {@code SearchPath} is a prefix of an other
 * {@code SearchPath}, the following rules are used:
 * <ul>
 *      <li>the other {@code SearchPath} starts with the same path segments as
 *          the prefixing path
 *      <li>both {@code SearchPath}s have the same extension value
 *      <li>a wildcard {@code SearchPath} is a prefix of the exact
 *          {@code SearchPath} with the same path segments
 * </ul>
 * In addition nodes do not inherit the color value of a node at an exact
 * matching path. In this case the color of next wildcard parent is passed on.
 *
 * (see {@link PriorityTrieMultimap})
 */
public class SearchPath implements Comparable<SearchPath>
{
    private static final String MAX_CHAR = String.valueOf(Character.MAX_VALUE);
    private static final char SLASH = '/';
    private static final String WILDCARD = "/*";
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("/\\*\\.(?=[^.\\s]+$)");
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\.(?=[^.\\s]+$)");

    private final String path;
    private final String extension;
    private final boolean isWildcard;

    SearchPath(String path, String extension, boolean isWildcard)
    {
        this.path = validatePath(path);
        this.extension = extension;
        this.isWildcard = isWildcard;
    }

    /**
     * Creates a {@code SearchPath} from a pattern.
     * <p>
     * Patterns are interpreted in the following way:
     * <ul>
     *      <li>if the pattern ends with "/*", a wildcard {@code SearchPath} is
     *          created
     *      <li>if the pattern ends with "*." followed by an extension, a
     *          wildcard {@code SearchPath} with the given extension is created
     *      <li>otherwise a {@code SearchPath} for an exact path is created
     * </ul>
     *
     * @param path a pattern defining the path
     *
     * @return a {@code SearchPath} based on the specified extension pattern
     */
    public static SearchPath forPattern(String pattern)
    {
        List<String> pathComponents = splitPath(pattern);
        String path = pathComponents.get(0);
        boolean isWildcard = pathComponents.size() == 2;
        String extension = isWildcard ? pathComponents.get(1) : null;

        return new SearchPath(path, extension, isWildcard);
    }

    /**
     * Creates an exact {@code SearchPath}.
     *
     * @param path the exact path
     * @return a {@code SearchPath} with an exact path
     */
    public static SearchPath forPath(String path)
    {
        return new SearchPath(path, null, false);
    }

    /**
     * Creates a {@code SearchPath} with an extension.
     * <p>
     * The specified path must have an extension, i.e. end in a segment
     * separated by a dot.
     *
     * @param path the exact path, ending in an extension
     *
     * @return a {@code SearchPath} with an extension or null, if the specified
     *          path does not have an extension
     */
    public static SearchPath forExtensionPath(String path)
    {
        String[] urlComponents = EXTENSION_PATTERN.split(path);
        if (urlComponents.length < 2)
        {
            return null;
        }
        return new SearchPath(urlComponents[0], urlComponents[1], false);
    }

    private static List<String> splitPath(String path)
    {

        int wildcardLength = WILDCARD.length();
        if (path.endsWith(WILDCARD))
        {
            return asList(path.substring(0, path.length() - wildcardLength), null);
        }

        return asList(WILDCARD_PATTERN.split(path));
    }

    /**
     * Returns the least {@code SearchPath} that is not a child of this {@code SearchPath}.
     *
     * @return the least {@code SearchPath} that is not a child of this
     *          {@code SearchPath}
     */
    public SearchPath childBound()
    {
        return new SearchPath(incrementPrefix(path), extension, isWildcard);
    }

    private String incrementPrefix(String prefix)
    {
        if (prefix.isEmpty())
        {
            return MAX_CHAR;
        }

        char[] charArray = prefix.toCharArray();
        int lastIndex = charArray.length - 1;
        if (charArray[lastIndex] == Character.MAX_VALUE)
        {
            throw new IllegalArgumentException("Unsupported character in path (Character.MAX_VALUE)");
        }
        charArray[lastIndex] += 1;

        return new String(charArray);
    }

    public boolean isPrefix(SearchPath other)
    {
        if (!equalSafely(extension, other.extension))
        {
            return false;
        }

        // wildcard path is parent of exact path
        if (path.equals(other.path))
        {
            return isWildcard || !other.isWildcard;
        }
        return other.path.startsWith(path);
    }

    public boolean matches(SearchPath other)
    {
        if (!isWildcard)
        {
            return path.equals(other.path) && equalSafely(extension, other.extension);
        }

        if (!isPrefix(other))
        {
            return false;
        }

        return path.length() == other.path.length()
            // ensure only full path segments match
            || other.path.charAt(path.length()) == SLASH;
    }

    boolean hasDominantColor()
    {
        return isWildcard;
    }

    private String validatePath(String path)
    {
        if (path == null)
        {
            throw new NullPointerException("Path must not be null");
        }

        return path;
    }

    public String getPath()
    {
        return path;
    }

    public String getExtension()
    {
        return extension;
    }

    public boolean isWildcard()
    {
        return isWildcard;
    }

    @Override
    public int compareTo(SearchPath other)
    {
        int extensionComparison = compareSafely(extension, other.extension);
        if (extensionComparison != 0)
        {
            return extensionComparison;
        }

        int pathComparison = path.compareTo(other.path);
        if (pathComparison != 0)
        {
            return pathComparison;
        }

        if (isWildcard == other.isWildcard)
        {
            return 0;
        }

        return isPrefix(other) ? -1 : 1;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        result = prime * result + (isWildcard ? 1231 : 1237);
        result = prime * result + path.hashCode();
        return result;
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

        if (!(obj instanceof SearchPath))
        {
            return false;
        }

        SearchPath other = (SearchPath) obj;

        boolean extensionEquals = extension == null ?
            other.extension == null :
            extension.equals(other.extension);

        return path.equals(other.path) &&
            extensionEquals &&
            isWildcard == other.isWildcard;
    }

    @Override
    public String toString()
    {
        return path + (isWildcard ? "*" : "");
    }
}
