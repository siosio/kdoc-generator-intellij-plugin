package siosio.kodkod

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class EnterAfterKDocGenHandler : EnterHandlerDelegateAdapter() {

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): EnterHandlerDelegate.Result {
        if (file !is KtFile || !CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
            return EnterHandlerDelegate.Result.Continue
        }

        val caretOffset = editor.caretModel.offset
        val kdoc = findKDocAtCaret(file, caretOffset) ?: return EnterHandlerDelegate.Result.Continue
        val kdocSection = PsiTreeUtil.getChildOfType(kdoc, KDocSection::class.java)
            ?: return EnterHandlerDelegate.Result.Continue
        // 空の KDoc のときだけ生成する。
        if (!isEffectivelyEmptyKDocSection(kdocSection)) {
            return EnterHandlerDelegate.Result.Continue
        }

        val project = file.project
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitAllDocuments()

        WriteCommandAction.runWriteCommandAction(project) {
            val factory = KDocElementFactory(project)
            val generated = when (val parent = kdoc.parent) {
                is KtNamedFunction -> NamedFunctionKDocGenerator(parent).generate()
                is KtClass -> ClassKDocGenerator(parent).generate()
                else -> null
            } ?: return@runWriteCommandAction

            val replaced = kdoc.replace(factory.createKDocFromText(generated))
            val reformatted = CodeStyleManager.getInstance(project).reformat(replaced)
            moveCaretToTodo(editor, reformatted.text, reformatted.textOffset)
        }
        return EnterHandlerDelegate.Result.Continue
    }

    private fun findKDocAtCaret(file: KtFile, caretOffset: Int): KDoc? {
        val elementAtCaret = file.findElementAt(caretOffset)
        val elementBeforeCaret = file.findElementAt((caretOffset - 1).coerceAtLeast(0))
        return PsiTreeUtil.getParentOfType(elementAtCaret, KDoc::class.java)
            ?: PsiTreeUtil.getParentOfType(elementBeforeCaret, KDoc::class.java)
    }

    private fun isEffectivelyEmptyKDocSection(section: KDocSection): Boolean {
        val normalized = section.text
            .lineSequence()
            .map { it.trim() }
            .joinToString("")
            .replace("*", "")
            .replace("/", "")
        return normalized.isBlank()
    }

    private fun moveCaretToTodo(editor: Editor, kdocText: String, kdocTextOffset: Int) {
        val todoIndex = kdocText.indexOf("TODO")
        if (todoIndex >= 0) {
            editor.caretModel.moveToOffset(kdocTextOffset + todoIndex)
        }
    }
}
