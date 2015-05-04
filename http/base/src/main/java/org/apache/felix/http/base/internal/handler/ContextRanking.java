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
package org.apache.felix.http.base.internal.handler;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;

class ContextRanking implements Comparable<ContextRanking>
{
    private final long serviceId;
    private final int ranking;
    private final String path;

    ContextRanking()
    {
        this.serviceId = 0;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
    }

    ContextRanking(ServletContextHelperInfo info)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
    }

    public String getPath()
    {
        return path;
    }

    public long getServiceId()
    {
        return serviceId;
    }

    @Override
    public int compareTo(ContextRanking other)
    {
        // the context of the HttpService is the least element
        if (this.serviceId == 0 ^ other.serviceId == 0)
        {
            return this.serviceId == 0 ? -1 : 1;
        }

        final int result = Integer.compare(other.getPath().length(), this.getPath().length());
        if ( result == 0 ) {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = ( this.serviceId <= 0 && other.serviceId <= 0 ) ? -1 : 1;
                return reverseOrder * Long.compare(this.serviceId, other.serviceId);
            }

            return Integer.compare(other.ranking, this.ranking);
        }
        return result;
    }
}