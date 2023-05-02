package com.github.tenebras.minipb.parsing

import org.intellij.lang.annotations.Language
import java.lang.Integer.max

private val singleCharTokens = setOf('=', ';', '(', ')', '[', ']', '{', '}', ',', '/', '<', '>', ':')
private val nameCharCodes = listOf(
    '0'.code..'9'.code, // 0-9
    'A'.code..'Z'.code, // A-Z
    'a'.code..'z'.code  // a-z
)

fun tokenizeProto(@Language("protobuf") content: String, includeComments: Boolean = false): List<String> {
    val tokens = mutableListOf<String>()
    var offset = 0
    var start = -1
    var readUntil: Char? = null

    while (offset < content.length) {
        // single line comment
        if (content[offset] == '/' && content.length > offset + 1 && content[offset + 1] == '/') {
            val commentStart = offset
            while (++offset < content.length && content[offset] != '\n');

            if (includeComments) {
                val isLineStart = commentStart == 0 ||
                    (
                        content[commentStart - 1] == '\n' ||
                        content.substring(max(content.lastIndexOf('\n', commentStart), 0), commentStart).isBlank()
                    )

                tokens.add("…")
                tokens.add((if (isLineStart) "" else "…") + content.substring(commentStart, offset))
            }
        } // multiline comment
        else if (content[offset] == '/' && content.length > offset + 1 && content[offset + 1] == '*') {
            val commentStart = offset++
            while (++offset < content.length && !(content[offset - 1] == '*' && content[offset] == '/'));

            if (includeComments) {
                val isLineStart = commentStart == 0 ||
                    (
                        content[commentStart - 1] == '\n' ||
                            content.substring(max(content.lastIndexOf('\n', commentStart), 0), commentStart).isBlank()
                        )

                val isLeft = isLineStart && content.substring(offset, max(content.indexOf('\n', commentStart), offset))
                    .isNotBlank()
                val isRight = !isLineStart && !isLeft

                tokens.add("…")
                tokens.add(
                    (if (isRight) "…" else "") + content.substring(commentStart, offset + 1) + (if (isLeft) "…" else "")
                )
            }
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