/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.base.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some convenience utilities to deal with path patterns.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PatternUtil
{

    public static String convertToRegEx(String pattern)
    {
        String result = pattern;
        // see Servlet 3.0, section 12.2
        // - replace '*.' prefixes with a regex that matches extensions...
        result = result.replaceFirst("^\\Q*.\\E(.*)$", "\\^(.*)(\\\\.\\\\Q$1\\\\E)\\$");
        // - replace '/*' suffixes with a regex that matches paths (actually,
        //   the path should also start with a leading slash, but we're a bit
        //   more liberal on this one)...
        result = result.replaceFirst("^(.*)\\Q/*\\E$", "\\^($1)(|/.*)\\$");
        return result;
    }

    // check for valid symbolic name
    public static boolean isValidSymbolicName(final String name)
    {
        if ( name == null || name.isEmpty() )
        {
            return false;
        }
        boolean valid = true;
        boolean expectToken = false;
        boolean done = false;
        final StringTokenizer st = new StringTokenizer(name, ".", true);
        while ( !done && st.hasMoreTokens() )
        {
            final String token = st.nextToken();
            if ( expectToken )
            {
                if ( !".".equals(token) )
                {
                    valid = false;
                    done = true;
                }
                else
                {
                    expectToken = false;
                }
            }
            else
            {
                if ( ".".equals(token) )
                {
                    valid = false;
                    done = true;
                }
                else
                {
                    int i = 0;
                    while ( i < token.length() && valid )
                    {
                        final char c = token.charAt(i);
                        i++;
                        if ( c >= 'a' && c <= 'z' )
                        {
                            continue;
                        }
                        if ( c >= 'A' && c <= 'Z' )
                        {
                            continue;
                        }
                        if ( c >= '0' && c <= '9' )
                        {
                            continue;
                        }
                        if ( c == '-' || c == '_' )
                        {
                            continue;
                        }
                        valid = false;
                        done = true;
                    }
                }
                expectToken = true;
            }
        }
        if ( !expectToken )
        {
            valid = false;
        }

        return valid;
    }

    /**
     * Compares {@link Pattern}s based on a set of simple rules:
     * <ol>
     * <li>exact matches go first;</li>
     * <li>followed by wildcard path matches;</li>
     * <li>lastly all wildcard extension matches.</li>
     * </ol>
     * <p>
     * Equal matches will first be sorted on length in descending order (longest patterns first),
     * and in case of equal lengths, they are sorted in natural (ascending) order.
     * </p>
     */
    public static class PatternComparator implements Comparator<Pattern>
    {
        @Override
        public int compare(Pattern p1, Pattern p2)
        {
            String ps1 = p1.pattern();
            String ps2 = p2.pattern();

            // Sorts wildcard path matches before wildcard extension matches...
            int r;
            if (isWildcardPath(ps1))
            {
                if (isWildcardPath(ps2))
                {
                    // Descending on length...
                    r = ps2.length() - ps1.length();
                }
                else
                {
                    // Exact matches go first...
                    r = isWildcardExtension(ps2) ? -1 : 1;
                }
            }
            else if (isWildcardExtension(ps1))
            {
                if (isWildcardExtension(ps2))
                {
                    // Descending on length...
                    r = ps2.length() - ps1.length();
                }
                else
                {
                    // Wildcard paths & exact matches go first...
                    r = 1;
                }
            }
            else
            {
                if (isWildcardExtension(ps2) || isWildcardPath(ps2))
                {
                    // Exact matches go first...
                    r = -1;
                }
                else
                {
                    // Descending on length...
                    r = ps2.length() - ps1.length();
                }
            }

            if (r == 0)
            {
                // In case of a draw, ensure we sort in a predictable (ascending) order...
                r = ps1.compareTo(ps2);
            }

            return r;
        }

        private boolean isWildcardExtension(String p)
        {
            return p.startsWith("^(.*");
        }

        private boolean isWildcardPath(String p)
        {
            return p.startsWith("^(/");
        }
    }

    public static boolean isWildcardPattern(Pattern p)
    {
        return p.pattern().contains(".*");
    }
    
    /**
     * Performs the actual matching, yielding a list of either the first or all matching patterns.
     *
     * @param path the path to match, can be <code>null</code> in which case an empty string is
     *        used;
     * @param exact a list of exact patterns
     * @param wildcard a list of wildcard patterns
     * @param firstOnly <code>true</code> if only the first matching pattern should be returned,
     *        <code>false</code> if all matching patterns should be returned.
     * @return a list with matching patterns, never <code>null</code>.
     */
    public static List<Pattern> getAllMatches(String path, 
    		SortedSet<Pattern> exact, 
    		SortedSet<Pattern> wildcard,
    		boolean firstOnly)
    {
        path = (path == null) ? "" : path.trim();

        List<Pattern> result = new ArrayList<Pattern>();
        // Look for exact matches only, that is, those patterns without wildcards...
        for (Pattern p : exact)
        {
            Matcher matcher = p.matcher(path);
            // !!! we should always match the *entire* pattern, instead of the longest prefix...
            if (matcher.matches())
            {
            	result.add(p);
            	if(firstOnly)
            	{
            		return result;
            	}
            }
        }

        // Try to apply the wildcard patterns...
        for (Pattern p : wildcard)
        {
            Matcher matcher = p.matcher(path);
            if (matcher.find(0))
            {
            	result.add(p);
            	if(firstOnly)
            	{
            		return result;
            	}
            }
        }

        // Make sure the results are properly sorted...
        // TODO: check if this is still needed, patterns are added from SortedSets, i.e. they are already sorted according to the rules in Servlet Specification 3.0 
        Collections.sort(result, new PatternComparator());
        return result;
    }
}
