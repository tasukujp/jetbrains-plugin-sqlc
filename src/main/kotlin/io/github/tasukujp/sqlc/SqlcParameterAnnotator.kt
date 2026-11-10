package io.github.tasukujp.sqlc

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.sql.editor.SqlColors

// 既定色は colorSchemes/ で配る。そこに無いスキームでは SQL の Parameter 色を引き継ぐ
val SQLC_PARAMETER: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("SQLC_PARAMETER", SqlColors.SQL_PARAMETER)

// `sqlc.arg` を Go の `errors.Wrap` と同じ色にする。Go プラグインに依存しないよう、キーは名前で引く
val SQLC_MACRO_QUALIFIER: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("SQLC_MACRO_QUALIFIER", TextAttributesKey.find("GO_PACKAGE"))
val SQLC_MACRO_NAME: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("SQLC_MACRO_NAME", TextAttributesKey.find("GO_EXPORTED_FUNCTION_CALL"))

class SqlcParameterAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.firstChild != null) return
        val key = when (sqlcPartOf(element) ?: return) {
            SqlcPart.PARAMETER -> SQLC_PARAMETER
            SqlcPart.MACRO_QUALIFIER -> SQLC_MACRO_QUALIFIER
            SqlcPart.MACRO_NAME -> SQLC_MACRO_NAME
        }
        if (!isSqlcQueryFile(element.containingFile)) return
        // SQL プラグインは同じ範囲を列名などとして INFORMATION で塗る。TEXT_ATTRIBUTES はそれより上の層に載る
        holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
            .textAttributes(key)
            .create()
    }
}
