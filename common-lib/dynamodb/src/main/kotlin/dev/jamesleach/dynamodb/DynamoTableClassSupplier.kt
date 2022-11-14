package dev.jamesleach.dynamodb

import kotlin.reflect.KClass

interface DynamoTableClassSupplier {
    fun getTable(): KClass<out Any>
}