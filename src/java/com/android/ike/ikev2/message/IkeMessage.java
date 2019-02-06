/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.ike.ikev2.message.IkePayload.PayloadType;

import android.util.Pair;

import com.android.ike.ikev2.exceptions.IkeException;
import com.android.ike.ikev2.exceptions.InvalidSyntaxException;
import com.android.ike.ikev2.exceptions.UnsupportedCriticalPayloadException;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.Provider;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * IkeMessage represents an IKE message.
 *
 * <p>It contains all attributes and provides methods for encoding, decoding, encrypting and
 * decrypting.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2).
 */
public final class IkeMessage {

    // Currently use Bouncy Castle as crypto security provider
    static final Provider SECURITY_PROVIDER = new BouncyCastleProvider();

    public final IkeHeader ikeHeader;
    public final List<IkePayload> ikePayloadList;

    /**
     * Conctruct an instance of IkeMessage. It is called by decode or for building outbound message.
     *
     * @param header the header of this IKE message
     * @param payloadList the list of decoded IKE payloads in this IKE message
     */
    public IkeMessage(IkeHeader header, List<IkePayload> payloadList) {
        ikeHeader = header;
        ikePayloadList = payloadList;
    }

    /**
     * Decrypt and decode encrypted IKE message body and create an instance of IkeMessage.
     *
     * <p> This method catches all RuntimeException during decoding incoming IKE packet.
     *
     * @param header the IKE header that is decoded but not validated.
     * @param inputPacket the byte array containing the whole IKE message.
     * @param integrityMac the initialized Message Authentication Code (MAC) for integrity check.
     * @param checksumLen the length of integrity checksum.
     * @param decryptCipher the uninitialized Cipher for doing decryption.
     * @param dKey the decryption key.
     * @param ivLen the length of Initialization Vector.
     * @return the IkeMessage instance.
     * @throws IkeException if there is any protocol error.
     * @throws IOException if there is any error during integrity check or decryption.
     */
    public static IkeMessage decode(
            IkeHeader header,
            byte[] inputPacket,
            Mac integrityMac,
            int checksumLen,
            Cipher decryptCipher,
            SecretKey dKey,
            int ivLen)
            throws IkeException, IOException {

        header.checkValidOrThrow(inputPacket.length);

        try {
            Pair<IkeSkPayload, Integer> pair =
                    IkePayloadFactory.getIkeSkPayload(
                            inputPacket, integrityMac, checksumLen, decryptCipher, dKey, ivLen);
            IkeSkPayload skPayload = pair.first;
            int firstPayloadType = pair.second;

            List<IkePayload> supportedPayloadList =
                    decodePayloadList(firstPayloadType, skPayload.unencryptedPayloads);
            return new IkeMessage(header, supportedPayloadList);
        } catch (NegativeArraySizeException | BufferUnderflowException e) {
            // Invalid length error when parsing payload bodies.
            throw new InvalidSyntaxException("Malformed IKE Payload");
        }
    }

    /**
     * Decode unencrypted IKE message body and create an instance of IkeMessage.
     *
     * <p> This method catches all RuntimeException during decoding incoming IKE packet.
     *
     * @param header the IKE header that is decoded but not validated.
     * @param inputPacket the byte array contains the whole IKE message.
     * @return the IkeMessage instance.
     * @throws IkeException if there is any protocol error.
     */
    public static IkeMessage decode(IkeHeader header, byte[] inputPacket) throws IkeException {

        header.checkValidOrThrow(inputPacket.length);

        byte[] unencryptedPayloads =
                Arrays.copyOfRange(inputPacket, IkeHeader.IKE_HEADER_LENGTH, inputPacket.length);

        try {
            List<IkePayload> supportedPayloadList =
                    decodePayloadList(header.nextPayloadType, unencryptedPayloads);
            return new IkeMessage(header, supportedPayloadList);
        } catch (NegativeArraySizeException | BufferUnderflowException e) {
            // Invalid length error when parsing payload bodies.
            throw new InvalidSyntaxException("Malformed IKE Payload");
        }
    }

    private static List<IkePayload> decodePayloadList(
            @PayloadType int firstPayloadType, byte[] unencryptedPayloads) throws IkeException {
        ByteBuffer inputBuffer = ByteBuffer.wrap(unencryptedPayloads);
        int currentPayloadType = firstPayloadType;
        // For supported payload
        List<IkePayload> supportedPayloadList = new LinkedList<>();
        // For unsupported critical payload
        List<Integer> unsupportedCriticalPayloadList = new LinkedList<>();

        while (currentPayloadType != IkePayload.PAYLOAD_TYPE_NO_NEXT) {
                Pair<IkePayload, Integer> pair =
                        IkePayloadFactory.getIkePayload(currentPayloadType, inputBuffer);
                IkePayload payload = pair.first;

                if (!(payload instanceof IkeUnsupportedPayload)) {
                    supportedPayloadList.add(payload);
                } else if (payload.isCritical) {
                    unsupportedCriticalPayloadList.add(payload.payloadType);
                }
                // Simply ignore unsupported uncritical payload.

                currentPayloadType = pair.second;
        }

        if (inputBuffer.remaining() > 0) {
            throw new InvalidSyntaxException(
                    "Malformed IKE Payload: Unexpected bytes at the end of packet.");
        }

        if (unsupportedCriticalPayloadList.size() > 0) {
            throw new UnsupportedCriticalPayloadException(unsupportedCriticalPayloadList);
        }
        return supportedPayloadList;
    }

    static Provider getSecurityProvider() {
        return SECURITY_PROVIDER;
    }

    /**
     * Encode all payloads to a byte array.
     *
     * @return byte array contains all encoded payloads
     */
    public byte[] encodePayloads() {
        if (ikePayloadList.isEmpty()) {
            return new byte[0];
        }

        int payloadLengthSum = 0;
        for (IkePayload payload : ikePayloadList) {
            payloadLengthSum += payload.getPayloadLength();
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(payloadLengthSum);

        for (int i = 0; i < ikePayloadList.size() - 1; i++) {
            ikePayloadList
                    .get(i)
                    .encodeToByteBuffer(ikePayloadList.get(i + 1).payloadType, byteBuffer);
        }
        ikePayloadList
                .get(ikePayloadList.size() - 1)
                .encodeToByteBuffer(IkePayload.PAYLOAD_TYPE_NO_NEXT, byteBuffer);

        return byteBuffer.array();
    }

    // TODO: Add a method that takes cyptographic algorithms and parameters to encrypt all payloads
    // to a byte array.

    /**
     * Encode entire IKE message to a byte array.
     *
     * @param encodedIkeBody IKE message body in byte array. IKE message body is encrypted and
     *     integrity protected except in an IKE_SA_INIT message.
     * @return the entire encoded IKE message as a byte array.
     */
    public byte[] encode(byte[] encodedIkeBody) {
        ByteBuffer outputBuffer =
                ByteBuffer.allocate(IkeHeader.IKE_HEADER_LENGTH + encodedIkeBody.length);
        ikeHeader.encodeToByteBuffer(outputBuffer);
        outputBuffer.put(encodedIkeBody);
        return outputBuffer.array();
    }
}
