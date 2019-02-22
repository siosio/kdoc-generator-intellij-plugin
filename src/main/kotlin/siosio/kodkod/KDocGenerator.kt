package siosio.kodkod

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*

interface KDocGenerator {

    companion object {
        const val LF = "\n"
    }

    fun generate(): String

    fun toParamsKdoc(keyword: String = "@param", params: List<PsiNameIdentifierOwner>): String =
            params.map { "$keyword ${it.name}" }
                    .joinToString(LF, transform = { "* $it" })

    fun StringBuilder.appendLine(text: String): StringBuilder = append(text).append(LF)
}

class NamedFunctionKDocGenerator(private val function: KtNamedFunction) : KDocGenerator {
    override fun generate(): String {
        val builder = StringBuilder()
        builder.appendLine("/**")
                .appendLine("* TODO")
                .appendLine("*")
        if (function.typeParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = function.typeParameters))
        }
        if (function.valueParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = function.valueParameters))
        }
        function.typeReference?.let {
            if (it.text != "Unit") {
                builder.appendLine("* @return")
            }
        }
        builder.appendLine("*/")
        return builder.toString()
    }
}

class ClassKDocGenerator(private val klass: KtClass) : KDocGenerator {
    override fun generate(): String {
        val builder = StringBuilder()
        builder.appendLine("/**")
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
            builder.appendLine("* @constructor")
                    .appendLine("* TODO")
                    .appendLine("*")
                    .appendLine(toParamsKdoc(params = parameters))
        }
        
        builder.appendLine("*/")
        return builder.toString()
    }
}