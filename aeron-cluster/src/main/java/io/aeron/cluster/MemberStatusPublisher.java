/*
 *  Copyright 2017 Real Logic Ltd.
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
package io.aeron.cluster;

import io.aeron.Publication;
import io.aeron.cluster.codecs.*;
import io.aeron.logbuffer.BufferClaim;

class MemberStatusPublisher
{
    private static final int SEND_ATTEMPTS = 3;

    private final BufferClaim bufferClaim = new BufferClaim();
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final RequestVoteEncoder requestVoteEncoder = new RequestVoteEncoder();
    private final VoteEncoder voteEncoder = new VoteEncoder();
    private final AppendedPositionEncoder appendedPositionEncoder = new AppendedPositionEncoder();
    private final CommitPositionEncoder commitPositionEncoder = new CommitPositionEncoder();

    public boolean requestVote(
        final Publication publication,
        final long candidateTermId,
        final long lastBaseLogPosition,
        final long lastTermPosition,
        final int candidateMemberId)
    {
        final int length = MessageHeaderEncoder.ENCODED_LENGTH + AppendedPositionEncoder.BLOCK_LENGTH;

        int attempts = SEND_ATTEMPTS;
        do
        {
            final long result = publication.tryClaim(length, bufferClaim);
            if (result > 0)
            {
                requestVoteEncoder
                    .wrapAndApplyHeader(bufferClaim.buffer(), bufferClaim.offset(), messageHeaderEncoder)
                    .candidateTermId(candidateTermId)
                    .lastBaseLogPosition(lastBaseLogPosition)
                    .lastTermPosition(lastTermPosition)
                    .candidateMemberId(candidateMemberId);

                bufferClaim.commit();

                return true;
            }

            checkResult(result);
        }
        while (--attempts > 0);

        return false;
    }

    public boolean vote(
        final Publication publication,
        final long candidateTermId,
        final long lastBaseLogPosition,
        final long lastTermPosition,
        final int candidateMemberId,
        final int followerMemberId,
        final boolean vote)
    {
        final int length = MessageHeaderEncoder.ENCODED_LENGTH + AppendedPositionEncoder.BLOCK_LENGTH;

        int attempts = SEND_ATTEMPTS;
        do
        {
            final long result = publication.tryClaim(length, bufferClaim);
            if (result > 0)
            {
                voteEncoder
                    .wrapAndApplyHeader(bufferClaim.buffer(), bufferClaim.offset(), messageHeaderEncoder)
                    .candidateTermId(candidateTermId)
                    .lastBaseLogPosition(lastBaseLogPosition)
                    .lastTermPosition(lastTermPosition)
                    .candidateMemberId(candidateMemberId)
                    .followerMemberId(followerMemberId)
                    .vote(vote ? BooleanType.TRUE : BooleanType.FALSE);

                bufferClaim.commit();

                return true;
            }

            checkResult(result);
        }
        while (--attempts > 0);

        return false;
    }

    public boolean appendedPosition(
        final Publication publication, final long termPosition, final long leadershipTermId, final int followerMemberId)
    {
        final int length = MessageHeaderEncoder.ENCODED_LENGTH + AppendedPositionEncoder.BLOCK_LENGTH;

        int attempts = SEND_ATTEMPTS;
        do
        {
            final long result = publication.tryClaim(length, bufferClaim);
            if (result > 0)
            {
                appendedPositionEncoder
                    .wrapAndApplyHeader(bufferClaim.buffer(), bufferClaim.offset(), messageHeaderEncoder)
                    .termPosition(termPosition)
                    .leadershipTermId(leadershipTermId)
                    .followerMemberId(followerMemberId);

                bufferClaim.commit();

                return true;
            }

            checkResult(result);
        }
        while (--attempts > 0);

        return false;
    }

    public boolean commitPosition(
        final Publication publication,
        final long termPosition,
        final long leadershipTermId,
        final int leaderMemberId,
        final int logSessionId)
    {
        final int length = MessageHeaderEncoder.ENCODED_LENGTH + CommitPositionEncoder.BLOCK_LENGTH;

        int attempts = SEND_ATTEMPTS;
        do
        {
            final long result = publication.tryClaim(length, bufferClaim);
            if (result > 0)
            {
                commitPositionEncoder
                    .wrapAndApplyHeader(bufferClaim.buffer(), bufferClaim.offset(), messageHeaderEncoder)
                    .termPosition(termPosition)
                    .leadershipTermId(leadershipTermId)
                    .leaderMemberId(leaderMemberId)
                    .logSessionId(logSessionId);

                bufferClaim.commit();

                return true;
            }

            checkResult(result);
        }
        while (--attempts > 0);

        return false;
    }

    private static void checkResult(final long result)
    {
        if (result == Publication.CLOSED || result == Publication.MAX_POSITION_EXCEEDED)
        {
            throw new IllegalStateException("Unexpected publication state: " + result);
        }
    }
}
