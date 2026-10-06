package org.starloco.locos.util.generator

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Objects

/**
 * This class is released under GNU general public license

 * Description: This class generates random names from syllables, and provides programmer a
 * simple way to set a group of rules for generator to avoid unpronounceable and bizarre names.

 * SYLLABLE FILE REQUIREMENTS/FORMAT:
 * 1) all syllables are separated by line break.
 * 2) Syllable should not contain or begin with whitespace, as this character is ignored and only first part of the syllable is read.
 * 3) + and - characters are used to set rules, and using them in other way, may result in unpredictable results.
 * 4) Empty lines are ignored.

 * SYLLABLE CLASSIFICATION:
 * Name is usually composed of 3 different class of syllables, which include prefix, middle part and suffix.
 * To declare syllable as a prefix in the file, insert "-" as a first character of the line.
 * To declare syllable as a suffix in the file, insert "+" as a first character of the line.
 * everything else is read as a middle part.

 * NUMBER OF SYLLABLES:
 * Names may have any positive number of syllables. In case of 2 syllables, name will be composed from prefix and suffix.
 * In case of 1 syllable, name will be chosen from amongst the prefixes.
 * In case of 3 and more syllables, name will begin with prefix, is filled with middle parts and ended with suffix.

 * ASSIGNING RULES:
 * I included a way to set 4 kind of rules for every syllable. To add rules to the syllables, write them right after the
 * syllable and SEPARATE WITH WHITESPACE. (example: "aad +v -c"). The order of rules is not important.

 * RULES:
 * 1) +v means that next syllable must definitely begin with a Vowel.
 * 2) +c means that next syllable must definitely begin with a consonant.
 * 3) -v means that this syllable can only be added to another syllable, that ends with a Vowel.
 * 4) -c means that this syllable can only be added to another syllable, that ends with a consonant.
 * So, our example: "aad +v -c" means that "aad" can only be after consonant and next syllable must begin with Vowel.
 * Beware of creating logical mistakes, like providing only syllables ending with consonants, but expecting only Vowels, which will be detected
 * and RuntimeException will be thrown.

 * TO START:
 * Create a new NameGenerator object, provide the syllable file, and create names using compose() method.
 *
 */
class NameGenerator(fileName: String) {

    private val pre = ArrayList<String>()
    private val mid = ArrayList<String>()
    private val sur = ArrayList<String>()

