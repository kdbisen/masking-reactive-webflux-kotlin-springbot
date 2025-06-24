package com.adyanta.iot.mask.serializer

import com.adyanta.iot.mask.model.KYCAppendix
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ObjectMapper

class PathAwareMaskingSerializer(
    private val fieldsToMask: List<String>,
    private val currentPath: String = ""
) : StdSerializer<KYCAppendix>(KYCAppendix::class.java) {

    override fun serialize(value: KYCAppendix?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        val mapper = ObjectMapper() // optionally inject
        val jsonNode = mapper.valueToTree<JsonNode>(value)
        val maskedNode = applyMasking(jsonNode, currentPath)
        gen.writeTree(maskedNode)
    }

    private fun applyMasking(node: JsonNode, path: String): JsonNode {
        if (!node.isObject && !node.isArray) return node

        val mapper = ObjectMapper()

        if (node.isArray) {
            val updatedArray = node.map { element ->
                if (element.isObject || element.isArray) {
                    applyMasking(element, path)
                } else element
            }
            return mapper.valueToTree(updatedArray)
        }

        val copy = (node as ObjectNode).deepCopy()
        val fields = node.fieldNames()

        while (fields.hasNext()) {
            val field = fields.next()
            val child = node.get(field)
            val fullPath = if (path.isEmpty()) field else "$path.$field"
            println("Writing $fullPath")
            if (shouldMask(fullPath)) {
                copy.put(field, "****")
            } else if (child.isObject || child.isArray) {
                copy.set<JsonNode>(field, applyMasking(child, fullPath))
            } else {
                copy.set<JsonNode>(field, child)
            }
        }

        return copy
    }

    private fun shouldMask(fullPath: String): Boolean {
        return fieldsToMask.any { maskPath ->
            maskPath == fullPath || (maskPath.startsWith("*.") && fullPath.endsWith(maskPath.removePrefix("*.").trim()))
        }
    }
}
