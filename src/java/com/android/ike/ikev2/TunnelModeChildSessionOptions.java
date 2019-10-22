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

package com.android.ike.ikev2;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.annotation.NonNull;
import android.net.LinkAddress;

import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttribute;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv4Address;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv4Dns;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv4Netmask;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv4Subnet;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv6Address;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv6Dns;
import com.android.ike.ikev2.message.IkeConfigPayload.ConfigAttributeIpv6Subnet;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

/**
 * This class contains all user provided configuration options for negotiating a tunnel mode Child
 * Session.
 */
public final class TunnelModeChildSessionOptions extends ChildSessionOptions {
    private final ConfigAttribute[] mConfigRequests;

    private TunnelModeChildSessionOptions(
            IkeTrafficSelector[] localTs,
            IkeTrafficSelector[] remoteTs,
            ChildSaProposal[] proposals,
            ConfigAttribute[] configRequests) {
        super(localTs, remoteTs, proposals, false /*isTransport*/);
        mConfigRequests = configRequests;
    }

    /** Package private */
    ConfigAttribute[] getConfigurationRequests() {
        return mConfigRequests;
    }

    /** This class can be used to incrementally construct a TunnelModeChildSessionOptions. */
    public static final class Builder extends ChildSessionOptions.Builder {
        private static final int IPv4_DEFAULT_PREFIX_LEN = 32;

        private boolean mHasIp4AddressRequest;
        private List<ConfigAttribute> mConfigRequestList;

        /** Create a Builder for negotiating a transport mode Child Session. */
        public Builder() {
            super();
            mHasIp4AddressRequest = false;
            mConfigRequestList = new LinkedList<>();
        }

        /**
         * Adds an Child SA proposal to TunnelModeChildSessionOptions being built.
         *
         * @param proposal Child SA proposal.
         * @return Builder this, to facilitate chaining.
         * @throws IllegalArgumentException if input proposal is not a Child SA proposal.
         */
        public Builder addSaProposal(@NonNull ChildSaProposal proposal) {
            validateAndAddSaProposal(proposal);
            return this;
        }

        /**
         * Adds internal address requests to TunnelModeChildSessionOptions being built.
         *
         * @param addressFamily the address family. Only OsConstants.AF_INET and
         *     OsConstants.AF_INET6 are allowed.
         * @param numOfRequest the number of requests for this type of address.
         * @return Builder this, to facilitate chaining.
         */
        public Builder addInternalAddressRequest(int addressFamily, int numOfRequest) {
            if (addressFamily == AF_INET) {
                mHasIp4AddressRequest = true;
                for (int i = 0; i < numOfRequest; i++) {
                    mConfigRequestList.add(new ConfigAttributeIpv4Address());
                }
                return this;
            } else if (addressFamily == AF_INET6) {
                for (int i = 0; i < numOfRequest; i++) {
                    mConfigRequestList.add(new ConfigAttributeIpv6Address());
                }
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Adds specific internal address request to TunnelModeChildSessionOptions being built.
         *
         * @param address the requested address.
         * @param prefixLen length of the InetAddress prefix. When requesting an IPv4 address,
         *     prefixLen MUST be 32.
         * @return Builder this, to facilitate chaining.
         */
        public Builder addInternalAddressRequest(@NonNull InetAddress address, int prefixLen) {
            if (address instanceof Inet4Address) {
                if (prefixLen != IPv4_DEFAULT_PREFIX_LEN) {
                    throw new IllegalArgumentException("Invalid IPv4 prefix length: " + prefixLen);
                }
                mHasIp4AddressRequest = true;
                mConfigRequestList.add(new ConfigAttributeIpv4Address((Inet4Address) address));
                return this;
            } else if (address instanceof Inet6Address) {
                mConfigRequestList.add(
                        new ConfigAttributeIpv6Address(new LinkAddress(address, prefixLen)));
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address " + address);
            }
        }

        /**
         * Adds internal DNS server requests to TunnelModeChildSessionOptions being built.
         *
         * @param addressFamily the address family. Only OsConstants.AF_INET and
         *     OsConstants.AF_INET6 are allowed.
         * @param numOfRequest the number of requests for this type of address.
         * @return Builder this, to facilitate chaining.
         */
        public Builder addInternalDnsServerRequest(int addressFamily, int numOfRequest) {
            if (addressFamily == AF_INET) {
                for (int i = 0; i < numOfRequest; i++) {
                    mConfigRequestList.add(new ConfigAttributeIpv4Dns());
                }
                return this;
            } else if (addressFamily == AF_INET6) {
                for (int i = 0; i < numOfRequest; i++) {
                    mConfigRequestList.add(new ConfigAttributeIpv6Dns());
                }
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Adds internal DNS server requests to TunnelModeChildSessionOptions being built.
         *
         * @param address the requested DNS server address.
         * @return Builder this, to facilitate chaining.
         */
        public Builder addInternalDnsServerRequest(@NonNull InetAddress address) {
            if (address instanceof Inet4Address) {
                mConfigRequestList.add(new ConfigAttributeIpv4Dns((Inet4Address) address));
                return this;
            } else if (address instanceof Inet6Address) {
                mConfigRequestList.add(new ConfigAttributeIpv6Dns((Inet6Address) address));
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address " + address);
            }
        }

        /**
         * Adds internal subnet requests to TunnelModeChildSessionOptions being built.
         *
         * @param addressFamily the address family. Only OsConstants.AF_INET and
         *     OsConstants.AF_INET6 are allowed.
         * @param numOfRequest the number of requests for this type of address.
         * @return Builder this, to facilitate chaining.
         */
        public Builder addInternalSubnetRequest(int addressFamily, int numOfRequest) {
            if (addressFamily == AF_INET) {
                for (int i = 0; i < numOfRequest; i++) {
                    mConfigRequestList.add(new ConfigAttributeIpv4Subnet());
                }
                return this;
            } else if (addressFamily == AF_INET6) {
                for (int i = 0; i < numOfRequest; i++) {
                    mConfigRequestList.add(new ConfigAttributeIpv6Subnet());
                }
                return this;
            } else {
                throw new IllegalArgumentException("Invalid address family: " + addressFamily);
            }
        }

        /**
         * Validates, builds and returns the TunnelModeChildSessionOptions.
         *
         * @return the validated TunnelModeChildSessionOptions.
         * @throws IllegalArgumentException if no Child SA proposal is provided.
         */
        public TunnelModeChildSessionOptions build() {
            validateOrThrow();

            if (mHasIp4AddressRequest) {
                mConfigRequestList.add(new ConfigAttributeIpv4Netmask());
            }

            return new TunnelModeChildSessionOptions(
                    mLocalTsList.toArray(new IkeTrafficSelector[mLocalTsList.size()]),
                    mRemoteTsList.toArray(new IkeTrafficSelector[mRemoteTsList.size()]),
                    mSaProposalList.toArray(new ChildSaProposal[mSaProposalList.size()]),
                    mConfigRequestList.toArray(new ConfigAttribute[mConfigRequestList.size()]));
        }
    }

    // TODO: b/140644654 Add API for configuration requests.
}