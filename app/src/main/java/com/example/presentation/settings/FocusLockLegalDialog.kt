package com.example.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.liquidGlass

enum class LegalTab {
    PRIVACY,
    TERMS
}

@Composable
fun FocusLockLegalDialog(
    initialTab: LegalTab = LegalTab.PRIVACY,
    defaultLanguageCode: String = "en",
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var isBengali by remember { mutableStateOf(defaultLanguageCode == "bn") }
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedTab, isBengali) {
        scrollState.animateScrollTo(0)
    }

    val cyanAccent = Color(0xFF24DFEC)
    val emeraldGreen = Color(0xFF00E676)
    val purpleAccent = Color(0xFFA855F7)
    val amberGold = Color(0xFFFFB300)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    isElevated = true,
                    borderWidth = 1.dp
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFA122234),
                            Color(0xF60B1623),
                            Color(0xFA070F19)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row: Badge + EN/বাংলা Toggle + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(emeraldGreen.copy(alpha = 0.16f))
                                .border(0.8.dp, emeraldGreen.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = emeraldGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBengali) "১০০% নিরাপদ ও স্বচ্ছ" else "100% TRUST & TRANSPARENCY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = emeraldGreen,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        // Instant EN / বাংলা Reader Toggle
                        Box(
                            modifier = Modifier
                                .testTag("legal_language_toggle")
                                .clip(RoundedCornerShape(8.dp))
                                .background(cyanAccent.copy(alpha = 0.14f))
                                .border(0.8.dp, cyanAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .clickable { isBengali = !isBengali }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isBengali) "Read in EN" else "বাংলায় পড়ুন",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = cyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Tab Switcher: Privacy Policy vs Terms of Service
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A1420))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LegalTabButton(
                        title = if (isBengali) "প্রাইভেসি পলিসি" else "Privacy Policy",
                        icon = Icons.Default.Shield,
                        isSelected = selectedTab == LegalTab.PRIVACY,
                        accentColor = cyanAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_privacy_policy"),
                        onClick = { selectedTab = LegalTab.PRIVACY }
                    )
                    LegalTabButton(
                        title = if (isBengali) "ব্যবহারের শর্তাবলী" else "Terms of Service",
                        icon = Icons.Default.Gavel,
                        isSelected = selectedTab == LegalTab.TERMS,
                        accentColor = cyanAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_terms_of_service"),
                        onClick = { selectedTab = LegalTab.TERMS }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedContent(
                        targetState = Pair(selectedTab, isBengali),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "legal_content_transition"
                    ) { (tab, bn) ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (tab == LegalTab.PRIVACY) {
                                PrivacyContent(
                                    isBengali = bn,
                                    cyanAccent = cyanAccent,
                                    emeraldGreen = emeraldGreen,
                                    purpleAccent = purpleAccent,
                                    amberGold = amberGold
                                )
                            } else {
                                TermsContent(
                                    isBengali = bn,
                                    cyanAccent = cyanAccent,
                                    emeraldGreen = emeraldGreen,
                                    purpleAccent = purpleAccent,
                                    amberGold = amberGold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Reassurance Action Button
                GlassButton(
                    text = if (isBengali) "বুঝেছি — ফোকাসে লক ইন থাকুন 🔒" else "Got It — Let's Stay Locked In 🔒",
                    style = GlassButtonStyle.PRIMARY,
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("legal_confirm_button")
                )
            }
        }
    }
}

