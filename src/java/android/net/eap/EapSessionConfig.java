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

package android.net.eap;

import static com.android.internal.net.eap.message.EapData.EAP_TYPE_AKA;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_AKA_PRIME;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_MSCHAP_V2;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_SIM;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_TTLS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.PersistableBundle;
import android.telephony.Annotation.UiccAppType;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.message.EapData.EapMethod;
import com.android.internal.net.ipsec.ike.utils.IkeCertUtils;
import com.android.server.vcn.util.PersistableBundleUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * EapSessionConfig represents a container for EAP method configuration.
 *
 * <p>The EAP authentication server decides which EAP method is used, so clients are encouraged to
 * provide configs for several EAP methods.
 */
public final class EapSessionConfig {
    private static final String EAP_ID_KEY = "eapIdentity";
    private static final String EAP_METHOD_CONFIGS_KEY = "eapConfigs";

    private static final byte[] DEFAULT_IDENTITY = new byte[0];

    // IANA -> EapMethodConfig for that method
    private final Map<Integer, EapMethodConfig> mEapConfigs;
    private final byte[] mEapIdentity;

    /** @hide */
    @VisibleForTesting
    public EapSessionConfig(Map<Integer, EapMethodConfig> eapConfigs, byte[] eapIdentity) {
        Objects.requireNonNull(eapConfigs, "eapConfigs must not be null");
        Objects.requireNonNull(eapIdentity, "eapIdentity must not be null");

        mEapConfigs = Collections.unmodifiableMap(eapConfigs);
        mEapIdentity = eapIdentity;
    }

    /**
     * Gets the EAP configs set in this EapSessionConfig.
     *
     * @hide
     */
    public Map<Integer, EapMethodConfig> getEapConfigs() {
        // Return the underlying Collection directly because it's unmodifiable
        return mEapConfigs;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle *
     *
     * <p>Constructed EapSessionConfigs are guaranteed to be valid, as checked by the
     * EapSessionConfig.Builder
     *
     * @hide
     */
    @NonNull
    public static EapSessionConfig fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        EapSessionConfig.Builder builder = new EapSessionConfig.Builder();

        PersistableBundle eapIdBundle = in.getPersistableBundle(EAP_ID_KEY);
        Objects.requireNonNull(eapIdBundle, "EAP ID bundle is null");
        byte[] eapId = PersistableBundleUtils.toByteArray(eapIdBundle);
        builder.setEapIdentity(eapId);

        PersistableBundle configsBundle = in.getPersistableBundle(EAP_METHOD_CONFIGS_KEY);
        Objects.requireNonNull(configsBundle, "EAP method configs bundle is null");
        Map<Integer, EapMethodConfig> eapMethodConfigs =
                PersistableBundleUtils.toMap(
                        configsBundle,
                        PersistableBundleUtils.INTEGER_DESERIALIZER,
                        EapMethodConfig::fromPersistableBundle);
        for (EapMethodConfig config : eapMethodConfigs.values()) {
            builder.addEapMethodConfig(config);
        }

        return builder.build();
    }

    /**
     * Serializes this object to a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = new PersistableBundle();
        result.putPersistableBundle(EAP_ID_KEY, PersistableBundleUtils.fromByteArray(mEapIdentity));

        final PersistableBundle configsBundle =
                PersistableBundleUtils.fromMap(
                        mEapConfigs,
                        PersistableBundleUtils.INTEGER_SERIALIZER,
                        EapMethodConfig::toPersistableBundle);
        result.putPersistableBundle(EAP_METHOD_CONFIGS_KEY, configsBundle);
        return result;
    }

    /** Retrieves client's EAP Identity */
    @NonNull
    public byte[] getEapIdentity() {
        return mEapIdentity.clone();
    }

    /**
     * Retrieves configuration for EAP SIM
     *
     * @return the configuration for EAP SIM, or null if it was not set
     */
    @Nullable
    public EapSimConfig getEapSimConfig() {
        return (EapSimConfig) mEapConfigs.get(EAP_TYPE_SIM);
    }

    /**
     * Retrieves configuration for EAP AKA
     *
     * @return the configuration for EAP AKA, or null if it was not set
     */
    @Nullable
    public EapAkaConfig getEapAkaConfig() {
        return (EapAkaConfig) mEapConfigs.get(EAP_TYPE_AKA);
    }

