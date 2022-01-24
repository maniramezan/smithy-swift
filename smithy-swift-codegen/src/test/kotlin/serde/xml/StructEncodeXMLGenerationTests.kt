/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package serde.xml

import MockHttpRestXMLProtocolGenerator
import TestContext
import defaultSettings
import getFileContents
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.junit.jupiter.api.Test

class StructEncodeXMLGenerationTests {
    @Test
    fun `simpleScalar serialization`() {
        val context = setupTests("Isolated/Restxml/xml-scalar.smithy", "aws.protocoltests.restxml#RestXml")
        val contents = getFileContents(context.manifest, "/RestXml/models/SimpleScalarPropertiesInput+Encodable.swift")
        val expectedContents =
            """
            extension SimpleScalarPropertiesInput: Swift.Encodable, Runtime.Reflection {
                enum CodingKeys: Swift.String, Swift.CodingKey {
                    case byteValue
                    case doubleValue = "DoubleDribble"
                    case falseBooleanValue
                    case floatValue
                    case integerValue
                    case longValue
                    case `protocol` = "protocol"
                    case shortValue
                    case stringValue
                    case trueBooleanValue
                }
            
                public func encode(to encoder: Swift.Encoder) throws {
                    var container = encoder.container(keyedBy: Runtime.Key.self)
                    if let byteValue = byteValue {
                        try container.encode(byteValue, forKey: Runtime.Key("byteValue"))
                    }
                    if let doubleValue = doubleValue {
                        try container.encode(Swift.String(doubleValue), forKey: Runtime.Key("DoubleDribble"))
                    }
                    if let falseBooleanValue = falseBooleanValue {
                        try container.encode(falseBooleanValue, forKey: Runtime.Key("falseBooleanValue"))
                    }
                    if let floatValue = floatValue {
                        try container.encode(Swift.String(floatValue), forKey: Runtime.Key("floatValue"))
                    }
                    if let integerValue = integerValue {
                        try container.encode(integerValue, forKey: Runtime.Key("integerValue"))
                    }
                    if let longValue = longValue {
                        try container.encode(longValue, forKey: Runtime.Key("longValue"))
                    }
                    if let `protocol` = `protocol` {
                        try container.encode(`protocol`, forKey: Runtime.Key("protocol"))
                    }
                    if let shortValue = shortValue {
                        try container.encode(shortValue, forKey: Runtime.Key("shortValue"))
                    }
                    if let stringValue = stringValue {
                        try container.encode(stringValue, forKey: Runtime.Key("stringValue"))
                    }
                    if let trueBooleanValue = trueBooleanValue {
                        try container.encode(trueBooleanValue, forKey: Runtime.Key("trueBooleanValue"))
                    }
                }
            }
            """.trimIndent()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `008 structure xmlName`() {
        val context = setupTests("Isolated/Restxml/xml-lists-structure.smithy", "aws.protocoltests.restxml#RestXml")
        val contents = getFileContents(context.manifest, "/RestXml/models/StructureListMember+Codable.swift")
        val expectedContents =
            """
            extension RestXmlProtocolClientTypes.StructureListMember: Swift.Codable, Runtime.Reflection {
                enum CodingKeys: Swift.String, Swift.CodingKey {
                    case a = "value"
                    case b = "other"
                }
            
                public func encode(to encoder: Swift.Encoder) throws {
                    var container = encoder.container(keyedBy: Runtime.Key.self)
                    if let a = a {
                        try container.encode(a, forKey: Runtime.Key("value"))
                    }
                    if let b = b {
                        try container.encode(b, forKey: Runtime.Key("other"))
                    }
                }
            
                public init (from decoder: Swift.Decoder) throws {
                    let containerValues = try decoder.container(keyedBy: CodingKeys.self)
                    let aDecoded = try containerValues.decodeIfPresent(Swift.String.self, forKey: .a)
                    a = aDecoded
                    let bDecoded = try containerValues.decodeIfPresent(Swift.String.self, forKey: .b)
                    b = bDecoded
                }
            }
            """.trimIndent()

        contents.shouldContainOnlyOnce(expectedContents)
    }

    private fun setupTests(smithyFile: String, serviceShapeId: String): TestContext {
        val context = TestContext.initContextFrom(smithyFile, serviceShapeId, MockHttpRestXMLProtocolGenerator()) { model ->
            model.defaultSettings(serviceShapeId, "RestXml", "2019-12-16", "Rest Xml Protocol")
        }
        context.generator.generateCodableConformanceForNestedTypes(context.generationCtx)
        context.generator.generateSerializers(context.generationCtx)
        context.generationCtx.delegator.flushWriters()
        return context
    }
}
