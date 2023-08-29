package io.github.alexispurslane.bloc.data

// All credit for this work goes to Anton Zhilin on GitHub: https://gist.github.com/Anton3/348e639b6d46c3598f3311b9feca8578l
// Usually I don't copy-paste code ever, but... Jesus, I don't want to deal with whatever the hell this is.

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TreeTraversingParser
import com.fasterxml.jackson.databind.util.NameTransformer

class SinglePolyUnwrappedDeserializer<T : Any> : JsonDeserializer<T>(),
    ContextualDeserializer {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T =
        error("Not implemented")

    override fun createContextual(
        ctxt: DeserializationContext,
        property: BeanProperty?
    ): JsonDeserializer<T> =
        SinglePolyUnwrappedDeserializerImpl(ctxt)
}

private class SinglePolyUnwrappedDeserializerImpl<T : Any>(ctxt: DeserializationContext) :
    StdDeserializer<T>(null as JavaType?) {

    private val type: JavaType = ctxt.contextualType
    private val beanDeserializer: JsonDeserializer<T>
    private val ownPropertyNames: Set<String>

    private val unwrappedType: JavaType
    private val unwrappedPropertyName: String
    private val nameTransformer: NameTransformer

    init {
        val description: BeanDescription = ctxt.config.introspect(type)

        var tempUnwrappedAnnotation: JsonUnwrapped? = null

        val unwrappedProperties = description.findProperties().filter { prop ->
            listOfNotNull(
                prop.constructorParameter,
                prop.mutator,
                prop.field
            ).any { member ->
                val unwrappedAnnotation: JsonUnwrapped? =
                    member.getAnnotation(JsonUnwrapped::class.java)
                if (unwrappedAnnotation != null) {
                    tempUnwrappedAnnotation = unwrappedAnnotation
                    member.allAnnotations.add(notUnwrappedAnnotation)
                }
                unwrappedAnnotation != null
            }
        }

        val unwrappedProperty = when (unwrappedProperties.size) {
            0 -> error("@JsonUnwrapped properties not found in ${type.typeName}")
            1 -> unwrappedProperties.single()
            else -> error("Multiple @JsonUnwrapped properties found in ${type.typeName}")
        }

        nameTransformer = tempUnwrappedAnnotation!!.run {
            NameTransformer.simpleTransformer(
                prefix,
                suffix
            )
        }

        unwrappedPropertyName = unwrappedProperty.name

        ownPropertyNames =
            description.findProperties().mapTo(mutableSetOf()) { it.name }
        ownPropertyNames.remove(unwrappedPropertyName)
        ownPropertyNames.removeAll(description.ignoredPropertyNames)

        unwrappedType = unwrappedProperty.primaryType

        val rawBeanDeserializer =
            ctxt.factory.createBeanDeserializer(ctxt, type, description)
        (rawBeanDeserializer as? ResolvableDeserializer)?.resolve(ctxt)
        @Suppress("UNCHECKED_CAST")
        beanDeserializer = rawBeanDeserializer as JsonDeserializer<T>
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T {
        val node = p.readValueAsTree<ObjectNode>()

        val ownNode = ObjectNode(ctxt.nodeFactory)
        val unwrappedNode = ObjectNode(ctxt.nodeFactory)

        node.fields().forEach { (key, value) ->
            val transformed: String? = nameTransformer.reverse(key)

            if (transformed != null && key !in ownPropertyNames) {
                unwrappedNode.replace(transformed, value)
            } else {
                ownNode.replace(key, value)
            }
        }

        ownNode.replace(unwrappedPropertyName, unwrappedNode)

        val syntheticParser = TreeTraversingParser(ownNode)
        syntheticParser.nextToken()
        return beanDeserializer.deserialize(syntheticParser, ctxt)
    }

    private class NotUnwrapped(
        @Suppress("unused")
        @field:JsonUnwrapped(enabled = false)
        @JvmField
        val dummy: Nothing
    )

    companion object {
        val notUnwrappedAnnotation: JsonUnwrapped =
            NotUnwrapped::class.java.getField("dummy")
                .getAnnotation(JsonUnwrapped::class.java)
    }
}