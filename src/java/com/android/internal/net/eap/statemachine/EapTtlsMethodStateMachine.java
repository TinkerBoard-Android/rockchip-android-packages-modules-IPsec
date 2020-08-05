/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.net.eap.statemachine;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapData.EAP_IDENTITY;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_TTLS;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_RESPONSE;

import android.content.Context;
import android.net.eap.EapSessionConfig;
import android.net.eap.EapSessionConfig.EapTtlsConfig;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.crypto.TlsSessionFactory;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.message.EapData;
import com.android.internal.net.eap.message.EapData.EapMethod;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.ttls.EapTtlsAvp;
import com.android.internal.net.eap.message.ttls.EapTtlsTypeData.EapTtlsTypeDataDecoder;
import com.android.internal.net.eap.message.ttls.EapTtlsTypeData.EapTtlsTypeDataDecoder.DecodeResult;

import java.security.SecureRandom;

/**
 * EapTtlsMethodStateMachine represents the valid paths possible for the EAP-TTLS protocol
 *
 * <p>EAP-TTLS sessions will always follow the path:
 *
 * <p>Created --+--> Handshake --+--> Tunnel (EAP) --+--> Final
 *
 * <p>Note: EAP-TTLS will only be allowed to run once. The inner EAP instance will not be able to
 * select EAP-TTLS. This is handled in the tunnel state when a new EAP session config is created.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5281">RFC 5281, Extensible Authentication Protocol
 *     Tunneled Transport Layer Security Authenticated Protocol Version 0 (EAP-TTLSv0)</a>
 */
public class EapTtlsMethodStateMachine extends EapMethodStateMachine {

    private final Context mContext;
    private final EapSessionConfig mEapSessionConfig;
    private final EapTtlsConfig mEapTtlsConfig;
    private final EapTtlsTypeDataDecoder mTypeDataDecoder;
    private final SecureRandom mSecureRandom;
    private final TlsSessionFactory mTlsSessionFactory;

    public EapTtlsMethodStateMachine(
            Context context,
            EapSessionConfig eapSessionConfig,
            EapTtlsConfig eapTtlsConfig,
            SecureRandom secureRandom) {
        this(
                context,
                eapSessionConfig,
                eapTtlsConfig,
                secureRandom,
                new EapTtlsTypeDataDecoder(),
                new TlsSessionFactory());
    }

    @VisibleForTesting
    public EapTtlsMethodStateMachine(
            Context context,
            EapSessionConfig eapSessionConfig,
            EapTtlsConfig eapTtlsConfig,
            SecureRandom secureRandom,
            EapTtlsTypeDataDecoder typeDataDecoder,
            TlsSessionFactory tlsSessionFactory) {
        mContext = context;
        mEapSessionConfig = eapSessionConfig;
        mEapTtlsConfig = eapTtlsConfig;
        mTypeDataDecoder = typeDataDecoder;
        mSecureRandom = secureRandom;
        mTlsSessionFactory = tlsSessionFactory;

        transitionTo(new CreatedState());
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_TTLS;
    }

    @Override
    EapResult handleEapNotification(String tag, EapMessage message) {
        return EapStateMachine.handleNotification(tag, message);
    }

    /**
     * The created state verifies the start request before transitioning to phase 1 of EAP-TTLS
     * (RFC5281#7.1)
     */
    protected class CreatedState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        @Override
        public EapResult process(EapMessage message) {
            // TODO(b/160781895): Support decoding AVP's pre-tunnel in EAP-TTLS
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult decodeResult =
                    mTypeDataDecoder.decodeEapTtlsRequestPacket(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                LOG.e(mTAG, "Error parsing EAP-TTLS packet type data", decodeResult.eapError.cause);
                return decodeResult.eapError;
            } else if (!decodeResult.eapTypeData.isStart) {
                return new EapError(
                        new EapInvalidRequestException(
                                "Unexpected request received in EAP-TTLS: Received first request"
                                        + " without start bit set."));
            }

            return transitionAndProcess(new HandshakeState(), message);
        }
    }

    /**
     * The handshake (phase 1) state builds the tunnel for tunneled EAP authentication in phase 2
     *
     * <p>As per RFC5281#9.2.1, version negotiation occurs during the first exchange between client
     * and server. In other words, this is an implicit negotiation and is not handled independently.
     * In this case, the version will always be zero because that is the only currently supported
     * version of EAP-TTLS at the time of writing. The initiation of the handshake (RFC5281#7.1) is
     * the first response sent by the client.
     */
    protected class HandshakeState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        private static final int DEFAULT_VENDOR_ID = 0;

        @Override
        public EapResult process(EapMessage message) {
            // TODO(b/159929700): Implement handshake (phase 1) of EAP-TTLS (RFC5281#7.1)
            return handleEapSuccessFailureNotification(mTAG, message);
        }

        /**
         * Builds an EAP-MESSAGE AVP containing an EAP-Identity response
         *
         * <p>Note that this uses the EAP-Identity in the session config nested within EapTtlsConfig
         * which may be different than the identity in the top-level EapSessionConfig
         *
         * @param eapIdentifier the eap identifier for the response
         * @throws EapSilentException if an error occurs creating the eap message
         */
        @VisibleForTesting
        byte[] buildEapIdentityResponseAvp(int eapIdentifier) throws EapSilentException {
            EapData eapData =
                    new EapData(
                            EAP_IDENTITY, mEapTtlsConfig.getInnerEapSessionConfig().eapIdentity);
            EapMessage eapMessage = new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, eapData);
            return EapTtlsAvp.getEapMessageAvp(DEFAULT_VENDOR_ID, eapMessage.encode()).encode();
        }
    }

    /**
     * The tunnel state (phase 2) tunnels data produced by an inner EAP instance
     *
     * <p>The tunnel state creates an inner EAP instance via a new EAP state machine and handles
     * decryption and encryption of data using the previously established TLS tunnel (RFC5281#7.2)
     */
    protected class TunnelState extends EapMethodState {
        @Override
        public EapResult process(EapMessage message) {
            // TODO(b/159926139): Implement tunnel state (phase 2) of EAP-TTLS (RFC5281#7.2)
            return null;
        }
    }
}
