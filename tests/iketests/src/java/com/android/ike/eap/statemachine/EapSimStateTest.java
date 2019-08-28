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

package com.android.ike.eap.statemachine;

import static android.telephony.TelephonyManager.APPTYPE_USIM;

import static com.android.ike.TestUtils.hexStringToByteArray;
import static com.android.ike.eap.message.EapData.EAP_NOTIFICATION;
import static com.android.ike.eap.message.EapData.EAP_TYPE_SIM;
import static com.android.ike.eap.message.EapMessage.EAP_CODE_REQUEST;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_RESPONSE_NOTIFICATION_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_SIM_CLIENT_ERROR_INSUFFICIENT_CHALLENGES;
import static com.android.ike.eap.message.EapTestMessageDefinitions.ID_INT;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.telephony.TelephonyManager;

import com.android.ike.eap.EapResult;
import com.android.ike.eap.EapResult.EapResponse;
import com.android.ike.eap.EapSessionConfig.EapSimConfig;
import com.android.ike.eap.message.EapData;
import com.android.ike.eap.message.EapMessage;
import com.android.ike.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.ike.eap.message.simaka.EapSimAkaTypeData.DecodeResult;
import com.android.ike.eap.message.simaka.EapSimTypeData;
import com.android.ike.eap.message.simaka.EapSimTypeData.EapSimTypeDataDecoder;
import com.android.ike.eap.statemachine.EapMethodStateMachine.EapMethodState;

import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;

public class EapSimStateTest {
    protected static final int SUB_ID = 1;
    protected static final String NOTIFICATION_MESSAGE = "test";
    protected static final byte[] DUMMY_EAP_TYPE_DATA = hexStringToByteArray("112233445566");

    protected TelephonyManager mMockTelephonyManager;
    protected EapSimTypeDataDecoder mMockEapSimTypeDataDecoder;

    protected EapSimConfig mEapSimConfig = new EapSimConfig(SUB_ID, APPTYPE_USIM);
    protected EapSimMethodStateMachine mEapSimMethodStateMachine;

    @Before
    public void setUp() {
        mMockTelephonyManager = mock(TelephonyManager.class);
        mMockEapSimTypeDataDecoder = mock(EapSimTypeDataDecoder.class);

        when(mMockTelephonyManager.createForSubscriptionId(SUB_ID))
                .thenReturn(mMockTelephonyManager);

        mEapSimMethodStateMachine =
                new EapSimMethodStateMachine(
                        mMockTelephonyManager,
                        mEapSimConfig,
                        new SecureRandom(),
                        mMockEapSimTypeDataDecoder);

        verify(mMockTelephonyManager).createForSubscriptionId(SUB_ID);
    }

    @Test
    public void testProcessNotification() throws Exception {
        EapData eapData = new EapData(EAP_NOTIFICATION, NOTIFICATION_MESSAGE.getBytes());
        EapMessage notification = new EapMessage(EAP_CODE_REQUEST, ID_INT, eapData);
        EapMethodState preNotification = (EapMethodState) mEapSimMethodStateMachine.getState();

        EapResult result = mEapSimMethodStateMachine.process(notification);
        assertEquals(preNotification, mEapSimMethodStateMachine.getState());
        verifyNoMoreInteractions(mMockTelephonyManager, mMockEapSimTypeDataDecoder);

        assertTrue(result instanceof EapResponse);
        EapResponse eapResponse = (EapResponse) result;
        assertArrayEquals(EAP_RESPONSE_NOTIFICATION_PACKET, eapResponse.packet);
    }

    @Test
    public void testProcessInvalidDecodeResult() throws Exception {
        EapData eapData = new EapData(EAP_TYPE_SIM, DUMMY_EAP_TYPE_DATA);
        EapMessage eapMessage = new EapMessage(EAP_CODE_REQUEST, ID_INT, eapData);
        EapMethodState preProcess = (EapMethodState) mEapSimMethodStateMachine.getState();

        AtClientErrorCode atClientErrorCode = AtClientErrorCode.INSUFFICIENT_CHALLENGES;
        DecodeResult<EapSimTypeData> decodeResult = new DecodeResult<>(atClientErrorCode);
        when(mMockEapSimTypeDataDecoder.decode(eq(DUMMY_EAP_TYPE_DATA))).thenReturn(decodeResult);

        EapResult result = mEapSimMethodStateMachine.process(eapMessage);
        assertEquals(preProcess, mEapSimMethodStateMachine.getState());
        verify(mMockEapSimTypeDataDecoder).decode(DUMMY_EAP_TYPE_DATA);
        verifyNoMoreInteractions(mMockTelephonyManager, mMockEapSimTypeDataDecoder);

        assertTrue(result instanceof EapResponse);
        EapResponse eapResponse = (EapResponse) result;
        assertArrayEquals(EAP_SIM_CLIENT_ERROR_INSUFFICIENT_CHALLENGES, eapResponse.packet);
    }
}
