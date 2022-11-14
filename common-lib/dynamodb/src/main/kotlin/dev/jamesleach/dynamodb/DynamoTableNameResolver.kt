package dev.jamesleach.dynamodb

import kotlin.reflect.KClass

interface DynamoTableNameResolver {
    fun getName(clazz: KClass<out Any>): String
}