    /**
     * Retrieves configuration for EAP AKA'
     *
     * @return the configuration for EAP AKA', or null if it was not set
     */
    @Nullable
    public EapAkaPrimeConfig getEapAkaPrimeConfig() {
        return (EapAkaPrimeConfig) mEapConfigs.get(EAP_TYPE_AKA_PRIME);
    }

    /**
     * Retrieves configuration for EAP MSCHAPV2
     *
     * @return the configuration for EAP MSCHAPV2, or null if it was not set
     */
    @Nullable
    public EapMsChapV2Config getEapMsChapV2Config() {
        return (EapMsChapV2Config) mEapConfigs.get(EAP_TYPE_MSCHAP_V2);
    }

    /**
     * Retrieves configuration for EAP MSCHAPV2
     *
     * @return the configuration for EAP MSCHAPV2, or null if it was not set
     * @hide
     * @deprecated Callers should use {@link #getEapMsChapV2Config}
     */
    @Deprecated
    @SystemApi
    @Nullable
    public EapMsChapV2Config getEapMsChapV2onfig() {
        return getEapMsChapV2Config();
    }

    /**
     * Retrieves configuration for EAP-TTLS
     *
     * @return the configuration for EAP-TTLS, or null if it was not set
     * @hide
     */
    @Nullable
    public EapTtlsConfig getEapTtlsConfig() {
        return (EapTtlsConfig) mEapConfigs.get(EAP_TYPE_TTLS);
    }

