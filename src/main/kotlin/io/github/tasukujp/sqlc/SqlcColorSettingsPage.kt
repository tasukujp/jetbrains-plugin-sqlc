package io.github.tasukujp.sqlc

import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class SqlcColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "sqlc"

    override fun getIcon(): Icon? = null

    override fun getHighlighter(): SyntaxHighlighter =
        Language.findLanguageByID("SQL")
            ?.let { SyntaxHighlighterFactory.getSyntaxHighlighter(it, null, null) }
            ?: PlainSyntaxHighlighter()

    override fun getDemoText(): String = """
        -- name: ListProducts :many
        SELECT id FROM product
        WHERE tenant_id = <param>@tenant_id</param>::uuid
          AND id = ANY(<qualifier>sqlc</qualifier>.<macro>slice</macro>(<param>ids</param>))
          AND manufacturer_id = <qualifier>sqlc</qualifier>.<macro>narg</macro>('manufacturer_id')
        LIMIT <param>${'$'}1</param> OFFSET <param>${'$'}2</param>;
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        mapOf("param" to SQLC_PARAMETER, "qualifier" to SQLC_MACRO_QUALIFIER, "macro" to SQLC_MACRO_NAME)

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> =
        arrayOf(
            AttributesDescriptor("Parameter", SQLC_PARAMETER),
            AttributesDescriptor("Macro//Qualifier (sqlc)", SQLC_MACRO_QUALIFIER),
            AttributesDescriptor("Macro//Name (arg, narg, slice, embed)", SQLC_MACRO_NAME),
        )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
}
