package com.adyanta.iot.mask.handler


import com.adyanta.iot.mask.annotation.Mask
import com.adyanta.iot.mask.serializer.MaskingBeanSerializerModifier
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.HandlerResultHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MaskingHandlerResultHandler(private val objectMapper: ObjectMapper) : HandlerResultHandler {

    override fun supports(result: HandlerResult): Boolean = true

    override fun handleResult(exchange: ServerWebExchange, result: HandlerResult): Mono<Void> {
        val handler = exchange.getAttribute<Any>("org.springframework.web.reactive.HandlerMapping.bestMatchingHandler")
        val fieldsToMask = if (handler is HandlerMethod) {
            handler.getMethodAnnotation(Mask::class.java)?.value?.toList() ?: emptyList()
        } else emptyList()

        @Suppress("UNCHECKED_CAST")
        val bodyMono: Mono<Any> = when (val returnValue = result.returnValue) {
            is Mono<*> -> returnValue as Mono<Any>
            null -> Mono.empty()
            else -> Mono.just(returnValue)
        }

        val mapper = if (fieldsToMask.isNotEmpty()) {
            objectMapper.copy().apply {
                registerModule(SimpleModule().apply {
                    setSerializerModifier(MaskingBeanSerializerModifier(fieldsToMask.toSet()))
                })
            }
        } else objectMapper

        return bodyMono.flatMap { body ->
            val json = mapper.writeValueAsBytes(body)
            val response: ServerHttpResponse = exchange.response
            response.headers.contentType = MediaType.APPLICATION_JSON
            response.writeWith(Mono.just(response.bufferFactory().wrap(json)))
        }
    }

}