    /** @hide */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mEapIdentity), mEapConfigs);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EapSessionConfig)) {
            return false;
        }

        EapSessionConfig other = (EapSessionConfig) o;
        return Arrays.equals(mEapIdentity, other.mEapIdentity)
                && mEapConfigs.equals(other.mEapConfigs);
    }

    /** This class can be used to incrementally construct an {@link EapSessionConfig}. */
    public static final class Builder {
        private final Map<Integer, EapMethodConfig> mEapConfigs;
        private byte[] mEapIdentity;

        /** Constructs and returns a new Builder for constructing an {@link EapSessionConfig}. */
        public Builder() {
            mEapConfigs = new HashMap<>();
            mEapIdentity = DEFAULT_IDENTITY;
        }

        /**
         * Sets the client's EAP Identity.
         *
         * @param eapIdentity byte[] representing the client's EAP Identity.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder setEapIdentity(@NonNull byte[] eapIdentity) {
            Objects.requireNonNull(eapIdentity, "eapIdentity must not be null");
            this.mEapIdentity = eapIdentity.clone();
            return this;
        }

        /**
         * Sets the configuration for EAP SIM.
         *
         * @param subId int the client's subId to be authenticated.
         * @param apptype the {@link UiccAppType} apptype to be used for authentication.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder setEapSimConfig(int subId, @UiccAppType int apptype) {
            mEapConfigs.put(EAP_TYPE_SIM, new EapSimConfig(subId, apptype));
            return this;
        }

        /**
         * Sets the configuration for EAP AKA.
         *
         * @param subId int the client's subId to be authenticated.
         * @param apptype the {@link UiccAppType} apptype to be used for authentication.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder setEapAkaConfig(int subId, @UiccAppType int apptype) {
            mEapConfigs.put(EAP_TYPE_AKA, new EapAkaConfig(subId, apptype));
            return this;
        }

        /**
         * Sets the configuration for EAP AKA'.
         *
         * @param subId int the client's subId to be authenticated.
         * @param apptype the {@link UiccAppType} apptype to be used for authentication.
         * @param networkName String the network name to be used for authentication.
         * @param allowMismatchedNetworkNames indicates whether the EAP library can ignore potential
         *     mismatches between the given network name and that received in an EAP-AKA' session.
         *     If false, mismatched network names will be handled as an Authentication Reject
         *     message.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder setEapAkaPrimeConfig(
                int subId,
                @UiccAppType int apptype,
                @NonNull String networkName,
                boolean allowMismatchedNetworkNames) {
            mEapConfigs.put(
                    EAP_TYPE_AKA_PRIME,
                    new EapAkaPrimeConfig(
                            subId, apptype, networkName, allowMismatchedNetworkNames));
            return this;
        }

        /**
         * Sets the configuration for EAP MSCHAPv2.
         *
         * @param username String the client account's username to be authenticated.
         * @param password String the client account's password to be authenticated.
         * @return Builder this, to faciliate chaining.
         */
        @NonNull
        public Builder setEapMsChapV2Config(@NonNull String username, @NonNull String password) {
            mEapConfigs.put(EAP_TYPE_MSCHAP_V2, new EapMsChapV2Config(username, password));
            return this;
        }

        /**
         * Sets the configuration for EAP-TTLS
         *
         * <p>Nested tunnel authentications are disallowed.
         *
         * @param serverCaCert the CA certificate for validating the received server certificate(s).
         *     If a certificate is provided, it MUST be the root CA used by the server, or
         *     authentication will fail. If no certificate is provided, any root CA in the system's
         *     truststore is considered acceptable.
         * @param innerEapSessionConfig represents the configuration for the inner EAP instance
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @NonNull
        public Builder setEapTtlsConfig(
                @Nullable X509Certificate serverCaCert,
                @NonNull EapSessionConfig innerEapSessionConfig) {
            mEapConfigs.put(EAP_TYPE_TTLS, new EapTtlsConfig(serverCaCert, innerEapSessionConfig));
            return this;
        }

        /**
         * Adds an EAP method configuration. Internal use only.
         *
         * <p>This method will override the previously set configuration with the same method type.
         *
         * @hide
         */
        @NonNull
        public Builder addEapMethodConfig(@NonNull EapMethodConfig config) {
            Objects.requireNonNull(config, "EapMethodConfig is null");
            mEapConfigs.put(config.mMethodType, config);
            return this;
        }

        /**
         * Constructs and returns an EapSessionConfig with the configurations applied to this
         * Builder.
         *
         * @return the EapSessionConfig constructed by this Builder.
         */
        @NonNull
        public EapSessionConfig build() {
            if (mEapConfigs.isEmpty()) {
                throw new IllegalStateException("Must have at least one EAP method configured");
            }

            return new EapSessionConfig(mEapConfigs, mEapIdentity);
        }
    }

    /** EapMethodConfig represents a generic EAP method configuration. */
    public abstract static class EapMethodConfig {
        private static final String METHOD_TYPE = "methodType";

        @EapMethod private final int mMethodType;

        /** @hide */
        EapMethodConfig(@EapMethod int methodType) {
            mMethodType = methodType;
        }

        /**
         * Constructs this object by deserializing a PersistableBundle
         *
         * @hide
         */
        @NonNull
        public static EapMethodConfig fromPersistableBundle(PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");

            int methodType = in.getInt(METHOD_TYPE);
            switch (methodType) {
                case EAP_TYPE_SIM:
                    return EapSimConfig.fromPersistableBundle(in);
                case EAP_TYPE_AKA:
                    return EapAkaConfig.fromPersistableBundle(in);
                case EAP_TYPE_AKA_PRIME:
                    return EapAkaPrimeConfig.fromPersistableBundle(in);
                case EAP_TYPE_MSCHAP_V2:
                    return EapMsChapV2Config.fromPersistableBundle(in);
                case EAP_TYPE_TTLS:
                    return EapTtlsConfig.fromPersistableBundle(in);
                default:
                    throw new IllegalArgumentException("Invalid EAP Type: " + methodType);
            }
        }

        /**
         * Serializes this object to a PersistableBundle
         *
         * @hide
         */
        @NonNull
        protected PersistableBundle toPersistableBundle() {
            final PersistableBundle result = new PersistableBundle();
            result.putInt(METHOD_TYPE, mMethodType);
            return result;
        }

        /**
         * Retrieves the EAP method type
         *
         * @return the IANA-defined EAP method constant
         */
        public int getMethodType() {
            return mMethodType;
        }

        /**
         * Check if this is EAP-only safe method.
         *
         * @return whether the method is EAP-only safe
         *
         * @see <a href="https://tools.ietf.org/html/rfc5998">RFC 5998#section 4, for safe eap
         * methods</a>
         *
         * @hide
         */
        public boolean isEapOnlySafeMethod() {
            return false;
        }

        /** @hide */
        @Override
        public int hashCode() {
            return Objects.hash(mMethodType);
        }

        /** @hide */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EapMethodConfig)) {
                return false;
            }

            return mMethodType == ((EapMethodConfig) o).mMethodType;
        }
    }

    /**
     * EapUiccConfig represents the configs needed for EAP methods that rely on UICC cards for
     * authentication.
     *
     * @hide
     * @deprecated This class is not useful. Callers should only use its two subclasses {@link
     *     EapSimConfig} and {@link EapAkaConfig}
     */
    @Deprecated
    @SystemApi
    public abstract static class EapUiccConfig extends EapMethodConfig {
        /** @hide */
        protected static final String SUB_ID_KEY = "subId";
        /** @hide */
        protected static final String APP_TYPE_KEY = "apptype";

        private final int mSubId;
        private final int mApptype;

        private EapUiccConfig(@EapMethod int methodType, int subId, @UiccAppType int apptype) {
            super(methodType);
            mSubId = subId;
            mApptype = apptype;
        }

        /**
         * Serializes this object to a PersistableBundle
         *
         * @hide
         */
        @Override
        @NonNull
        protected PersistableBundle toPersistableBundle() {
            final PersistableBundle result = super.toPersistableBundle();
            result.putInt(SUB_ID_KEY, mSubId);
            result.putInt(APP_TYPE_KEY, mApptype);

            return result;
        }

        /**
         * Retrieves the subId
         *
         * @return the subId
         */
        public int getSubId() {
            return mSubId;
        }

        /**
         * Retrieves the UICC app type
         *
         * @return the {@link UiccAppType} constant
         */
        public int getAppType() {
            return mApptype;
        }

        /** @hide */
        @Override
        public boolean isEapOnlySafeMethod() {
            return true;
        }

        /** @hide */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), mSubId, mApptype);
        }

        /** @hide */
        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof EapUiccConfig)) {
                return false;
            }

            EapUiccConfig other = (EapUiccConfig) o;

            return mSubId == other.mSubId && mApptype == other.mApptype;
        }
    }

    /**
     * EapSimConfig represents the configs needed for an EAP SIM session.
     */
    public static class EapSimConfig extends EapUiccConfig {
        /** @hide */
        @VisibleForTesting
        public EapSimConfig(int subId, @UiccAppType int apptype) {
            super(EAP_TYPE_SIM, subId, apptype);
        }

        /**
         * Constructs this object by deserializing a PersistableBundle
         *
         * @hide
         */
        @NonNull
        public static EapSimConfig fromPersistableBundle(@NonNull PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");
            return new EapSimConfig(in.getInt(SUB_ID_KEY), in.getInt(APP_TYPE_KEY));
        }
    }

    /**
     * EapAkaConfig represents the configs needed for an EAP AKA session.
     */
    public static class EapAkaConfig extends EapUiccConfig {
        /** @hide */
        @VisibleForTesting
        public EapAkaConfig(int subId, @UiccAppType int apptype) {
            this(EAP_TYPE_AKA, subId, apptype);
        }

        /** @hide */
        EapAkaConfig(int methodType, int subId, @UiccAppType int apptype) {
            super(methodType, subId, apptype);
        }

        /**
         * Constructs this object by deserializing a PersistableBundle
         *
         * @hide
         */
        @NonNull
        public static EapAkaConfig fromPersistableBundle(@NonNull PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");
            return new EapAkaConfig(in.getInt(SUB_ID_KEY), in.getInt(APP_TYPE_KEY));
        }
    }

    /**
     * EapAkaPrimeConfig represents the configs needed for an EAP-AKA' session.
     */
    public static class EapAkaPrimeConfig extends EapAkaConfig {
        private static final String NETWORK_NAME_KEY = "networkName";
        private static final String ALL_MISMATCHED_NETWORK_KEY = "allowMismatchedNetworkNames";

        @NonNull private final String mNetworkName;
        private final boolean mAllowMismatchedNetworkNames;

        /** @hide */
        @VisibleForTesting
        public EapAkaPrimeConfig(
                int subId,
                @UiccAppType int apptype,
                @NonNull String networkName,
                boolean allowMismatchedNetworkNames) {
            super(EAP_TYPE_AKA_PRIME, subId, apptype);

            Objects.requireNonNull(networkName, "networkName must not be null");

            mNetworkName = networkName;
            mAllowMismatchedNetworkNames = allowMismatchedNetworkNames;
        }

        /**
         * Constructs this object by deserializing a PersistableBundle
         *
         * @hide
         */
        @NonNull
        public static EapAkaPrimeConfig fromPersistableBundle(@NonNull PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");
            return new EapAkaPrimeConfig(
                    in.getInt(SUB_ID_KEY),
                    in.getInt(APP_TYPE_KEY),
                    in.getString(NETWORK_NAME_KEY),
                    in.getBoolean(ALL_MISMATCHED_NETWORK_KEY));
        }

        /**
         * Serializes this object to a PersistableBundle
         *
         * @hide
         */
        @Override
        @NonNull
        protected PersistableBundle toPersistableBundle() {
            final PersistableBundle result = super.toPersistableBundle();
            result.putString(NETWORK_NAME_KEY, mNetworkName);
            result.putBoolean(ALL_MISMATCHED_NETWORK_KEY, mAllowMismatchedNetworkNames);

            return result;
        }

        /**
         * Retrieves the UICC app type
         *
         * @return the {@link UiccAppType} constant
         */
        @NonNull
        public String getNetworkName() {
            return mNetworkName;
        }

        /**
         * Checks if mismatched network names are allowed
         *
         * @return whether network name mismatches are allowed
         */
        public boolean allowsMismatchedNetworkNames() {
            return mAllowMismatchedNetworkNames;
        }

        /** @hide */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), mNetworkName, mAllowMismatchedNetworkNames);
        }

        /** @hide */
        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof EapAkaPrimeConfig)) {
                return false;
            }

            EapAkaPrimeConfig other = (EapAkaPrimeConfig) o;

            return mNetworkName.equals(other.mNetworkName)
                    && mAllowMismatchedNetworkNames == other.mAllowMismatchedNetworkNames;
        }
    }

    /**
     * EapMsChapV2Config represents the configs needed for an EAP MSCHAPv2 session.
     */
    public static class EapMsChapV2Config extends EapMethodConfig {
        private static final String USERNAME_KEY = "username";
        private static final String PASSWORD_KEY = "password";

        @NonNull private final String mUsername;
        @NonNull private final String mPassword;

        /** @hide */
        @VisibleForTesting
        public EapMsChapV2Config(String username, String password) {
            super(EAP_TYPE_MSCHAP_V2);

            Objects.requireNonNull(username, "username must not be null");
            Objects.requireNonNull(password, "password must not be null");

            mUsername = username;
            mPassword = password;
        }

        /**
         * Constructs this object by deserializing a PersistableBundle
         *
         * @hide
         */
        @NonNull
        public static EapMsChapV2Config fromPersistableBundle(@NonNull PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");
            return new EapMsChapV2Config(in.getString(USERNAME_KEY), in.getString(PASSWORD_KEY));
        }

        /**
         * Serializes this object to a PersistableBundle
         *
         * @hide
         */
        @Override
        @NonNull
        protected PersistableBundle toPersistableBundle() {
            final PersistableBundle result = super.toPersistableBundle();
            result.putString(USERNAME_KEY, mUsername);
            result.putString(PASSWORD_KEY, mPassword);

            return result;
        }

        /**
         * Retrieves the username
         *
         * @return the username to be used by MSCHAPV2
         */
        @NonNull
        public String getUsername() {
            return mUsername;
        }

        /**
         * Retrieves the password
         *
         * @return the password to be used by MSCHAPV2
         */
        @NonNull
        public String getPassword() {
            return mPassword;
        }

        /** @hide */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), mUsername, mPassword);
        }

        /** @hide */
        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof EapMsChapV2Config)) {
                return false;
            }

            EapMsChapV2Config other = (EapMsChapV2Config) o;

            return mUsername.equals(other.mUsername) && mPassword.equals(other.mPassword);
        }
    }

    /**
     * EapTtlsConfig represents the configs needed for an EAP-TTLS session.
     *
     * @hide
     */
    public static class EapTtlsConfig extends EapMethodConfig {
        private static final String TRUST_CERT_KEY = "TRUST_CERT_KEY";
        private static final String EAP_SESSION_CONFIG_KEY = "EAP_SESSION_CONFIG_KEY";

        @Nullable private final TrustAnchor mOverrideTrustAnchor;
        @NonNull private final EapSessionConfig mInnerEapSessionConfig;

        /** @hide */
        @VisibleForTesting
        public EapTtlsConfig(
                @Nullable X509Certificate serverCaCert,
                @NonNull EapSessionConfig innerEapSessionConfig) {
            super(EAP_TYPE_TTLS);
            mInnerEapSessionConfig =
                    Objects.requireNonNull(
                            innerEapSessionConfig, "innerEapSessionConfig must not be null");
            if (mInnerEapSessionConfig.getEapConfigs().containsKey(EAP_TYPE_TTLS)) {
                throw new IllegalArgumentException("Recursive EAP-TTLS method configs not allowed");
            }

            mOverrideTrustAnchor =
                    (serverCaCert == null)
                            ? null
                            : new TrustAnchor(serverCaCert, null /* nameConstraints */);
        }

        /**
         * Constructs this object by deserializing a PersistableBundle.
         *
         * @hide
         */
        @NonNull
        public static EapTtlsConfig fromPersistableBundle(@NonNull PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");

            PersistableBundle trustCertBundle = in.getPersistableBundle(TRUST_CERT_KEY);
            X509Certificate caCert = null;
            if (trustCertBundle != null) {
                byte[] encodedCert = PersistableBundleUtils.toByteArray(trustCertBundle);
                caCert = IkeCertUtils.certificateFromByteArray(encodedCert);
            }

            PersistableBundle eapSessionConfigBundle =
                    in.getPersistableBundle(EAP_SESSION_CONFIG_KEY);
            Objects.requireNonNull(eapSessionConfigBundle, "eapSessionConfigBundle is null");
            EapSessionConfig eapSessionConfig =
                    EapSessionConfig.fromPersistableBundle(eapSessionConfigBundle);

            return new EapTtlsConfig(caCert, eapSessionConfig);
        }

        /**
         * Serializes this object to a PersistableBundle.
         *
         * @hide
         */
        @Override
        @NonNull
        protected PersistableBundle toPersistableBundle() {
            final PersistableBundle result = super.toPersistableBundle();

            try {
                if (mOverrideTrustAnchor != null) {
                    result.putPersistableBundle(
                            TRUST_CERT_KEY,
                            PersistableBundleUtils.fromByteArray(
                                    mOverrideTrustAnchor.getTrustedCert().getEncoded()));
                }

                result.putPersistableBundle(
                        EAP_SESSION_CONFIG_KEY, mInnerEapSessionConfig.toPersistableBundle());
            } catch (CertificateEncodingException e) {
                throw new IllegalArgumentException("Fail to encode the certificate");
            }

            return result;
        }

        /** @hide */
        @Override
        public boolean isEapOnlySafeMethod() {
            return true;
        }

        /**
         * Retrieves the provided CA certificate for validating the remote certificate(s)
         *
         * @return the CA certificate for validating the received server certificate or null if the
         *     system default is preferred
         * @hide
         */
        @Nullable
        public X509Certificate getServerCaCert() {
            return (mOverrideTrustAnchor == null) ? null : mOverrideTrustAnchor.getTrustedCert();
        }

        /**
         * Retrieves the inner EAP session config
         *
         * @return an EapSessionConfig representing the config for tunneled EAP authentication
         * @hide
         */
        @NonNull
        public EapSessionConfig getInnerEapSessionConfig() {
            return mInnerEapSessionConfig;
        }

        /** @hide */
        @Override
        public int hashCode() {
            // Use #getTrustedCert() because TrustAnchor does not override #hashCode()

            return Objects.hash(
                    super.hashCode(),
                    mInnerEapSessionConfig,
                    (mOverrideTrustAnchor == null) ? null : mOverrideTrustAnchor.getTrustedCert());
        }

        /** @hide */
        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof EapTtlsConfig)) {
                return false;
            }

            EapTtlsConfig other = (EapTtlsConfig) o;

            if (!Objects.equals(mInnerEapSessionConfig, other.mInnerEapSessionConfig)) {
                return false;
            }

            if (mOverrideTrustAnchor == null && other.mOverrideTrustAnchor == null) {
                return true;
            }

            return mOverrideTrustAnchor != null
                    && other.mOverrideTrustAnchor != null
                    && Objects.equals(
                            mOverrideTrustAnchor.getTrustedCert(),
                            other.mOverrideTrustAnchor.getTrustedCert());
        }
    }

    /**
     * Checks if all the methods in the session are EAP-only safe
     *
     * @return whether all the methods in the session are EAP-only safe
     *
     * @see <a href="https://tools.ietf.org/html/rfc5998">RFC 5998#section 4, for safe eap
     * methods</a>
     *
     * @hide
     */
    public boolean areAllMethodsEapOnlySafe() {
        for (Map.Entry<Integer, EapMethodConfig> eapConfigsEntry : mEapConfigs.entrySet()) {
            if (!eapConfigsEntry.getValue().isEapOnlySafeMethod()) {
                return false;
            }
        }

        return true;
    }
}
