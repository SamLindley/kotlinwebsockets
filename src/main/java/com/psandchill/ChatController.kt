package com.psandchill

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*
import spark.Spark.*
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {
    port(9000)
    staticFileLocation("/public")
    webSocket("/chat", ChatWSHandler::class.java)
    init()
}

class User(val id: Long, val name: String)
class Message(val messageType: String, val data: Any)

@WebSocket
class ChatWSHandler{

    val users = HashMap<Session, User>()
    var userIDs = AtomicLong(0)

    @OnWebSocketConnect
    fun connected(session: Session) = println("session connected")

    @OnWebSocketMessage
    fun message(session: Session, message: String) {
        val json = ObjectMapper().readTree(message)
        when (json.get("type").asText()) {
            "join" -> {
                val user = User(userIDs.getAndIncrement(), json.get("data").asText())
                users.put(session, user)
                emit(session, Message("users", users.values))
                broadcastToOthers(session, Message("join", user))
            }
            "say" -> {
                broadcast(Message("say", json.get("data").asText()))
            }
        }
        println("json msg $message")
    }



    @OnWebSocketClose
    fun disconnect(session: Session, code: Int, reason: String?) {
        val user = users.remove(session)
        if (user != null) broadcast(Message("left", user))
    }

    private fun emit(session: Session, message: Message) = session.remote.sendString(jacksonObjectMapper().writeValueAsString(message))
}