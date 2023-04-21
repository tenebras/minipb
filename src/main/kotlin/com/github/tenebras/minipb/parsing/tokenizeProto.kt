package com.github.tenebras.minipb.parsing

import org.intellij.lang.annotations.Language

private val singleCharTokens = setOf('=', ';', '(', ')', '[', ']', '{', '}', ',', '/', '<', '>')
private val nameCharCodes = listOf(
    '0'.code..'9'.code, // 0-9
    'A'.code..'Z'.code, // A-Z
    'a'.code..'z'.code  // a-z
)

fun tokenizeProto(@Language("protobuf") content: String): List<String> {
    val tokens = mutableListOf<String>()
    var offset = 0
    var start = -1
    var readUntil: Char? = null

    while (offset < content.length) {
        // single line comment
        if (content[offset] == '/' && content.length > offset + 1 && content[offset + 1] == '/') {
            while (++offset < content.length && content[offset] != '\n');
        } // multiline comment
        else if (content[offset] == '/' && content.length > offset + 1 && content[offset + 1] == '*') {
            offset++
            while (++offset < content.length && !(content[offset - 1] == '*' && content[offset] == '/'));
        } // string
        else if (readUntil != null) {
            if (content[offset] == readUntil) {
                tokens.add(content.substring(start, ++offset))
                readUntil = null
                start = -1

                continue
            } else {
                offset++

                continue
            }
        } else if (content[offset] == '"') {
            start = offset
            readUntil = '"'
        } else if (start == -1 && content[offset] in singleCharTokens) {
            tokens.add(content.substring(offset, ++offset))

            continue
        } else if (nameCharCodes.any { content[offset].code in it } || content[offset] == '_' || content[offset] == '.') {
            if (start == -1) {
                start = offset
            }
        } else if (start != -1) {
            tokens.add(content.substring(start, offset))

            start = -1

            continue
        }

        offset++
    }

    return tokens
}