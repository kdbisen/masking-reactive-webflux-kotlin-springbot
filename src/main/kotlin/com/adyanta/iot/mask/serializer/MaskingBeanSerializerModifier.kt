package com.adyanta.iot.mask.serializer

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier


class MaskingBeanSerializerModifier(
    private val fieldsToMask: Set<String>,
    private val deep: Boolean = false
) : BeanSerializerModifier() {

    override fun updateBuilder(
        config: SerializationConfig,
        beanDesc: BeanDescription,
        builder: BeanSerializerBuilder
    ): BeanSerializerBuilder {
        val prefix = beanDesc.beanClass.simpleName.replaceFirstChar { it.lowercase() }
        val originalWriters = builder.properties

        val newWriters = originalWriters.map { writer ->
            val fullPath = buildFullPath(prefix, writer.name)
            if (shouldMask(writer.name, fullPath)) {
                println("[MASKING] Matched field: '${writer.name}' at path: '$fullPath'")
                writer.assignSerializer(MaskingSerializer())
            }
            writer
        }.toMutableList()

        builder.setProperties(newWriters)
        return builder
    }

    private fun buildFullPath(prefix: String, fieldName: String): String {
        return if (deep) "$prefix.$fieldName" else fieldName
    }

    private fun shouldMask(name: String, fullPath: String): Boolean {
        return fieldsToMask.contains(name) || fieldsToMask.contains(fullPath)
    }
}
