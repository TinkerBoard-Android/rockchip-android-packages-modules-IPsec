/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.ipsec.ike;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.net.InetAddresses;

import org.junit.Test;

import java.net.InetAddress;
import java.util.Arrays;

public final class ChildSessionParamsTest {
    private static final int NUM_TS = 2;

    @Test
    public void testBuild() throws Exception {
        ChildSaProposal saProposal =
                new ChildSaProposal.Builder()
                        .addEncryptionAlgorithm(
                                SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12,
                                SaProposal.KEY_LEN_AES_128)
                        .build();
        ChildSessionParams sessionParams =
                new TunnelModeChildSessionParams.Builder().addSaProposal(saProposal).build();

        assertArrayEquals(new SaProposal[] {saProposal}, sessionParams.getSaProposalsInternal());
        assertEquals(NUM_TS, sessionParams.getLocalTrafficSelectorsInternal().length);
        assertEquals(NUM_TS, sessionParams.getRemoteTrafficSelectorsInternal().length);
        assertEquals(Arrays.asList(sessionParams.getLocalTrafficSelectorsInternal()),
                Arrays.asList(getExpectedDefaultIpv4Ts(), getExpectedDefaultIpv6Ts()));
        assertEquals(Arrays.asList(sessionParams.getRemoteTrafficSelectorsInternal()),
                Arrays.asList(getExpectedDefaultIpv4Ts(), getExpectedDefaultIpv6Ts()));
        assertFalse(sessionParams.isTransportMode());
    }

    @Test
    public void testBuildWithoutSaProposal() throws Exception {
        try {
            new TunnelModeChildSessionParams.Builder().build();
            fail("Expected to fail due to the absence of SA proposal.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private IkeTrafficSelector getExpectedDefaultIpv4Ts() {
        final InetAddress tsStartAddress = InetAddresses.parseNumericAddress("0.0.0.0");
        final InetAddress tsEndAddress = InetAddresses.parseNumericAddress("255.255.255.255");

        return new IkeTrafficSelector(0, 65535, tsStartAddress, tsEndAddress);
    }

    private IkeTrafficSelector getExpectedDefaultIpv6Ts() {
        final InetAddress tsStartAddress = InetAddresses.parseNumericAddress("::");
        final InetAddress tsEndAddress = InetAddresses.parseNumericAddress(
                "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");

        return new IkeTrafficSelector(0, 65535, tsStartAddress, tsEndAddress);
    }
}
