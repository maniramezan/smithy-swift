/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

import Foundation
import ClientRuntime
import XCTest
import class AwsCommonRuntimeKit.ByteBuffer

/**
 Includes Utility functions for Http Protocol Response Deserialization Tests
 */
open class HttpResponseTestBase: XCTestCase {
    /**
     Create `HttpResponse` from its components
     */
    public func buildHttpResponse(code: Int,
                                  path: String? = nil,
                                  headers: [String: String]? = nil,
                                  content: HttpBody = HttpBody.empty) -> HttpResponse? {
        
        var internalHeaders: Headers = Headers()
        if let headers = headers {
            internalHeaders = Headers(headers)
        }
        
        return HttpResponse(headers: internalHeaders,
                            body: content,
                            statusCode: HttpStatusCode(rawValue: code) ?? HttpStatusCode.badRequest)
        
    }
}
