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

class EnterAfterKDocGenHandler : EnterHandlerDelegateAdapter() {


    override fun postProcessEnter(file: PsiFile,
                                  editor: Editor,
                                  dataContext: DataContext): EnterHandlerDelegate.Result {

        if (file !is KtFile || !CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
            return EnterHandlerDelegate.Result.Continue
        }

        val offset = editor.caretModel.offset
        if (!isInKDoc(editor, offset)) {
            return EnterHandlerDelegate.Result.Continue
        }

        val project = file.project
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file)
        documentManager.commitDocument(document!!)

        ApplicationManager.getApplication().runWriteAction {
            val elementAtCaret = file.findElementAt(offset)

            val kdoc = PsiTreeUtil.getParentOfType(elementAtCaret, KDoc::class.java) ?: return@runWriteAction
            val kDocSection = PsiTreeUtil.getChildOfType(kdoc, KDocSection::class.java) ?: return@runWriteAction
            val func = PsiTreeUtil.getParentOfType(kDocSection, KtNamedFunction::class.java)?.valueParameters
                       ?: return@runWriteAction

            val kDocElementFactory = KDocElementFactory(project)
            val newKdoc = func.map { it.name }
                    .map { "* @param [$it]" }
                    .joinToString("\n")
                    .let { "/**\n$it\n*/" }
                    .let { kDocElementFactory.createKDocFromText(it) }
            CodeStyleManager.getInstance(project).reformat(kdoc.replaced(newKdoc))
            
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