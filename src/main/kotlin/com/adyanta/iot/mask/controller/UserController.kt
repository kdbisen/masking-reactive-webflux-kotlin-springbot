package com.adyanta.iot.mask.controller


import com.adyanta.iot.mask.model.User
import com.adyanta.iot.mask.serializer.MaskingBeanSerializerModifier
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@Bean
fun objectMapper(): ObjectMapper {
    return jacksonObjectMapper()
}

@RestController
@RequestMapping("/users")
class UserController(private val objectMapper: ObjectMapper) {




    inline fun <reified T> maskResponseTyped(data: T, fieldsToMask: List<String>): Mono<T> {
        val maskedMapper = objectMapper.copy().apply {
            registerModule(SimpleModule().apply {
                setSerializerModifier(MaskingBeanSerializerModifier(fieldsToMask.toSet()))
            })
        }
        val jsonBytes = maskedMapper.writeValueAsBytes(data)
        val result = objectMapper.readValue(jsonBytes, T::class.java)
        return Mono.just(result)
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUser(@PathVariable id: String): Mono<User> {
        val address = User.Address("Pune", "MH", "411001")
        val user = User(id, null, "john.doe@example.com", "superSecretPassword", null, address)
        return maskResponseTyped(user, listOf("email", "password", "address.pincode"))
    }

    @GetMapping("/public/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUserPublic(@PathVariable id: String): Mono<User> {
        val address = User.Address("Mumbai", "MH", "400001")
        val user = User(id, "public_user", "pub.user@example.com", "hiddenPassword", "1234567890", address)
        return Mono.just(user)
    }

    @GetMapping("/null-test", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUserNullTest(): Mono<User> {
        val address = User.Address(null, null, null)
        val user = User("3", null, null, null, null, address)
        return maskResponseTyped(user, listOf("email", "password", "address.pincode"))
    }

    @PostMapping("/add", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createUser(): Mono<User> {
        val address = User.Address("Delhi", "DL", "110001")
        val user = User("test.user@example.com", "testpass", null, address)
        return maskResponseTyped(user, listOf("email", "password", "address.pincode"))
    }
}

