package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized AdMob & Privacy Consent Manager for FocusLock.
 *
 * Rules:
 * 1. ZERO ads for Premium users (`isPremium == true`).
 * 2. ZERO ads during Focus Mode, Unlock Challenges, Onboarding, Blocking screens, or Settings.
 * 3. Uses Production IDs on release builds when provided via Secrets; automatically falls back to standard Google Test IDs on debug/development builds.
 * 4. Safe lifecycle management, avoiding memory leaks, multiple concurrent loads, or accidental clicks.
 */
object AdsManager {
    private const val TAG = "AdsManager"

    // Standard Google AdMob Sample / Test Ad Unit IDs
    private const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val isMobileAdsInitialized = AtomicBoolean(false)
    private var consentInformation: ConsentInformation? = null

    // Rewarded Ad state
    private var rewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = false

    private val _isRewardedAdLoaded = MutableStateFlow(false)
    val isRewardedAdLoaded: StateFlow<Boolean> = _isRewardedAdLoaded.asStateFlow()

    /**
     * Resolves the active Native Ad Unit ID based on build variant and secrets.
     */
    fun getNativeAdUnitId(): String {
        return if (BuildConfig.DEBUG) {
            TEST_NATIVE_AD_UNIT_ID
        } else {
            val configured = BuildConfig.ADMOB_NATIVE_AD_UNIT_ID
            if (!configured.isNullOrBlank() && configured != "MY_ADMOB_NATIVE_AD_UNIT_ID") {
                configured
            } else {
                TEST_NATIVE_AD_UNIT_ID
            }
        }
    }

    /**
     * Resolves the active Banner Ad Unit ID based on build variant and secrets.
     */
    fun getBannerAdUnitId(): String {
        return if (BuildConfig.DEBUG) {
            TEST_BANNER_AD_UNIT_ID
        } else {
            val configured = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
            if (!configured.isNullOrBlank() && configured != "MY_ADMOB_BANNER_AD_UNIT_ID") {
                configured
            } else {
                TEST_BANNER_AD_UNIT_ID
            }
        }
    }

    /**
     * Resolves the active Rewarded Ad Unit ID based on build variant and secrets.
     */
    fun getRewardedAdUnitId(): String {
        return if (BuildConfig.DEBUG) {
            TEST_REWARDED_AD_UNIT_ID
        } else {
            val configured = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
            if (!configured.isNullOrBlank() && configured != "MY_ADMOB_REWARDED_AD_UNIT_ID") {
                configured
            } else {
                TEST_REWARDED_AD_UNIT_ID
            }
        }
    }

    /**
     * Initialize Google Mobile Ads SDK and UMP Consent.
     */
    fun initialize(context: Context, onInitialized: () -> Unit = {}) {
        if (isMobileAdsInitialized.getAndSet(true)) {
            onInitialized()
            return
        }

        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob SDK Initialized: ${initializationStatus.adapterStatusMap.keys}")
                onInitialized()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds: ${e.message}")
            onInitialized()
        }
    }

    /**
     * Request and gather User Messaging Platform (UMP) Consent for GDPR / Privacy compliance.
     */
    fun requestConsentAndInit(activity: Activity, onConsentCompleted: () -> Unit = {}) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    if (consentInformation?.canRequestAds() == true) {
                        initialize(activity) {
                            preloadRewardedAd(activity)
                            onConsentCompleted()
                        }
                    } else {
                        onConsentCompleted()
                    }
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent info update failed: ${requestConsentError.message}")
                initialize(activity) {
                    preloadRewardedAd(activity)
                    onConsentCompleted()
                }
            }
        )
    }

    /**
     * Opens the privacy options / consent form directly if required.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.e(TAG, "Failed to show privacy options form: ${formError.message}")
            }
            onDismissed()
        }
    }

    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation?.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    // ==========================================
    // NATIVE ADVANCED ADS
    // ==========================================

    /**
     * Loads a Native Ad asynchronously.
     */
    fun loadNativeAd(
        context: Context,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (LoadAdError) -> Unit = {}
    ) {
        val adUnitId = getNativeAdUnitId()
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native Ad loaded successfully: ${nativeAd.headline}")
                onLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message} (code ${error.code})")
                    onFailed(error)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setRequestMultipleImages(false)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ==========================================
    // REWARDED ADS
    // ==========================================

    /**
     * Preloads a Rewarded Ad so it is instantly available when requested.
     */
    fun preloadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedAdLoading) return

        isRewardedAdLoading = true
        val adUnitId = getRewardedAdUnitId()
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    _isRewardedAdLoaded.value = true
                    Log.d(TAG, "Rewarded ad preloaded successfully.")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            _isRewardedAdLoaded.value = false
                            preloadRewardedAd(context)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                            rewardedAd = null
                            _isRewardedAdLoaded.value = false
                            preloadRewardedAd(context)
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedAdLoading = false
                    _isRewardedAdLoaded.value = false
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows the rewarded ad to the user.
     * The reward callback ONLY fires when the user successfully finishes watching the ad.
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (rewardAmount: Int, rewardType: String) -> Unit,
        onAdUnavailableOrFailed: () -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            currentAd.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
        } else {
            Log.w(TAG, "Rewarded ad was not ready yet.")
            preloadRewardedAd(activity)
            onAdUnavailableOrFailed()
        }
    }
}
