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

import static org.apache.felix.http.base.internal.handler.trie.CompareUtil.compareSafely;

public final class ColoredValue<V extends Comparable<V>, C extends Comparable<C>> implements Comparable<ColoredValue<V, C>>
{
    private final C color;
    private final V value;

    ColoredValue(V value, C color)
    {
        this.color = color;
        this.value = value;
    }

    public C getColor()
    {
        return color;
    }

    public V getValue()
    {
        return value;
    }

    @Override
    public int compareTo(ColoredValue<V, C> other)
    {
        int colorComparison = compareSafely(color, other.color);
        return colorComparison == 0 ? value.compareTo(other.value) : colorComparison;
    }

    @Override
    public int hashCode()
    {
        return value.hashCode();
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

        if (!(obj instanceof ColoredValue))
        {
            return false;
        }

        try
        {
            @SuppressWarnings("unchecked")
            ColoredValue<V, C> other = (ColoredValue<V, C>) obj;

            return value.equals(other.value) && this.compareTo(other) == 0;
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }
}
