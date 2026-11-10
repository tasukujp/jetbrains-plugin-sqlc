package io.github.tasukujp.sqlc

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.postgres.PgDialect
import com.intellij.sql.inspections.SqlAmbiguousColumnInspection
import com.intellij.sql.inspections.SqlResolveInspection
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val DDL = """
CREATE TABLE parent (id uuid, tenant_id uuid);
CREATE TABLE child (id uuid, tenant_id uuid);
"""

class SqlcInspectionSuppressorTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SqlResolveInspection(), SqlAmbiguousColumnInspection())
        SqlDialectMappings.getInstance(project).setMapping(null, PgDialect.INSTANCE)
        myFixture.addFileToProject("sqlc.yaml", "version: \"2\"\nsql:\n  - queries:\n      - \"sql/listed.sql\"\n")
    }

    fun `test at parameter is not reported as unresolved column`() {
        assertEquals(emptyList<String>(), errorsIn("sql/listed.sql", "SELECT id FROM parent WHERE id = ANY(@ids::uuid[]);"))
    }

    fun `test at parameter named like a column is not reported as ambiguous`() {
        val sql = "SELECT p.id FROM parent p JOIN child c ON c.id = p.id WHERE p.tenant_id = @tenant_id;"
        assertEquals(emptyList<String>(), errorsIn("sql/listed.sql", sql))
    }

    fun `test sqlc macros are not reported`() {
        val sql = "SELECT id FROM parent WHERE id = sqlc.arg('id')::uuid AND tenant_id = sqlc.narg(tenant)::uuid;"
        assertEquals(emptyList<String>(), errorsIn("sql/listed.sql", sql))
    }

    fun `test positional parameters are not reported`() {
        val sql = "SELECT id FROM parent WHERE tenant_id = @tenant_id::uuid ORDER BY id LIMIT ${'$'}1 OFFSET ${'$'}2;"
        assertEquals(emptyList<String>(), errorsIn("sql/listed.sql", sql))
    }

    fun `test positional parameter in file not listed in sqlc config is still reported`() {
        assertSize(1, errorsIn("sql/other.sql", "SELECT id FROM parent WHERE tenant_id = ${'$'}1;"))
    }

    fun `test parameters are highlighted`() {
        val sql = "SELECT id FROM parent WHERE tenant_id = @tenant_id::uuid AND id = sqlc.arg('id') " +
            "AND id = sqlc.narg(\"id\") AND id = ANY(sqlc.slice(ids)) AND id = @ id LIMIT ${'$'}1 OFFSET ${'$'}12;"
        val expected = listOf(
            "@tenant_id" to SQLC_PARAMETER,
            "sqlc" to SQLC_MACRO_QUALIFIER, "arg" to SQLC_MACRO_NAME,
            "sqlc" to SQLC_MACRO_QUALIFIER, "narg" to SQLC_MACRO_NAME,
            "sqlc" to SQLC_MACRO_QUALIFIER, "slice" to SQLC_MACRO_NAME, "ids" to SQLC_PARAMETER,
            "${'$'}1" to SQLC_PARAMETER, "${'$'}12" to SQLC_PARAMETER,
        )
        assertEquals(expected, highlightedParametersIn("sql/listed.sql", sql))
    }

    fun `test parameters in file not listed in sqlc config are not highlighted`() {
        assertEquals(emptyList<Pair<String, TextAttributesKey>>(), highlightedParametersIn("sql/other.sql", "SELECT id FROM parent WHERE id = @id;"))
    }

    fun `test genuinely unresolved column is still reported`() {
        assertSize(1, errorsIn("sql/listed.sql", "SELECT no_such FROM parent WHERE id = @id;"))
    }

    fun `test genuinely ambiguous column is still reported`() {
        assertSize(1, errorsIn("sql/listed.sql", "SELECT tenant_id FROM parent p JOIN child c ON c.id = p.id WHERE p.id = @id;"))
    }

    fun `test abs operator with whitespace is still reported`() {
        assertSize(1, errorsIn("sql/listed.sql", "SELECT id FROM parent WHERE id = @ no_such;"))
    }

    fun `test file not listed in sqlc config is still reported`() {
        assertSize(1, errorsIn("sql/other.sql", "SELECT id FROM parent WHERE id = @no_such;"))
    }

    fun `test parses queries of v1 and v2 in string and list form`() {
        assertEquals(listOf("a.sql", "dir"), parseQueryPaths("version: 2\nsql:\n  - queries: a.sql\n  - queries: [dir]\n"))
        assertEquals(listOf("b.sql"), parseQueryPaths("""{"version": "1", "packages": [{"queries": "b.sql"}]}"""))
        assertEquals(emptyList<String>(), parseQueryPaths("sql: ["))
    }

    private fun errorsIn(path: String, query: String): List<String> =
        highlight(path, query)
            .filter { it.severity >= HighlightSeverity.WARNING }
            .mapNotNull { it.description }

    // 葉ごとに塗られるので、同じ色で隣接する範囲をつないで、塗られた文字列と色の組にする
    private fun highlightedParametersIn(path: String, query: String): List<Pair<String, TextAttributesKey>> {
        val keys = setOf(SQLC_PARAMETER, SQLC_MACRO_QUALIFIER, SQLC_MACRO_NAME)
        val infos = highlight(path, query)
            .filter { it.forcedTextAttributesKey in keys }
            .sortedBy { it.startOffset }
        val merged = infos.fold(mutableListOf<Pair<TextRange, TextAttributesKey>>()) { acc, info ->
            val range = TextRange(info.startOffset, info.endOffset)
            val key = info.forcedTextAttributesKey!!
            val last = acc.lastOrNull()
            if (last != null && last.second == key && last.first.endOffset == range.startOffset) {
                acc[acc.lastIndex] = last.first.union(range) to key
            } else {
                acc.add(range to key)
            }
            acc
        }
        val text = myFixture.editor.document.text
        return merged.map { (range, key) -> range.substring(text) to key }
    }

    private fun highlight(path: String, query: String): List<HighlightInfo> {
        val file = myFixture.addFileToProject(path, DDL + query)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return myFixture.doHighlighting()
    }
}
