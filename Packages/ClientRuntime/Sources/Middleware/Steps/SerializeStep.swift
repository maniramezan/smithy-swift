// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0.

/// Serializes the prepared input into a data structure that can be consumed
/// by the target transport's message, (e.g. REST-JSON serialization)
///
/// Converts Input Parameters into a Request, and returns the result or error.
///
/// Receives result or error from Build step.
public typealias SerializeStep<I,
                               O: HttpResponseBinding,
                               E: HttpResponseBinding> = MiddlewareStep<HttpContext,
                                                                        SerializeStepInput<I>,
                                                                        OperationOutput<O>,
                                                                        SdkError<E>>

public let SerializeStepId = "Serialize"

public struct SerializeStepHandler<OperationStackInput,
                                   OperationStackOutput: HttpResponseBinding,
                                   OperationStackError: HttpResponseBinding,
                                   H: Handler>: Handler where H.Context == HttpContext,
                                                              H.Input == SdkHttpRequestBuilder,
                                                              H.Output == OperationOutput<OperationStackOutput>,
                                                              H.MiddlewareError == SdkError<OperationStackError> {

    public typealias Input = SerializeStepInput<OperationStackInput>
    
    public typealias Output = OperationOutput<OperationStackOutput>
    
    public typealias MiddlewareError = SdkError<OperationStackError>
    
    let handler: H
    
    public init(handler: H) {
        self.handler = handler
    }
    
    public func handle(context: HttpContext, input: Input) -> Result<Output, MiddlewareError> {
        return handler.handle(context: context, input: input.builder)
    }
}

public struct SerializeStepInput<OperationStackInput> {
    public let operationInput: OperationStackInput
    public let builder: SdkHttpRequestBuilder = SdkHttpRequestBuilder()
}
