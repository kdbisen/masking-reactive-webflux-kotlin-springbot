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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MaskingHandlerResultHandler(
    private val objectMapper: ObjectMapper
) : HandlerResultHandler {

    override fun supports(result: HandlerResult): Boolean = true

    override fun handleResult(exchange: ServerWebExchange, result: HandlerResult): Mono<Void> {
        val handler = exchange.getAttribute<Any>("org.springframework.web.reactive.HandlerMapping.bestMatchingHandler")

        // Extract fields from @Mask annotation
        val fieldsToMask: List<String> = if (handler is HandlerMethod) {
            handler.getMethodAnnotation(Mask::class.java)?.value?.toList() ?: return defaultWrite(result, exchange)
        } else return defaultWrite(result, exchange)

        // Skip if no fields specified
        if (fieldsToMask.isEmpty()) return defaultWrite(result, exchange)

        // Wrap the return value (Mono or Flux or raw object)
        val returnValue = result.returnValue
        val mapper = objectMapper.copy().apply {
            registerModule(SimpleModule().apply {
                setSerializerModifier(MaskingBeanSerializerModifier(fieldsToMask.toSet()))
            })
        }

        @Suppress("UNCHECKED_CAST")
        return when (returnValue) {
            is Mono<*> -> {
                (returnValue as Mono<Any>).flatMap { writeMaskedBody(exchange, it, mapper) }
            }
            is Flux<*> -> {
                (returnValue as Flux<Any>).collectList().flatMap { writeMaskedBody(exchange, it, mapper) }
            }
            null -> exchange.response.setComplete()
            else -> {
                writeMaskedBody(exchange, returnValue, mapper)
            }
        }
    }

    private fun writeMaskedBody(exchange: ServerWebExchange, body: Any, mapper: ObjectMapper): Mono<Void> {
        val response: ServerHttpResponse = exchange.response
        response.headers.contentType = MediaType.APPLICATION_JSON
        val buffer = response.bufferFactory().wrap(mapper.writeValueAsBytes(body))
        return response.writeWith(Mono.just(buffer))
    }

    private fun defaultWrite(result: HandlerResult, exchange: ServerWebExchange): Mono<Void> {
        // Let WebFlux's default handlers process this normally
        return Mono.error(IllegalStateException("Should be handled by a standard result handler"))
    }
}
