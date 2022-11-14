package dev.jamesleach.dynamodb

import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class DefaultDynamoTableNameResolver : DynamoTableNameResolver {
    override fun getName(clazz: KClass<out Any>) = clazz.simpleName!!
}