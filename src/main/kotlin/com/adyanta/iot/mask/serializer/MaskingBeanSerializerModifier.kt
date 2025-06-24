package com.adyanta.iot.mask.serializer

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier

class MaskingBeanSerializerModifier(private val fieldsToMask: Set<String>) : BeanSerializerModifier() {
    override fun changeProperties(
        config: com.fasterxml.jackson.databind.SerializationConfig,
        beanDesc: BeanDescription,
        beanProperties: MutableList<BeanPropertyWriter>
    ): MutableList<BeanPropertyWriter> {
        for (writer in beanProperties) {
            val fullPath = "${beanDesc.beanClass.simpleName.lowercase()}.${writer.name}"
            if (fieldsToMask.contains(writer.name) || fieldsToMask.contains(fullPath)) {
                writer.assignSerializer(MaskingSerializer())
            }
        }
        return beanProperties
    }
}