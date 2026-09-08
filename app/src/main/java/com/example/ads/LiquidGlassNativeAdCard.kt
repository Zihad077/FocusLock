package com.example.ads

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.liquidGlass
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Liquid Glass Styled Native Advanced Ad Container for FocusLock.
 *
 * Visually pairs with the Obsidian / Glacial theme, featuring:
 * - Liquid glass translucent card backdrop with specular border
 * - Clear "Ad" / "Sponsored" badge for Play Policy compliance
 * - Icon, headline, advertiser / body text, call-to-action button, and rating bar
 * - Safe disposal of NativeAd objects to prevent memory leaks
 * - Zero layout flickering / jumps via graceful animated visibility
 */
@Composable
fun LiquidGlassNativeAdCard(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isFailedToLoad by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    DisposableEffect(Unit) {
        AdsManager.loadNativeAd(
            context = context,
            onLoaded = { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                isFailedToLoad = false
            },
            onFailed = {
                isFailedToLoad = true
            }
        )
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    AnimatedVisibility(
        visible = nativeAd != null && !isFailedToLoad,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        nativeAd?.let { ad ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        isElevated = false,
                        borderWidth = 0.8.dp
                    )
                    .padding(16.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        createNativeAdView(ctx, ad, isDark)
                    },
                    update = { view ->
                        populateNativeAdView(ad, view, isDark)
                    }
                )
            }
        }
    }
}

private fun createNativeAdView(
    context: Context,
    nativeAd: NativeAd,
    isDark: Boolean
): NativeAdView {
    val nativeAdView = NativeAdView(context)
    val container = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Header: "Ad" badge + Advertiser / Attribution
    val headerRow = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, 16)
    }

    val adBadge = TextView(context).apply {
        text = "SPONSORED"
        textSize = 9f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(if (isDark) 0xFF00E5FF.toInt() else 0xFF0077D6.toInt())
        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 10f
            setColor(if (isDark) 0x3000E5FF.toInt() else 0x200077D6.toInt())
            setStroke(2, if (isDark) 0x6000E5FF.toInt() else 0x400077D6.toInt())
        }
        background = bgDrawable
        setPadding(14, 4, 14, 4)
    }
    headerRow.addView(adBadge)

    val advertiserView = TextView(context).apply {
        textSize = 11f
        setTextColor(if (isDark) 0xB3FFFFFF.toInt() else 0x99000000.toInt())
        setPadding(16, 0, 0, 0)
    }
    headerRow.addView(advertiserView)
    nativeAdView.advertiserView = advertiserView

    container.addView(headerRow)

    // Middle Row: App Icon + Headline + Body
    val contentRow = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }

    val iconView = ImageView(context).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(110, 110).apply {
            setMargins(0, 0, 24, 0)
        }
        val bgIcon = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 24f
            setColor(if (isDark) 0x401E3555.toInt() else 0x40D5E5F2.toInt())
        }
        background = bgIcon
        clipToOutline = true
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    contentRow.addView(iconView)
    nativeAdView.iconView = iconView

    val textCol = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        layoutParams = android.widget.LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )
    }

    val headlineView = TextView(context).apply {
        textSize = 15f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(if (isDark) 0xFFFFFFFF.toInt() else 0xFF101928.toInt())
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    textCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(if (isDark) 0x99FFFFFF.toInt() else 0x8A000000.toInt())
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, 4, 0, 0)
    }
    textCol.addView(bodyView)
    nativeAdView.bodyView = bodyView

    contentRow.addView(textCol)
    container.addView(contentRow)

    // Call To Action Button (Glass / Pill gradient style)
    val ctaButton = Button(context).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 20, 0, 0)
        }
        textSize = 13f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(0xFFFFFFFF.toInt())
        val ctaDrawable = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 28f
            colors = intArrayOf(
                if (isDark) 0xFF00E5FF.toInt() else 0xFF0077D6.toInt(),
                if (isDark) 0xFF0077D6.toInt() else 0xFF005DB2.toInt()
            )
            orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
        }
        background = ctaDrawable
    }
    container.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(container)
    populateNativeAdView(nativeAd, nativeAdView, isDark)

    return nativeAdView
}

private fun populateNativeAdView(
    nativeAd: NativeAd,
    nativeAdView: NativeAdView,
    isDark: Boolean
) {
    // Headline
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    // Body
    val body = nativeAd.body
    val bodyView = nativeAdView.bodyView as? TextView
    if (body != null) {
        bodyView?.visibility = View.VISIBLE
        bodyView?.text = body
    } else {
        bodyView?.visibility = View.GONE
    }

    // Call to Action
    val cta = nativeAd.callToAction
    val ctaView = nativeAdView.callToActionView as? Button
    if (cta != null) {
        ctaView?.visibility = View.VISIBLE
        ctaView?.text = cta
    } else {
        ctaView?.visibility = View.GONE
    }

    // Icon
    val icon = nativeAd.icon
    val iconView = nativeAdView.iconView as? ImageView
    if (icon?.drawable != null) {
        iconView?.visibility = View.VISIBLE
        iconView?.setImageDrawable(icon.drawable)
    } else {
        iconView?.visibility = View.GONE
    }

    // Advertiser
    val advertiser = nativeAd.advertiser
    val advertiserView = nativeAdView.advertiserView as? TextView
    if (advertiser != null) {
        advertiserView?.visibility = View.VISIBLE
        advertiserView?.text = advertiser
    } else {
        advertiserView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}
