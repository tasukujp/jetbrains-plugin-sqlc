package io.github.tasukujp.sqlc

import com.intellij.lang.annotation.HighlightSeverity
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

    private fun errorsIn(path: String, query: String): List<String> {
        val file = myFixture.addFileToProject(path, DDL + query)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return myFixture.doHighlighting(HighlightSeverity.WARNING).mapNotNull { it.description }
    }
}
