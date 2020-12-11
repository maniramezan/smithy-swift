/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.swift.codegen

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import java.util.function.Consumer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.StructureShape

class StructureGeneratorTests : TestsBase() {
    @Test
    fun `it renders non-error structures`() {

        val struct: StructureShape = createStructureWithoutErrorTrait()
        val model: Model = createModelWithStructureShape(struct)
        val provider: SymbolProvider = SwiftCodegenPlugin.createSymbolProvider(model, "test")
        val writer = SwiftWriter("MockPackage")
        val generator = StructureGenerator(model, provider, writer, struct)
        generator.render()

        val contents = writer.toString()

        contents.shouldContain(SwiftWriter.staticHeader)
        val expectedGeneratedStructure =
            """
                /// This *is* documentation about the shape.
                public struct MyStruct: Equatable {
                    public let bar: Int
                    /// This *is* documentation about the member.
                    public let baz: Int?
                    public let foo: String?

                    public init (
                        bar: Int = 0,
                        baz: Int? = nil,
                        foo: String? = nil
                    )
                    {
                        self.bar = bar
                        self.baz = baz
                        self.foo = foo
                    }
                }
            """.trimIndent()
        contents.shouldContain(expectedGeneratedStructure)
    }

    @Test
    fun `it renders recursive nested shapes`() {
        val structs = createStructureContainingNestedRecursiveShape()
        val model = createModelFromSmithy("recursive-shape-test.smithy")
        val provider = SwiftCodegenPlugin.createSymbolProvider(model, "smithy.example")
        val writer = SwiftWriter("MockPackage")

        for (struct in structs) {
            val generator = StructureGenerator(model, provider, writer, struct)
            generator.render()
        }
        val contents = writer.toString()
        val expected =
            """
public struct RecursiveShapesInputOutputNested1: Equatable {
    public let foo: String?
    public let nested: Box<RecursiveShapesInputOutputNested2>?

    public init (
        foo: String? = nil,
        nested: Box<RecursiveShapesInputOutputNested2>? = nil
    )
    {
        self.foo = foo
        self.nested = nested
    }
}

public struct RecursiveShapesInputOutputNested2: Equatable {
    public let bar: String?
    public let recursiveMember: RecursiveShapesInputOutputNested1?

    public init (
        bar: String? = nil,
        recursiveMember: RecursiveShapesInputOutputNested1? = nil
    )
    {
        self.bar = bar
        self.recursiveMember = recursiveMember
    }
}

/// This *is* documentation about the shape.
public struct RecursiveShapesInputOutput: Equatable {
    public let nested: RecursiveShapesInputOutputNested1?

    public init (
        nested: RecursiveShapesInputOutputNested1? = nil
    )
    {
        self.nested = nested
    }
}
                """.trimIndent()
        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun `it renders recursive nested shapes in lists`() {
        val structs = createStructureContainingNestedRecursiveShapeList()
        val model = createModelFromSmithy("recursive-shape-test.smithy")
        val provider = SwiftCodegenPlugin.createSymbolProvider(model, "smithy.example")
        val writer = SwiftWriter("MockPackage")

        for (struct in structs) {
            val generator = StructureGenerator(model, provider, writer, struct)
            generator.render()
        }
        val contents = writer.toString()
        val expected =
            """
public struct RecursiveShapesInputOutputNestedList1: Equatable {
    public let foo: String?
    public let recursiveList: [RecursiveShapesInputOutputNested2]?

    public init (
        foo: String? = nil,
        recursiveList: [RecursiveShapesInputOutputNested2]? = nil
    )
    {
        self.foo = foo
        self.recursiveList = recursiveList
    }
}

public struct RecursiveShapesInputOutputNested2: Equatable {
    public let bar: String?
    public let recursiveMember: RecursiveShapesInputOutputNested1?

    public init (
        bar: String? = nil,
        recursiveMember: RecursiveShapesInputOutputNested1? = nil
    )
    {
        self.bar = bar
        self.recursiveMember = recursiveMember
    }
}

/// This *is* documentation about the shape.
public struct RecursiveShapesInputOutputLists: Equatable {
    public let nested: RecursiveShapesInputOutputNested1?

    public init (
        nested: RecursiveShapesInputOutputNested1? = nil
    )
    {
        self.nested = nested
    }
}
                """.trimIndent()
        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun `it renders error structures along with proper import statement`() {

        val struct: StructureShape = createStructureWithOptionalErrorMessage()
        val model: Model = createModelWithStructureShape(struct)
        val provider: SymbolProvider = SwiftCodegenPlugin.createSymbolProvider(model, "test")
        val writer = SwiftWriter("MockPackage")
        val generator = StructureGenerator(model, provider, writer, struct)
        generator.render()

        val contents = writer.toString()

        contents.shouldContain(SwiftWriter.staticHeader)
        val expectedGeneratedStructure =
            """
                import ClientRuntime

                /// This *is* documentation about the shape.
                public struct MyError: ServiceError {
                    public var _headers: Headers?
                    public var _statusCode: HttpStatusCode?
                    public var _message: String?
                    public var _requestID: String?
                    public var _retryable: Bool? = true
                    public var _type: ErrorType = .client
                    /// This *is* documentation about the member.
                    public var baz: Int?
                    public var message: String?

                    public init (
                        baz: Int? = nil,
                        message: String? = nil
                    )
                    {
                        self.baz = baz
                        self.message = message
                    }
                }
            """.trimIndent()

        contents.shouldContain(expectedGeneratedStructure)
    }

    private fun createModelWithStructureShape(struct: StructureShape): Model {

        val assembler = Model.assembler().addShape(struct)
        struct.allMembers.values.forEach(Consumer { shape: MemberShape? ->
            assembler.addShape(
                shape
            )
        })

        return assembler.assemble().unwrap()
    }

    @Test
    fun `check for sparse and dense datatypes in list`() {
        /*  Also the integration test for model evolution: Struct JsonListsInputOutput is generating 2
            separate structs for input and output with members given in smithy model, without creating
            additional structs in the model
         */
        val model = createModelFromSmithy("sparse-trait-test.smithy")
        val manifest = MockManifest()
        val context = buildMockPluginContext(model, manifest)
        SwiftCodegenPlugin().execute(context)

        val jsonListsInput = manifest
            .getFileString("example/models/JsonListsInput.swift").get()
        Assertions.assertNotNull(jsonListsInput)
        jsonListsInput.shouldContain("public struct JsonListsInput: Equatable {\n" +
                "    public let booleanList: [Bool]?\n" +
                "    public let integerList: [Int]?\n" +
                "    public let nestedStringList: [[String]?]?\n" +
                "    public let sparseStringList: [String?]?\n" +
                "    public let stringList: [String]?\n" +
                "    public let stringSet: Set<String>?\n" +
                "    public let timestampList: [Date]?")

        val jsonListsOutput = manifest
                .getFileString("example/models/JsonListsOutput.swift").get()
        Assertions.assertNotNull(jsonListsOutput)
        jsonListsOutput.shouldContain("public struct JsonListsOutput: Equatable {\n" +
                "    public let booleanList: [Bool]?\n" +
                "    public let integerList: [Int]?\n" +
                "    public let nestedStringList: [[String]?]?\n" +
                "    public let sparseStringList: [String?]?\n" +
                "    public let stringList: [String]?\n" +
                "    public let stringSet: Set<String>?\n" +
                "    public let timestampList: [Date]?")
    }

    @Test
    fun `check for sparse and dense datatypes in maps`() {
        /*  Also the integration test for model evolution: Struct JsonListsInputOutput is generating 2
            separate structs for input and output with members given in smithy model, without creating
            additional structs in the model
         */
        val model = createModelFromSmithy("sparse-trait-test.smithy")
        val manifest = MockManifest()
        val context = buildMockPluginContext(model, manifest)
        SwiftCodegenPlugin().execute(context)

        val jsonMapsInput = manifest
            .getFileString("example/models/JsonMapsInput.swift").get()
        Assertions.assertNotNull(jsonMapsInput)
        val expectedJsonMapsInput =
            """
                public struct JsonMapsInput: Equatable {
                    public let denseBooleanMap: [String:Bool]?
                    public let denseNumberMap: [String:Int]?
                    public let denseStringMap: [String:String]?
                    public let denseStructMap: [String:GreetingStruct]?
                    public let sparseBooleanMap: [String:Bool?]?
                    public let sparseNumberMap: [String:Int?]?
                    public let sparseStringMap: [String:String?]?
                    public let sparseStructMap: [String:GreetingStruct?]?

                    public init (
                        denseBooleanMap: [String:Bool]? = nil,
                        denseNumberMap: [String:Int]? = nil,
                        denseStringMap: [String:String]? = nil,
                        denseStructMap: [String:GreetingStruct]? = nil,
                        sparseBooleanMap: [String:Bool?]? = nil,
                        sparseNumberMap: [String:Int?]? = nil,
                        sparseStringMap: [String:String?]? = nil,
                        sparseStructMap: [String:GreetingStruct?]? = nil
                    )
                    {
                        self.denseBooleanMap = denseBooleanMap
                        self.denseNumberMap = denseNumberMap
                        self.denseStringMap = denseStringMap
                        self.denseStructMap = denseStructMap
                        self.sparseBooleanMap = sparseBooleanMap
                        self.sparseNumberMap = sparseNumberMap
                        self.sparseStringMap = sparseStringMap
                        self.sparseStructMap = sparseStructMap
                    }
                }
            """.trimIndent()
        jsonMapsInput.shouldContain(expectedJsonMapsInput)

        val jsonMapsOutput = manifest
                .getFileString("example/models/JsonMapsOutput.swift").get()
        Assertions.assertNotNull(jsonMapsOutput)
        val expectedJsonMapsOutput =
                """
                public struct JsonMapsOutput: Equatable {
                    public let denseBooleanMap: [String:Bool]?
                    public let denseNumberMap: [String:Int]?
                    public let denseStringMap: [String:String]?
                    public let denseStructMap: [String:GreetingStruct]?
                    public let sparseBooleanMap: [String:Bool?]?
                    public let sparseNumberMap: [String:Int?]?
                    public let sparseStringMap: [String:String?]?
                    public let sparseStructMap: [String:GreetingStruct?]?

                    public init (
                        denseBooleanMap: [String:Bool]? = nil,
                        denseNumberMap: [String:Int]? = nil,
                        denseStringMap: [String:String]? = nil,
                        denseStructMap: [String:GreetingStruct]? = nil,
                        sparseBooleanMap: [String:Bool?]? = nil,
                        sparseNumberMap: [String:Int?]? = nil,
                        sparseStringMap: [String:String?]? = nil,
                        sparseStructMap: [String:GreetingStruct?]? = nil
                    )
                    {
                        self.denseBooleanMap = denseBooleanMap
                        self.denseNumberMap = denseNumberMap
                        self.denseStringMap = denseStringMap
                        self.denseStructMap = denseStructMap
                        self.sparseBooleanMap = sparseBooleanMap
                        self.sparseNumberMap = sparseNumberMap
                        self.sparseStringMap = sparseStringMap
                        self.sparseStructMap = sparseStructMap
                    }
                }
            """.trimIndent()

        jsonMapsOutput.shouldContain(expectedJsonMapsOutput)
    }
}
