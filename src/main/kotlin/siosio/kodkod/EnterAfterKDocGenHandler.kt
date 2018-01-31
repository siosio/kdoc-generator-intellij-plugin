package siosio.kodkod

import com.intellij.codeInsight.*
import com.intellij.codeInsight.editorActions.enter.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.*
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import com.intellij.psi.codeStyle.*
import com.intellij.psi.util.*
import com.intellij.util.text.*
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.kdoc.*
import org.jetbrains.kotlin.kdoc.psi.api.*
import org.jetbrains.kotlin.kdoc.psi.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

/**
 *
 */
class EnterAfterKDocGenHandler : EnterHandlerDelegateAdapter() {


    override fun postProcessEnter(file: PsiFile,
                                  editor: Editor,
                                  dataContext: DataContext): EnterHandlerDelegate.Result {

        if (file !is KtFile || !CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
            return EnterHandlerDelegate.Result.Continue
        }

        val caretModel = editor.caretModel
        if (!isInKDoc(editor, caretModel.offset)) {
            return EnterHandlerDelegate.Result.Continue
        }

        val project = file.project
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitAllDocuments()

        val elementAtCaret = file.findElementAt(caretModel.offset)
        val kdoc = PsiTreeUtil.getParentOfType(elementAtCaret, KDoc::class.java)
                   ?: return EnterHandlerDelegate.Result.Continue
        val kdocSection = kdoc.getChildOfType<KDocSection>() ?: return EnterHandlerDelegate.Result.Continue
        // KDocのセクションが空(*だけ)以外の場合は処理しない。
        if (kdocSection.text.trim() != "*") {
            return EnterHandlerDelegate.Result.Continue
        }

        ApplicationManager.getApplication().runWriteAction {
            val kDocElementFactory = KDocElementFactory(project)

            val parent = kdoc.parent
            when (parent) {
                is KtNamedFunction -> {
                    val params: List<PsiNameIdentifierOwner> = parent.typeParameters + parent.valueParameters
                    if (!params.isEmpty()) {
                        params.map { it.name }
                                .map { "@param $it" }
                                .joinToString("\n", transform = { "* $it" })
                                .let { "/**\n* TODO\n$it\n*/" }
                                .let { kDocElementFactory.createKDocFromText(it) }
                                .let { kdoc.replace(it) }
                                .let { CodeStyleManager.getInstance(project).reformat(it) }
                    } else {
                        null
                    }
                }
                else -> null
            }?.let {
                it.getChildOfType<KDocSection>()?.let {
                    caretModel.moveToOffset(it.textOffset + 6)
                }
            }
        }
        return EnterHandlerDelegate.Result.Continue
    }

    private fun isInKDoc(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val docChars = document.charsSequence
        var i = CharArrayUtil.lastIndexOf(docChars, "/**", offset)
        if (i >= 0) {
            i = CharArrayUtil.indexOf(docChars, "*/", i)
            return i > offset
        }
        return false
    }
}