@Composable
private fun LegalTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun PrivacyContent(
    isBengali: Boolean,
    cyanAccent: Color,
    emeraldGreen: Color,
    purpleAccent: Color,
    amberGold: Color
) {
    // Hero Trust Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        cyanAccent.copy(alpha = 0.16f),
                        purpleAccent.copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.dp, cyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isBengali) {
                    "আপনার সময় বাঁচাতে তৈরি — আপনার ডেটা নিতে নয় 🛡️"
                } else {
                    "Built to Protect Your Time — Never to Harvest Your Data 🛡️"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
            Text(
                text = if (isBengali) {
                    "অন্যান্য অ্যাপ চায় আপনি ফোনের স্ক্রিনে আটকে থাকুন। FocusLock চায় আপনি ফোন নামিয়ে নিজের স্বপ্ন, পড়াশোনা ও আসল জীবনে মনোযোগ দিন। আপনার সব তথ্য ১০০% আপনার ফোনেই সুরক্ষিত থাকে।"
                } else {
                    "Most apps are engineered to keep you scrolling and monetize your habits. FocusLock is built for the exact opposite: helping you reclaim your attention while keeping 100% of your personal data inside your device."
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }

    LegalSectionCard(
        icon = Icons.Default.Lock,
        iconColor = emeraldGreen,
        badge = if (isBengali) "জিরো ক্লাউড স্টোরেজ" else "ZERO CLOUD HARVESTING",
        title = if (isBengali) "১. ১০০% অন-ডিভাইস ডেটা ভল্ট" else "1. 100% On-Device Local Vault",
        body = if (isBengali) {
            "• আপনার কোন অ্যাপ কতক্ষণ চালিয়েছেন, ফোকাস সেশন, স্ট্রিক এবং জার্নাল নোট — সবকিছুই আপনার ফোনের ভেতরে সুরক্ষিত SQLite (Room) ডেটাবেসে জমা থাকে।\n• আমাদের কোনো রিমোট সার্ভার নেই যেখানে আপনার স্ক্রিন-টাইম বা ব্যক্তিগত অভ্যাস আপলোড করা হয়। অ্যাকাউন্ট খোলা বা ইমেইল দেওয়ারও কোনো বাধ্যবাধকতা নেই।"
        } else {
            "• Every screen-time record, app limit, focus streak, and reflection journal entry is stored strictly on your phone in a local SQLite (Room) database.\n• We never upload your app usage history or personal schedule to any external server or data broker. No account sign-up is ever required."
        }
    )

    LegalSectionCard(
        icon = Icons.Default.VisibilityOff,
        iconColor = cyanAccent,
        badge = if (isBengali) "কীস্ট্রোক ও মেসেজ সুরক্ষিত" else "ZERO SCREEN READING",
        title = if (isBengali) "২. অ্যাক্সেসিবিলিটি সার্ভিস কীভাবে কাজ করে?" else "2. Why Accessibility Service Is Safe",
        body = if (isBengali) {
            "• FocusLock শুধুমাত্র এইটুকু দেখে যে বর্তমানে কোন অ্যাপটি ওপেন করা হয়েছে (যেমন: YouTube বা Instagram), যাতে আপনার নির্ধারিত সময় শেষ হলে সাথে সাথে লক স্ক্রিন দেখানো যায় এবং স্ট্রিক্ট মোডে আনইনস্টল আটকানো যায়।\n• আমরা কখনোই আপনার ব্যক্তিগত চ্যাট, মেসেজ, পাসওয়ার্ড, ব্যাংকিং তথ্য বা টাইপিং পড়ি না বা সংরক্ষণ করি না।"
        } else {
            "• FocusLock uses Android's Accessibility API exclusively to detect the foreground app package name (to trigger your Focus Shield in real time) and guard against impulsive Force-Stop/Uninstall attempts.\n• We NEVER read, log, or transmit your personal chats, passwords, keystrokes, photos, or banking screens."
        }
    )

    LegalSectionCard(
        icon = Icons.Default.AdminPanelSettings,
        iconColor = purpleAccent,
        badge = if (isBengali) "পারমিশন স্বচ্ছতা" else "PERMISSION TRANSPARENCY",
        title = if (isBengali) "৩. প্রতিটি পারমিশনের সুনির্দিষ্ট কাজ" else "3. Every Permission Has One Honest Job",
        body = if (isBengali) {
            "• Usage Access: শুধুমাত্র আপনার ডেইলি ইউজেজ ও ফোকাস স্কোর হিসাব করার জন্য।\n• Display Over Other Apps: লিমিট শেষ হলে ডিস্ট্রাক্টিং অ্যাপের ওপর মাইন্ডফুল লক স্ক্রিন দেখানোর জন্য।\n• Do Not Disturb (DND): শুধুমাত্র ডিপ ফোকাস চলাকালীন অপ্রয়োজনীয় নোটিফিকেশন সাইলেন্ট রাখতে।"
        } else {
            "• Usage Access: Calculates your daily app time and Focus Score accurately on-device.\n• Display Over Other Apps: Shows the calming Focus Shield over restricted apps when your quota is up.\n• Do Not Disturb (DND): Silences distracting pings during Deep Focus and restores them automatically when you finish."
        }
    )

    LegalSectionCard(
        icon = Icons.Default.ImportExport,
        iconColor = amberGold,
        badge = if (isBengali) "সম্পূর্ণ আপনার নিয়ন্ত্রণ" else "YOUR DATA, YOUR CONTROL",
        title = if (isBengali) "৪. ব্যাকআপ, এক্সপোর্ট ও ডিলিট করার পূর্ণ স্বাধীনতা" else "4. Full Ownership, Backup & Instant Wipe",
        body = if (isBengali) {
            "• আপনি যেকোনো সময় Settings → Data & Backup থেকে আপনার সব ডেটা নিজের ফোনে ফাইল হিসেবে ব্যাকআপ নিতে পারেন অথবা এক ক্লিকেই রিসেট করতে পারেন। আপনার ডেটার মালিক একমাত্র আপনি।"
        } else {
            "• You own your data completely. Export your settings and progress as a local backup file anytime from Settings → Data & Backup, or wipe your history in one tap."
        }
    )
}

@Composable
private fun TermsContent(
    isBengali: Boolean,
    cyanAccent: Color,
    emeraldGreen: Color,
    purpleAccent: Color,
    amberGold: Color
) {
    // Hero Pact Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        emeraldGreen.copy(alpha = 0.15f),
                        cyanAccent.copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.dp, emeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isBengali) {
                    "নিজের সাথে নিজের ফোকাস চুক্তি (The Focus Pact) 🤝"
                } else {
                    "The Focus Pact: Your Commitment to Yourself 🤝"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
            Text(
                text = if (isBengali) {
                    "FocusLock কোনো সাধারণ অ্যাপ নয়—এটি আপনার ডিজিটাল ডিসিপ্লিন পার্টনার। যখন আপনার ইচ্ছাশক্তি দুর্বল হয়ে পড়ে, তখন আপনারই সেট করা নিয়ম দিয়ে আপনাকে লক্ষ্যে অটল রাখাই এই অ্যাপের কাজ।"
                } else {
                    "FocusLock is your personal digital discipline partner. By using FocusLock, you are setting intentional boundaries for your future self so that impulsive scrolling never steals your best hours."
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }

    LegalSectionCard(
        icon = Icons.Default.Psychology,
        iconColor = cyanAccent,
        badge = if (isBengali) "সচেতন অভ্যাস" else "INTENTIONAL FRICTION",
        title = if (isBengali) "১. মাইন্ডফুল চ্যালেঞ্জ ও ফোকাস লক" else "1. Mindful Challenges & Self-Imposed Locks",
        body = if (isBengali) {
            "• আপনি নিজেই নির্ধারণ করেন কোন অ্যাপে কত মিনিট সময় দেবেন এবং কোন চ্যালেঞ্জ (Math, Typing, Focus Wait বা PIN) দিয়ে আনলক করবেন।\n• ডিপ ফোকাস চলাকালীন অ্যাপটি আপনাকে অন্য ট্যাবে বা সীমাবদ্ধ অ্যাপে যেতে দেবে না—কারণ আপনি নিজেই সেই সময়টুকু নিরবচ্ছিন্ন কাজের জন্য লক করেছেন।"
        } else {
            "• You choose which apps to limit and which cognitive challenge (Math, Typing, Countdown, or PIN) is required for temporary unlocks.\n• During an active Deep Focus session, FocusLock locks navigation to the Focus timer so you can finish what you started without impulse detours."
        }
    )

    LegalSectionCard(
        icon = Icons.Default.Security,
        iconColor = purpleAccent,
        badge = if (isBengali) "এসকেপ প্রিভেনশন" else "STRICT TAMPER GUARD",
        title = if (isBengali) "২. অ্যান্টি-ডিলিট ও এসকেপ প্রিভেনশন সম্মতি" else "2. Escape Prevention & Anti-Delete Protection",
        body = if (isBengali) {
            "• আপনি যখন Escape Prevention বা Anti-Delete Protection চালু করেন, তখন FocusLock সিস্টেম সেটিংস থেকে হঠাৎ Force Stop বা Uninstall করার চেষ্টা আটকে দেবে এবং ভেরিফিকেশন চ্যালেঞ্জ চাইবে।\n• এটি কোনো ম্যালওয়্যার আচরণ নয়, বরং আপনার নিজের দেওয়া নির্দেশ যাতে আপনি ঝোঁকের মাথায় নিজের ফোকাস রুটিন ভেঙে না ফেলেন।"
        } else {
            "• Enabling Anti-Delete Protection and Stable Lock Mode instructs FocusLock to intercept impulsive Force-Stop or Uninstall attempts in System Settings and require your verification challenge before turning protections off.\n• This strict guard is 100% user-authorized to protect your streak when willpower dips."
        }
    )

    LegalSectionCard(
        icon = Icons.Default.MedicalServices,
        iconColor = emeraldGreen,
        badge = if (isBengali) "জরুরী নিরাপত্তা গ্যারান্টি" else "SAFETY & EMERGENCY GUARANTEE",
        title = if (isBengali) "৩. ফোন কল ও জরুরী আনলক সবসময় খোলা" else "3. Essential Calls & Emergency Unlocks",
        body = if (isBengali) {
            "• আপনার বাস্তব জীবনের নিরাপত্তা সবার আগে। ফোনের ডায়ালার, জরুরী কল এবং সিস্টেম ইউআই কখনোই ব্লক করা হয় না।\n• এছাড়া প্রতিদিন নির্ধারিত সংখ্যক Emergency Unlock রয়েছে, যাতে সত্যিকারের জরুরী প্রয়োজনে আপনি চ্যালেঞ্জ ছাড়াই ৫ মিনিটের জন্য অ্যাপ ব্যবহার করতে পারেন।"
        } else {
            "• Your real-world safety always comes first: phone dialers, emergency calls, and core system UI are never blocked.\n• Configurable daily Emergency Unlocks ensure you always have immediate 5-minute access if a genuine urgent situation arises."
        }
    )

    LegalSectionCard(
        icon = Icons.Default.EmojiEvents,
        iconColor = amberGold,
        badge = if (isBengali) "স্ট্রিক ও প্রিমিয়াম" else "STREAKS, BADGES & FAIR USE",
        title = if (isBengali) "৪. স্ট্রিক, অ্যাচিভমেন্ট কার্ড ও প্রিমিয়াম সুবিধা" else "4. Streaks, Shareable Cards & Fair Use",
        body = if (isBengali) {
            "• আপনার ফোকাস স্ট্রিক এবং শেয়ারযোগ্য অ্যাচিভমেন্ট কার্ড আপনার আসল ফোকাস সেশন ও লক্ষ্য পূরণের ওপর ভিত্তি করে তৈরি হয়।\n• ফ্রি সংস্করণে অ্যাপের ডেভেলপমেন্ট চালু রাখতে হালকা ব্যানার অ্যাড থাকতে পারে (ফোকাস সেশন চলাকালীন কোনো অ্যাড থাকে না), এবং প্রিমিয়ামে আজীবন সম্পূর্ণ অ্যাড-মুক্ত অভিজ্ঞতা পাওয়া যায়।"
        } else {
            "• Focus streaks, XP levels, and high-resolution Shareable Achievement Cards reflect your real focus milestones.\n• Focus Mode sessions are always distraction-free, and upgrading to FocusLock Premium removes all banner ads permanently."
        }
    )
}

@Composable
private fun LegalSectionCard(
    icon: ImageVector,
    iconColor: Color,
    badge: String,
    title: String,
    body: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x16FFFFFF))
            .border(0.8.dp, Color(0x2EFFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.18f))
                        .border(0.8.dp, iconColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badge,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconColor,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White
                    )
                }
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp
                ),
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}
