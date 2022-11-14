package dev.jamesleach.location

import dev.jamesleach.dynamodb.DynamoTableNameResolver
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
@Primary
internal class LocationDynamoTableNameResolver : DynamoTableNameResolver {
    override fun getName(clazz: KClass<out Any>) =
        "jamesleachdev_${clazz.simpleName?.replace("Entity", "")}"
}