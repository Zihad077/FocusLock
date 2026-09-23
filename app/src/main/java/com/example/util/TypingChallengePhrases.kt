package com.example.util

import android.content.Context
import java.util.Locale
import kotlin.random.Random

/**
 * Curated repository of 20 inspiring focus affirmations and sentences
 * for mindful unlock typing challenges.
 */
object TypingChallengePhrases {

    val ENGLISH_PHRASES = listOf(
        "I choose to be present and focused right now.",
        "My time and energy are valuable and intentional.",
        "Deep work creates meaningful and lasting results.",
        "I control my technology, it does not control me.",
        "One mindful breath brings me back to clarity.",
        "Small steps of discipline lead to great success.",
        "I will resist temporary distraction for future growth.",
        "Focus is the superpower that unlocks my true potential.",
        "I am in complete control of my digital habits.",
        "Clear mind, steady focus, and purposeful action.",
        "I prioritize what truly matters for my life goals.",
        "Patience and consistency build unbroken momentum.",
        "Today I dedicate my attention to deep learning.",
        "I let go of mindless scrolling and embrace creation.",
        "Every moment of focus is an investment in myself.",
        "Silence the noise to hear your inner ambition.",
        "Discipline today brings absolute freedom tomorrow.",
        "I pause, breathe, and consciously choose my next action.",
        "Distraction is easy, but greatness requires focus.",
        "My attention is my greatest currency; I spend it wisely."
    )

    val BENGALI_PHRASES = listOf(
        "আমি এখনই বর্তমান মুহূর্তে মনোযোগী থাকতে বেছে নিচ্ছি।",
        "আমার সময় ও শক্তি অত্যন্ত মূল্যবান এবং অর্থপূর্ণ।",
        "গভীর মনোযোগ দীর্ঘস্থায়ী সাফল্য তৈরি করে।",
        "আমি আমার প্রযুক্তিকে নিয়ন্ত্রণ করি, প্রযুক্তি আমাকে নয়।",
        "একটি গভীর শ্বাস আমাকে পুনরায় সতেজ করে তোলে।",
        "প্রতিদিনের ছোট ছোট শৃঙ্খলা বড় বিজয়ের পথ তৈরি করে।",
        "আমি ক্ষণিকের বিভ্রান্তি এড়িয়ে আসল লক্ষ্যের দিকে এগোব।",
        "মনোযোগ হলো একটি পরাশক্তি যা আমার সম্ভাবনা উন্মোচন করে।",
        "আমার ডিজিটাল অভ্যাসের পূর্ণ নিয়ন্ত্রণ আমার হাতে।",
        "শান্ত মন, স্থির মনোযোগ এবং উদ্দেশ্যপূর্ণ কর্ম।",
        "জীবনের আসল লক্ষ্যের প্রতি আমি গুরুত্ব দিচ্ছি।",
        "ধৈর্য এবং ধারাবাহিকতা অপ্রতিরোধ্য শক্তি তৈরি করে।",
        "আজকে আমি আমার মনোযোগ গভীর শিক্ষায় ব্যয় করব।",
        "অর্থহীন স্ক্রোলিং ছেড়ে আমি সৃজনশীল কাজে যুক্ত হব।",
        "মনোযোগের প্রতিটি মুহূর্ত নিজের জন্য এক অমূল্য সঞ্চয়।",
        "বাহিরের কোলাহল থামিয়ে নিজের আত্মবিশ্বাস শুনুন।",
        "আজকের আত্মনিয়ন্ত্রণ ভবিষ্যতের পরম স্বাধীনতা।",
        "আমি থামছি, শ্বাস নিচ্ছি এবং সঠিক সিদ্ধান্ত নিচ্ছি।",
        "বিভ্রান্তি সহজ কিন্তু সাফল্য সবসময় কঠিন মনোযোগ চায়।",
        "আমার মনোযোগই আমার শ্রেষ্ঠ সম্পদ, আমি তা সচেতনভাবে ব্যবহার করব।"
    )

    val SPANISH_PHRASES = listOf(
        "Elijo estar presente y enfocado ahora mismo.",
        "Mi tiempo y energía son valiosos e intencionales.",
        "El trabajo profundo crea resultados significativos y duraderos.",
        "Yo controlo mi tecnología, no me controla a mí.",
        "Una respiración consciente me devuelve la claridad.",
        "Pequeños pasos de disciplina conducen al gran éxito.",
        "Resistiré la distracción temporal para mi crecimiento futuro.",
        "El enfoque es el superpoder que desbloquea mi potencial.",
        "Tengo el control total de mis hábitos digitales.",
        "Mente clara, enfoque constante y acción decidida.",
        "Priorizo lo que realmente importa para mis metas de vida.",
        "La paciencia y la constancia construyen un impulso imparable.",
        "Hoy dedico mi atención al aprendizaje profundo.",
        "Dejo el desplazamiento sin sentido y abrazo la creación.",
        "Cada momento de concentración es una inversión en mí mismo.",
        "Silencia el ruido para escuchar tu ambición interior.",
        "La disciplina de hoy trae libertad absoluta mañana.",
        "Hago una pausa, respiro y elijo conscientemente mi acción.",
        "La distracción es fácil, pero la grandeza requiere enfoque.",
        "Mi atención es mi mayor riqueza; la uso con sabiduría."
    )

    fun getRandomPhrase(languageCode: String): String {
        val list = when (languageCode.lowercase(Locale.ROOT)) {
            "bn" -> BENGALI_PHRASES
            "es" -> SPANISH_PHRASES
            else -> ENGLISH_PHRASES
        }
        return list[Random.nextInt(list.size)]
    }

    fun getAllPhrases(languageCode: String): List<String> {
        return when (languageCode.lowercase(Locale.ROOT)) {
            "bn" -> BENGALI_PHRASES
            "es" -> SPANISH_PHRASES
            else -> ENGLISH_PHRASES
        }
    }
}
