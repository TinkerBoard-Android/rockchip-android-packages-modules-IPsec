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

package com.android.ike.eap.message;

import static com.android.ike.TestUtils.hexStringToByteArray;

/**
 * EapTestMessageDefinitions provides byte[] encodings of commonly used EAP Messages.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 * Protocol (EAP)</a>
 */
public class EapTestMessageDefinitions {
    public static final String ID = "10";
    public static final int ID_INT = Integer.parseInt(ID, 16 /* radix */);

    // EAP-AKA Identity request
    public static final String EAP_REQUEST_TYPE_DATA = "050C010000";
    public static final byte[] EAP_REQUEST_AKA_IDENTITY_PACKET =
            hexStringToByteArray("01" + ID + "000A17" + EAP_REQUEST_TYPE_DATA);
    public static final byte[] EAP_REQUEST_IDENTITY_PACKET =
            hexStringToByteArray(("01" + ID + "000501"));
    // TODO(b/133794339): identity response packet data will need to be updated
    public static final byte[] EAP_RESPONSE_IDENTITY_PACKET =
            hexStringToByteArray("02" + ID + "000501");
    public static final byte[] EAP_REQUEST_NOTIFICATION_PACKET =
            hexStringToByteArray("01" + ID + "000802AABBCC");
    public static final byte[] EAP_SUCCESS_PACKET = hexStringToByteArray("03" + ID + "0004");

    // Body of EapData is the list of supported methods
    public static final byte[] EAP_RESPONSE_NAK_PACKET =
            hexStringToByteArray("02" + ID + "000803173212");
    public static final byte[] EAP_RESPONSE_NOTIFICATION_PACKET =
            hexStringToByteArray("02" + ID + "000502");
    public static final byte[] EAP_REQUEST_MD5_CHALLENGE =
            hexStringToByteArray("01" + ID + "000504");
    public static final byte[] EAP_REQUEST_NAK_PACKET =
            hexStringToByteArray("01" + ID + "000503");
    public static final byte[] EAP_REQUEST_SIM_START_PACKET =
            hexStringToByteArray("01" + ID + "0010120A00000F02000200010000");

    public static final byte[] REQUEST_UNSUPPORTED_TYPE_PACKET =
            hexStringToByteArray("01" + ID + "0005FF");
    public static final byte[] REQUEST_MISSING_TYPE_PACKET =
            hexStringToByteArray("01" + ID + "0004");
    public static final byte[] LONG_SUCCESS_PACKET = hexStringToByteArray("03" + ID + "000500");
    public static final byte[] SHORT_PACKET = hexStringToByteArray("01" + ID + "0005");
    public static final byte[] INCOMPLETE_HEADER_PACKET = hexStringToByteArray("03" + ID);
    public static final byte[] INVALID_CODE_PACKET = hexStringToByteArray("F0" + ID + "0004");
    public static final byte[] REQUEST_EAP_TYPE_NAK = hexStringToByteArray("01" + ID + "000503");
}