    init {
        try {
            refresh(fileName)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Refresh names from file. No need to call that method, if you are not changing the file during the operation of program, as this method
     * is called every time file name is changed or new NameGenerator object created.
     */
    @Throws(IOException::class)
    fun refresh(fileName: String) {
        val input = InputStreamReader(Objects.requireNonNull(NameGenerator::class.java.getResourceAsStream("/$fileName")))
        val bufRead = BufferedReader(input)
        var line: String? = ""

        while (line != null) {
            line = bufRead.readLine()
            if (line != null && line != "") {
                if (line[0] == '-') {
                    pre.add(line.substring(1).lowercase())
                } else if (line[0] == '+') {
                    sur.add(line.substring(1).lowercase())
                } else {
                    mid.add(line.lowercase())
                }
            }
        }
        bufRead.close()
    }

    private fun upper(s: String): String =
        s.substring(0, 1).uppercase() + s.substring(1)

    private fun doesNotContainsConsFirst(array: ArrayList<String>): Boolean {
        for (s in array)
            if (consonantFirst(s))
                return false
        return true
    }

    private fun doesNotContainsVocFirst(array: ArrayList<String>): Boolean {
        for (s in array)
            if (VowelFirst(s))
                return false
        return true
    }

    private fun disallowCons(array: ArrayList<String>): Boolean {
        for (s in array)
            if (hatesPreviousVowels(s) || !hatesPreviousConsonants(s))
                return false
        return true
    }

    private fun disallowVocs(array: ArrayList<String>): Boolean {
        for (s in array)
            if (hatesPreviousConsonants(s) || !hatesPreviousVowels(s))
                return false
        return true
    }

    private fun expectsVowel(s: String): Boolean = s.substring(1).contains("+v")

    private fun expectsConsonant(s: String): Boolean = s.substring(1).contains("+c")

    private fun hatesPreviousVowels(s: String): Boolean = s.substring(1).contains("-c")

    private fun hatesPreviousConsonants(s: String): Boolean = s.substring(1).contains("-v")

    private fun pureSyl(s: String): String {
        var s = s
        s = s.trim()
        if (s[0] == '+' || s[0] == '-') s = s.substring(1)
        return s.split(" ")[0]
    }

    private fun VowelFirst(s: String): Boolean =
        String(Vowels).contains(s[0].toString().lowercase())

    private fun consonantFirst(s: String): Boolean =
        String(consonants).contains(s[0].toString().lowercase())

    private fun VowelLast(s: String): Boolean =
        String(Vowels).contains(s[s.length - 1].toString().lowercase())

    private fun consonantLast(s: String): Boolean =
        String(consonants).contains(s[s.length - 1].toString().lowercase())

    /**
     * Compose a new name.
     * @param syls The number of syllables used in name.
     * @return Returns composed name as a String
     * @throws RuntimeException when logical mistakes are detected inside chosen file, and program is unable to complete the name.
     */
    fun compose(syls: Int): String {
        if (syls > 2 && mid.size == 0) throw RuntimeException("You are trying to create a name with more than 3 parts, which requires middle parts, " +
            "which you have none in the file. You should add some. Every word, which doesn't have + or - for a prefix is counted as a middle part.")
        if (pre.size == 0) throw RuntimeException("You have no prefixes to begin creating a name. add some and use \"-\" prefix, to identify it as a prefix for a name. (example: -asd)")
        if (sur.size == 0) throw RuntimeException("You have no suffixes to end a name. add some and use \"+\" prefix, to identify it as a suffix for a name. (example: +asd)")
        if (syls < 1) throw RuntimeException("compose(int syls) can't have less than 1 syllable")
        var expecting = 0 // 1 for Vowel, 2 for consonant
        var last: Int // 1 for Vowel, 2 for consonant
        var name: String
        val a = (Math.random() * pre.size).toInt()

        if (VowelLast(pureSyl(pre[a]))) last = 1
        else last = 2

        if (syls > 2) {
            if (expectsVowel(pre[a])) {
                expecting = 1
                if (doesNotContainsVocFirst(mid)) throw RuntimeException("Expecting \"middle\" part starting with Vowel, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
            }
            if (expectsConsonant(pre[a])) {
                expecting = 2
                if (doesNotContainsConsFirst(mid)) throw RuntimeException("Expecting \"middle\" part starting with consonant, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
            }
        } else {
            if (expectsVowel(pre[a])) {
                expecting = 1
                if (doesNotContainsVocFirst(sur)) throw RuntimeException("Expecting \"suffix\" part starting with Vowel, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
            }
            if (expectsConsonant(pre[a])) {
                expecting = 2
                if (doesNotContainsConsFirst(sur)) throw RuntimeException("Expecting \"suffix\" part starting with consonant, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
            }
        }
        if (VowelLast(pureSyl(pre[a])) && disallowVocs(mid)) throw RuntimeException("Expecting \"middle\" part that allows last character of prefix to be a Vowel, " +
            "but there is none. You should add one, or remove requirements that cannot be fulfilled.. the prefix used, was : \"" + pre[a] + "\", which" +
            "means there should be a part available, that has \"-v\" requirement or no requirements for previous syllables at all.")

        if (consonantLast(pureSyl(pre[a])) && disallowCons(mid)) throw RuntimeException("Expecting \"middle\" part that allows last character of prefix to be a consonant, " +
            "but there is none. You should add one, or remove requirements that cannot be fulfilled.. the prefix used, was : \"" + pre[a] + "\", which" +
            "means there should be a part available, that has \"-c\" requirement or no requirements for previous syllables at all.")

        val b = IntArray(syls)
        for (i in 0 until b.size - 2) {

            do {
                b[i] = (Math.random() * mid.size).toInt()
            } while (expecting == 1 && !VowelFirst(pureSyl(mid[b[i]])) || expecting == 2 && !consonantFirst(pureSyl(mid[b[i]]))
                || last == 1 && hatesPreviousVowels(mid[b[i]]) || last == 2 && hatesPreviousConsonants(mid[b[i]]))

            expecting = 0
            if (expectsVowel(mid[b[i]])) {
                expecting = 1
                if (i < b.size - 3 && doesNotContainsVocFirst(mid)) throw RuntimeException("Expecting \"middle\" part starting with Vowel, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
                if (i == b.size - 3 && doesNotContainsVocFirst(sur)) throw RuntimeException("Expecting \"suffix\" part starting with Vowel, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
            }
            if (expectsConsonant(mid[b[i]])) {
                expecting = 2
                if (i < b.size - 3 && doesNotContainsConsFirst(mid)) throw RuntimeException("Expecting \"middle\" part starting with consonant, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
                if (i == b.size - 3 && doesNotContainsConsFirst(sur)) throw RuntimeException("Expecting \"suffix\" part starting with consonant, " +
                    "but there is none. You should add one, or remove requirement for one.. ")
            }
            if (VowelLast(pureSyl(mid[b[i]])) && disallowVocs(mid) && syls > 3) throw RuntimeException("Expecting \"middle\" part that allows last character of last syllable to be a Vowel, " +
                "but there is none. You should add one, or remove requirements that cannot be fulfilled.. the part used, was : \"" + mid[b[i]] + "\", which " +
                "means there should be a part available, that has \"-v\" requirement or no requirements for previous syllables at all.")

            if (consonantLast(pureSyl(mid[b[i]])) && disallowCons(mid) && syls > 3) throw RuntimeException("Expecting \"middle\" part that allows last character of last syllable to be a consonant, " +
                "but there is none. You should add one, or remove requirements that cannot be fulfilled.. the part used, was : \"" + mid[b[i]] + "\", which " +
                "means there should be a part available, that has \"-c\" requirement or no requirements for previous syllables at all.")
            if (i == b.size - 3) {
                if (VowelLast(pureSyl(mid[b[i]])) && disallowVocs(sur)) throw RuntimeException("Expecting \"suffix\" part that allows last character of last syllable to be a Vowel, " +
                    "but there is none. You should add one, or remove requirements that cannot be fulfilled.. the part used, was : \"" + mid[b[i]] + "\", which " +
                    "means there should be a suffix available, that has \"-v\" requirement or no requirements for previous syllables at all.")

                if (consonantLast(pureSyl(mid[b[i]])) && disallowCons(sur)) throw RuntimeException("Expecting \"suffix\" part that allows last character of last syllable to be a consonant, " +
                    "but there is none. You should add one, or remove requirements that cannot be fulfilled.. the part used, was : \"" + mid[b[i]] + "\", which " +
                    "means there should be a suffix available, that has \"-c\" requirement or no requirements for previous syllables at all.")
            }
            if (VowelLast(pureSyl(mid[b[i]]))) last = 1
            else last = 2
        }

        var c: Int
        do {
            c = (Math.random() * sur.size).toInt()
        } while (expecting == 1 && !VowelFirst(pureSyl(sur[c])) || expecting == 2 && !consonantFirst(pureSyl(sur[c]))
            || last == 1 && hatesPreviousVowels(sur[c]) || last == 2 && hatesPreviousConsonants(sur[c]))

        name = upper(pureSyl(pre[a].lowercase()))
        for (i in 0 until b.size - 2) {
            name += pureSyl(mid[b[i]].lowercase())
        }
        if (syls > 1)
            name += pureSyl(sur[c].lowercase())
        return name
    }

    companion object {
        @JvmField
        var nameGenerator: NameGenerator = NameGenerator("names.properties")

        private val Vowels = charArrayOf('a', 'e', 'i', 'o', 'u', 'é', 'è', 'à', 'ù', 'y')
        private val consonants = charArrayOf('b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y')
    }
}
