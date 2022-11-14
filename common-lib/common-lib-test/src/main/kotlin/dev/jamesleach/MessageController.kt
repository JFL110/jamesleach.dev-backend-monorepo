package dev.jamesleach

import org.springframework.web.bind.annotation.*

@RestController
class MessageController(
    private val messageDao: MessageDao
)  {

    @PostMapping("/message")
    fun create(@RequestBody message: MessageDto) {
        messageDao.create(message.id, message.text)
    }

    @GetMapping("/message/{id}")
    fun get(@PathVariable("id") id: String): MessageDto? {
        val text = messageDao.get(id)
        return if (text == null) null else MessageDto(id, text)
    }
}