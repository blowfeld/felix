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

    public static SearchPath forPattern(String pattern)
    {
        List<String> pathComponents = splitPath(pattern);
        String path = pathComponents.get(0);
        boolean isWildcard = pathComponents.size() == 2;
        String extension = isWildcard ? pathComponents.get(1) : null;

        return new SearchPath(path, extension, isWildcard);
    }

    public static SearchPath forPath(String url)
    {
        return new SearchPath(url, null, false);
    }

    public static SearchPath forExtensionPath(String url)
    {
        String[] urlComponents = EXTENSION_PATTERN.split(url);
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

    public SearchPath upperBound()
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

    public boolean isParentOf(SearchPath other)
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

        if (!isParentOf(other))
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
    public int compareTo(SearchPath o)
    {
        int extensionComparison = compareSafely(extension, o.extension);
        return extensionComparison != 0 ? extensionComparison : path.compareTo(o.path);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        result = prime * result + (isWildcard ? 1231 : 1237);
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
