/*
 * Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.driver;

/**
 * Aeron client library tracker.
 */
public class AeronClient implements DriverManagedResource
{
    private final long clientId;
    private final long clientLivenessTimeoutNs;
    private long timeOfLastKeepaliveNs;
    private boolean reachedEndOfLife = false;

    public AeronClient(final long clientId, final long clientLivenessTimeoutNs, final long nowNs)
    {
        this.clientId = clientId;
        this.clientLivenessTimeoutNs = clientLivenessTimeoutNs;
        this.timeOfLastKeepaliveNs = nowNs;
    }

    public void close()
    {
    }

    public long clientId()
    {
        return clientId;
    }

    public long timeOfLastKeepalive()
    {
        return timeOfLastKeepaliveNs;
    }

    public void timeOfLastKeepalive(final long nowNs)
    {
        timeOfLastKeepaliveNs = nowNs;
    }

    public boolean hasTimedOut(final long nowNs)
    {
        return nowNs > (timeOfLastKeepaliveNs + clientLivenessTimeoutNs);
    }

    public void onTimeEvent(final long timeNs, final long timeMs, final DriverConductor conductor)
    {
        if (timeNs > (timeOfLastKeepaliveNs + clientLivenessTimeoutNs))
        {
            reachedEndOfLife = true;
        }
    }

    public boolean hasReachedEndOfLife()
    {
        return reachedEndOfLife;
    }
}
