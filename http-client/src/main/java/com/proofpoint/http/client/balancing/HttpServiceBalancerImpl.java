/*
 * Copyright 2013 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.http.client.balancing;

import com.google.common.annotations.Beta;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.proofpoint.http.client.balancing.HttpServiceBalancerStats.Status;

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpServiceBalancerImpl
        implements HttpServiceBalancer
{
    private final AtomicReference<Set<URI>> httpUris = new AtomicReference<>((Set<URI>) ImmutableSet.<URI>of());
    private final String description;
    private final HttpServiceBalancerStats httpServiceBalancerStats;
    private final Ticker ticker;

    public HttpServiceBalancerImpl(String description, HttpServiceBalancerStats httpServiceBalancerStats)
    {
        this(description, httpServiceBalancerStats, Ticker.systemTicker());
    }

    HttpServiceBalancerImpl(String description, HttpServiceBalancerStats httpServiceBalancerStats, Ticker ticker)
    {
        this.description = checkNotNull(description, "description is null");
        this.httpServiceBalancerStats = checkNotNull(httpServiceBalancerStats, "httpServiceBalancerStats is null");
        this.ticker = checkNotNull(ticker, "ticker is null");
    }

    @Override
    public HttpServiceAttempt createAttempt()
    {
        return new HttpServiceAttemptImpl(ImmutableSet.<URI>of());
    }

    @Beta
    public void updateHttpUris(Set<URI> newHttpUris)
    {
        httpUris.set(ImmutableSet.copyOf(newHttpUris));
    }

    private class HttpServiceAttemptImpl
            implements HttpServiceAttempt
    {
        private final Set<URI> attempted;
        private final URI uri;
        private final long startTick;

        public HttpServiceAttemptImpl(Set<URI> attempted)
        {
            ArrayList<URI> httpUris = new ArrayList<>(HttpServiceBalancerImpl.this.httpUris.get());
            httpUris.removeAll(attempted);

            if (httpUris.isEmpty()) {
                httpUris = new ArrayList<>(HttpServiceBalancerImpl.this.httpUris.get());
                attempted = ImmutableSet.of();

                if (httpUris.isEmpty()) {
                    throw new ServiceUnavailableException(description);
                }
            }

            this.attempted = attempted;
            uri = httpUris.get(ThreadLocalRandom.current().nextInt(0, httpUris.size()));
            startTick = ticker.read();
        }

        @Override
        public URI getUri()
        {
            return uri;
        }

        @Override
        public void markGood()
        {
            httpServiceBalancerStats.responseTime(uri, Status.SUCCESS).add(ticker.read() - startTick, TimeUnit.NANOSECONDS);
        }

        @Override
        public void markBad(String failureCategory)
        {
            httpServiceBalancerStats.responseTime(uri, Status.FAILURE).add(ticker.read() - startTick, TimeUnit.NANOSECONDS);
            httpServiceBalancerStats.failure(uri, failureCategory).update(1);
        }

        @Override
        public HttpServiceAttempt next()
        {
            Set<URI> newAttempted = ImmutableSet.<URI>builder()
                    .add(uri)
                    .addAll(attempted)
                    .build();
            return new HttpServiceAttemptImpl(newAttempted);
        }
    }
}