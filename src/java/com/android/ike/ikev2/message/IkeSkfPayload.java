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

package com.android.ike.ikev2.message;

import android.annotation.Nullable;

import com.android.ike.ikev2.crypto.IkeCipher;
import com.android.ike.ikev2.crypto.IkeMacIntegrity;
import com.android.ike.ikev2.exceptions.IkeProtocolException;
import com.android.ike.ikev2.exceptions.InvalidSyntaxException;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

/**
 * IkeSkfPayload represents an Encrypted and Authenticated Fragment Payload.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7383">RFC 7383, Internet Key Exchange Protocol
 *     Version 2 (IKEv2) Message Fragmentation</a>
 */
public final class IkeSkfPayload extends IkeSkPayload {
    private static final int SKF_HEADER_LEN = 4;

    /** Current Fragment message number, starting from 1 */
    public final int fragmentNum;
    /** Number of Fragment messages into which the original message was divided */
    public final int totalFragments;

    /**
     * Construct an instance of IkeSkfPayload by authenticating and decrypting an incoming packet.
     *
     * <p>SKF Payload with invalid fragmentNum or invalid totalFragments, or cannot be authenticated
     * or decrypted MUST be discarded
     *
     * @param critical indicates if it is a critical payload.
     * @param message the byte array contains the whole IKE message.
     * @param integrityMac the negotiated integrity algorithm.
     * @param decryptCipher the negotiated encryption algorithm.
     * @param integrityKey the negotiated integrity algorithm key.
     * @param decryptKey the negotiated decryption key.
     */
    IkeSkfPayload(
            boolean critical,
            byte[] message,
            @Nullable IkeMacIntegrity integrityMac,
            IkeCipher decryptCipher,
            byte[] integrityKey,
            byte[] decryptKey)
            throws IkeProtocolException, GeneralSecurityException {
        super(
                true /*isSkf*/,
                critical,
                IkeHeader.IKE_HEADER_LENGTH + GENERIC_HEADER_LENGTH + SKF_HEADER_LEN,
                message,
                integrityMac,
                decryptCipher,
                integrityKey,
                decryptKey);

        // TODO: Support constructing IkeEncryptedPayloadBody using AEAD.

        ByteBuffer inputBuffer = ByteBuffer.wrap(message);
        inputBuffer.get(new byte[IkeHeader.IKE_HEADER_LENGTH + GENERIC_HEADER_LENGTH]);

        fragmentNum = Short.toUnsignedInt(inputBuffer.getShort());
        totalFragments = Short.toUnsignedInt(inputBuffer.getShort());

        if (fragmentNum < 1 || totalFragments < 1 || fragmentNum > totalFragments) {
            throw new InvalidSyntaxException(
                    "Received invalid Fragment Number or Total Fragments Number. Fragment Number: "
                            + fragmentNum
                            + "  Total Fragments: "
                            + totalFragments);
        }
    }

    // TODO: Support constructing outbound SKF Payload.

    // TODO: Override encodeToByteBuffer and getPayloadLength

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "Encrypted and Authenticated Fragment Payload";
    }
}