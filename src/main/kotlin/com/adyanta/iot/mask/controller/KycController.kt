package com.adyanta.iot.mask.controller


import com.adyanta.iot.mask.model.CommonHeaderRequestType
import com.adyanta.iot.mask.model.KYCAppendix
import com.adyanta.iot.mask.model.TTSKYCPortalRequestType
import com.adyanta.iot.mask.serializer.MaskingBeanSerializerModifier
import com.adyanta.iot.mask.serializer.PathAwareMaskingSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/kyc")
class KYCController(private val objectMapper: ObjectMapper) {

    private inline fun <reified T : Any> maskMono(mono: Mono<T>, fieldsToMask: List<String>): Mono<T> {
        val maskedMapper = objectMapper.copy().apply {
            registerModule(SimpleModule().apply {
                addSerializer(KYCAppendix::class.java, PathAwareMaskingSerializer(fieldsToMask))
            })
        }
        return mono.map { item ->
            val json = maskedMapper.writeValueAsBytes(item)
            objectMapper.readValue(json, T::class.java)
        }
    }

    @GetMapping("/appendix", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAppendix(): Mono<KYCAppendix> {
        val appendix = KYCAppendix(
            TTSKYCPortalRequestType(
                CommonHeaderRequestType(
                    soeid = "ABC12345"
                )
            )
        )
        return maskMono(Mono.just(appendix), listOf("checklist.kycHeaderDetails.soeid"))
    }
}