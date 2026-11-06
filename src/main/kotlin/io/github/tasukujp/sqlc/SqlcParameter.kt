package io.github.tasukujp.sqlc

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private val SQLC_MACROS = setOf("arg", "narg", "slice", "embed")

internal fun isSqlcParameter(element: PsiElement): Boolean {
    val leaf = PsiTreeUtil.getDeepestFirst(element)
    return isAtParameter(leaf) || isMacroQualifier(leaf) || isUnquotedMacroArgument(leaf)
}

// `@ x` は PostgreSQL の絶対値演算子なので、空白を挟まない `@x` だけをパラメータとして扱う
private fun isAtParameter(leaf: PsiElement): Boolean =
    PsiTreeUtil.prevLeaf(leaf)?.text == "@"

private fun isMacroQualifier(leaf: PsiElement): Boolean {
    if (leaf.text != "sqlc") return false
    val dot = PsiTreeUtil.nextLeaf(leaf) ?: return false
    val macro = PsiTreeUtil.nextLeaf(dot) ?: return false
    val paren = PsiTreeUtil.nextLeaf(macro) ?: return false
    return dot.text == "." && macro.text in SQLC_MACROS && paren.text == "("
}

private fun isUnquotedMacroArgument(leaf: PsiElement): Boolean {
    val paren = PsiTreeUtil.prevLeaf(leaf) ?: return false
    val macro = PsiTreeUtil.prevLeaf(paren) ?: return false
    val dot = PsiTreeUtil.prevLeaf(macro) ?: return false
    val qualifier = PsiTreeUtil.prevLeaf(dot) ?: return false
    return paren.text == "(" && macro.text in SQLC_MACROS && dot.text == "." && qualifier.text == "sqlc"
}
