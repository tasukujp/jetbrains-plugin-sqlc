package io.github.tasukujp.sqlc

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path

private val CONFIG_FILE_NAMES = listOf("sqlc.yaml", "sqlc.yml", "sqlc.json")

internal fun isSqlcQueryFile(file: PsiFile): Boolean =
    CachedValuesManager.getCachedValue(file) {
        val virtualFile = file.originalFile.virtualFile
        val config = virtualFile?.let(::findNearestConfig)
        val isQueryFile = virtualFile != null && config != null && isListedInConfig(virtualFile, config)
        // 設定ファイルの追加・削除も結果を変えるので、内容の変更と構造の変更の両方で破棄する
        val dependencies = listOfNotNull(config, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS, file)
        CachedValueProvider.Result.create(isQueryFile, dependencies)
    }

private fun findNearestConfig(file: VirtualFile): VirtualFile? =
    generateSequence(file.parent) { it.parent }
        .firstNotNullOfOrNull { dir -> CONFIG_FILE_NAMES.firstNotNullOfOrNull(dir::findChild) }

private fun isListedInConfig(file: VirtualFile, config: VirtualFile): Boolean {
    val text = try {
        VfsUtilCore.loadText(config)
    } catch (_: IOException) {
        return false
    }
    return try {
        val filePath = Path.of(file.path)
        val configDir = Path.of(config.parent.path)
        parseQueryPaths(text)
            .map { configDir.resolve(it).normalize() }
            .any { it == filePath || it == filePath.parent }
    } catch (_: InvalidPathException) {
        false
    }
}

// version 2 は `sql`、version 1 は `packages` の下に queries を持つ。json も yaml として読める
internal fun parseQueryPaths(configText: String): List<String> {
    val root = try {
        Yaml(SafeConstructor(LoaderOptions())).load<Any?>(configText) as? Map<*, *>
    } catch (_: YAMLException) {
        null
    } ?: return emptyList()
    return listOf("sql", "packages")
        .flatMap { (root[it] as? List<*>).orEmpty() }
        .flatMap { entry ->
            when (val queries = (entry as? Map<*, *>)?.get("queries")) {
                is String -> listOf(queries)
                is List<*> -> queries.filterIsInstance<String>()
                else -> emptyList()
            }
        }
}
