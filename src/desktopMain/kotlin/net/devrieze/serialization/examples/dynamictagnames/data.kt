/*
 * Copyright (c) 2020.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package net.devrieze.serialization.examples.dynamictagnames

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import xml.Container
import xml.Element

/** Version that works with the released version 0.80.0 and 0.80.1 */
@Serializable(with = TestContainerSerializer::class)
data class TestContainer(val data: List<TestElement>) :Container<TestElement> {
    override val elements get() = data
}

@Serializable
data class TestElement(override val index: Int, val attr: Int, @XmlElement(true) val data: String): Element
