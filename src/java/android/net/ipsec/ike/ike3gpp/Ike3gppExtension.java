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

package android.net.ipsec.ike.ike3gpp;

import android.annotation.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Ike3gppExtension is used to provide 3GPP-specific extensions for an IKE Session.
 *
 * @see 3GPP ETSI TS 24.302: Access to the 3GPP Evolved Packet Core (EPC) via non-3GPP access
 *     networks
 * @hide
 */
// TODO(b/169856575): Improve documentation for 3GPP source docs
public final class Ike3gppExtension {
    @NonNull private final Ike3gppParams mIke3gppParams;
    @NonNull private final Ike3gppCallback mIke3gppCallback;

    /**
     * Constructs an Ike3gppExtension instance with the given Ike3gppCallback and Ike3gppParams
     * instances.
     *
     * @param ike3gppParams Ike3gppParams used to configure the 3GPP-support for an IKE Session.
     * @param ike3gppCallback Ike3gppCallback used to notify the caller of 3GPP-specific payloads
     *     received during an IKE Session.
     */
    public Ike3gppExtension(
            @NonNull Ike3gppParams ike3gppParams, @NonNull Ike3gppCallback ike3gppCallback) {
        Objects.requireNonNull(ike3gppParams, "ike3gppParams must not be null");
        Objects.requireNonNull(ike3gppCallback, "ike3gppCallback must not be null");

        mIke3gppParams = ike3gppParams;
        mIke3gppCallback = ike3gppCallback;
    }

    /** Retrieves the configured Ike3gppCallback. */
    @NonNull
    public Ike3gppCallback getIke3gppCallback() {
        return mIke3gppCallback;
    }

    /** Retrieves the configured Ike3gppParams. */
    @NonNull
    public Ike3gppParams getIke3gppParams() {
        return mIke3gppParams;
    }

    /**
     * Callback for receiving 3GPP-specific payloads.
     *
     * <p>MUST be unique to each IKE Session.
     */
    public interface Ike3gppCallback {
        /**
         * Invoked when the IKE Session receives one or more 3GPP-specific payloads.
         *
         * <p>This function will be invoked at most once for each IKE Message received by the IKEv2
         * library.
         *
         * @param payloads List<Ike3gppInfo> the 3GPP-payloads received
         */
        void onIke3gppPayloadsReceived(@NonNull List<Ike3gppInfo> payloads);
    }
}