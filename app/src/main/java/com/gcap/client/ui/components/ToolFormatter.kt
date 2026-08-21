package com.gcap.client.ui.components

fun formatToolCalls(
    functionCalls: List<String>,
    webSearchQueries: List<String>,
    groundingSources: List<Pair<String, String>>
): String {
    val sb = StringBuilder()
    if (webSearchQueries.isNotEmpty()) {
        sb.append("🔍 **搜索检索词**:\n")
        webSearchQueries.distinct().forEach { q ->
            sb.append("- `$q`\n")
        }
    }
    if (groundingSources.isNotEmpty()) {
        if (sb.isNotEmpty()) sb.append("\n")
        sb.append("🌐 **参考网页来源**:\n")
        groundingSources.distinctBy { it.second }.forEach { (title, uri) ->
            sb.append("- [$title]($uri)\n")
        }
    }
    if (functionCalls.isNotEmpty()) {
        if (sb.isNotEmpty()) sb.append("\n")
        sb.append("⚙️ **调用工具**:\n")
        functionCalls.distinct().forEach { fc ->
            sb.append("- `$fc`\n")
        }
    }
    return sb.toString().trim()
}
