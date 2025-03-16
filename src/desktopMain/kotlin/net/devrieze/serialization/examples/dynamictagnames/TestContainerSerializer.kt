package net.devrieze.serialization.examples.dynamictagnames

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import xml.CommonContainerSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer

class TestContainerSerializer() : CommonContainerSerializer<TestContainer, TestElement>() {
    override fun constructContainer(list: List<TestElement>): TestContainer {
        return TestContainer(list)
    }
    override val elementSerializer = serializer<TestElement>()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TestContainer") {
        element("data", ListSerializer(elementSerializer).descriptor)
    }
    override val prefix: String = "Test_"
}