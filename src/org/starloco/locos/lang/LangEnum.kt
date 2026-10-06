package org.starloco.locos.lang

import org.yaml.snakeyaml.Yaml

private fun loadYAML(fileName: String): Map<String, Any>? =
    Yaml().load(LangEnum::class.java.getResourceAsStream("/translations/$fileName"))

/**
 * Created by Locos on 04/04/2018.
 */
enum class LangEnum(val flag: String) {

    FRENCH("fr"),
    ENGLISH("en"),
    SPANISH("es"),
    PORTUGUESE("pt");

    private var result: Map<String, Any>? = loadYAML("${flag}_${flag.uppercase()}.yaml")

    fun trans(key: String, vararg str: Any?): String {
        val res = result
        if (res == null) {
            result = loadYAML("${flag}_${flag.uppercase()}.yaml")
            return "$key result null"
        }
        var sentence = res[key] as? String ?: return "$key not found"
        var count = 1
        for (t in str) {
            sentence = sentence.replace("#$count", t.toString())
            count++
        }
        return sentence
    }
}
