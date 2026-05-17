package siosio.kodkod

import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

interface KDocGenerator {

    companion object {
        const val LF = "\n"
    }

    fun generate(): String

    fun toParamsKdoc(keyword: String = "@param", params: List<PsiNameIdentifierOwner>): String =
        params
            .map { "$keyword ${it.name}" }
            .joinToString(LF) { "* $it" }
}

class NamedFunctionKDocGenerator(private val function: KtNamedFunction) : KDocGenerator {
    override fun generate(): String {
        val builder = StringBuilder()
        builder
            .appendLine("/**")
            .appendLine("* TODO")
            .appendLine("*")
        if (function.typeParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = function.typeParameters))
        }
        if (function.valueParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = function.valueParameters))
        }

        val returnType = function.typeReference
        if (returnType != null && !isUnitType(returnType.text)) {
            builder.appendLine("* @return")
        }
        builder.appendLine("*/")
        return builder.toString()
    }

    private fun isUnitType(typeText: String): Boolean {
        val normalized = typeText.trim()
        return normalized == "Unit" || normalized == "kotlin.Unit"
    }
}

class ClassKDocGenerator(private val klass: KtClass) : KDocGenerator {
    override fun generate(): String {
        val builder = StringBuilder()
        builder
            .appendLine("/**")
            .appendLine("* TODO")
            .appendLine("*")

        if (klass.typeParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = klass.typeParameters))
        }

        val (properties, parameters) = klass.primaryConstructor?.valueParameters?.partition {
            it.hasValOrVar()
        } ?: Pair(emptyList(), emptyList())

        if (properties.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(keyword = "@property", params = properties))
        }

        if (parameters.isNotEmpty()) {
            builder
                .appendLine("* @constructor")
                .appendLine("* TODO")
                .appendLine("*")
                .appendLine(toParamsKdoc(params = parameters))
        }

        builder.appendLine("*/")
        return builder.toString()
    }
}
