package io.github.tasukujp.sqlc

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement

private val SUPPRESSED_TOOL_IDS = setOf("SqlResolve", "SqlAmbiguousColumn")

class SqlcInspectionSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean =
        toolId in SUPPRESSED_TOOL_IDS &&
            isSqlcParameter(element) &&
            isSqlcQueryFile(element.containingFile)

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
