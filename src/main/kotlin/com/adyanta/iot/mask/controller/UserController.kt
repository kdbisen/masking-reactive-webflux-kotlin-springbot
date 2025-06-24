package com.adyanta.iot.mask.controller


import com.adyanta.iot.mask.annotation.Mask
import com.adyanta.iot.mask.model.User
import com.adyanta.iot.mask.serializer.MaskingBeanSerializerModifier
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/users")
class UserController {

    @Mask(["email", "password", "address.pincode"])
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): Mono<User> {
        return Mono.just(
            User(
                id = id,
                email = "john.doe@example.com",
                password = "secret",
                address = User.Address("Pune", "MH", "411001")
            )
        )
    }

    @GetMapping("/public/{id}")
    fun getUserPublic(@PathVariable id: String): Mono<User> {
        return Mono.just(
            User(
                id = id,
                username = "public_user",
                email = "pub.user@example.com",
                password = "hiddenPassword",
                mobile = "1234567890",
                address = User.Address("Mumbai", "MH", "400001")
            )
        )
    }

    @Mask(["email", "password", "address.pincode"])
    @GetMapping("/null-test")
    fun getUserNullTest(): Mono<User> {
        return Mono.just(
            User(
                id = "3",
                address = User.Address(null, null, null)
            )
        )
    }

    @PostMapping("/add")
    fun createUser(): Mono<User> {
        return Mono.just(
            User(
                email = "test.user@example.com",
                password = "testpass",
                address = User.Address("Delhi", "DL", "110001")
            )
        )
    }

    fun maskResponse(data: Any, fieldsToMask: List<String>, objectMapper: ObjectMapper): Mono<Any> {
        val maskedMapper = objectMapper.copy().apply {
            registerModule(SimpleModule().apply {
                setSerializerModifier(MaskingBeanSerializerModifier(fieldsToMask.toSet()))
            })
        }
        val jsonBytes = maskedMapper.writeValueAsBytes(data)
        val result = objectMapper.readValue(jsonBytes, Any::class.java)
        return Mono.just(result)
    }

}
