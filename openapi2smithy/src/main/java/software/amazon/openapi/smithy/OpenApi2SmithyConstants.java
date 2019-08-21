/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.openapi.smithy;

import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.utils.ListUtils;

public final class OpenApi2SmithyConstants {

    /** The supported version of OpenAPI. */
    public static final String VERSION = "3.0.2";

    /** The location of the OpenAPI folder. */
    public static final String OPENAPI_FILE_LOCATION = "openapi.file.location";

    /** The filename of the OpenAPI service. */
    public static final String OPENAPI_SERVICE_FILE = "openapi.filename";

    /** The Service Name for Smithy. */
    public static final String SERVICE_NAME = "service.name";

    /** The namespace for the smithy model. */
    public static final String NAMESPACE = "namespace";

    /** Model a array with uniqueItems as a List or Set. */
    public static final String UNIQUE_ITEMS_AS_LIST = "uniqueItems.list";

    //PUBLIC PRELUDE SHAPES CONSTANTS
    public static final String SMITHY_API_FLOAT = "smithy.api#Float";
    public static final String SMITHY_API_DOUBLE = "smithy.api#Double";
    public static final String SMITHY_API_LONG = "smithy.api#Long";
    public static final String SMITHY_API_INTEGER = "smithy.api#Integer";
    public static final String SMITHY_API_STRING = "smithy.api#String";
    public static final String SMITHY_API_BLOB = "smithy.api#Blob";
    public static final String SMITHY_API_BYTE = "smithy.api#Byte";
    public static final String SMITHY_API_TIMESTAMP = "smithy.api#Timestamp";
    public static final String SMITHY_API_BOOLEAN = "smithy.api#Boolean";

    //OpenAPI Data Formats
    public static final String OPENAPI_FORMAT_FLOAT = "float";
    public static final String OPENAPI_FORMAT_DOUBLE = "double";
    public static final String OPENAPI_FORMAT_INT_64 = "int64";
    public static final String OPENAPI_FORMAT_INT_32 = "int32";
    public static final String OPENAPI_FORMAT_BYTE = "byte";
    public static final String OPENAPI_FORMAT_DATE = "date";
    public static final String OPENAPI_FORMAT_BINARY = "binary";

    //OpenAPI Data Types
    public static final String OPENAPI_TYPE_NUMBER = "number";
    public static final String OPENAPI_TYPE_INTEGER = "integer";
    public static final String OPENAPI_TYPE_BOOLEAN = "boolean";
    public static final String OPENAPI_TYPE_STRING = "string";
    public static final String OPENAPI_TYPE_OBJECT = "object";
    public static final String OPENAPI_TYPE_ARRAY = "array";
    public static final String OPENAPI_FORMAT_DATE_TIME = "date-time";

    public static final String OPENAPI_DEFAULT_NAMESPACE = "ns.foo";

    public static final List<SecurityScheme.Type> SUPPORTED_OPENAPI_SECURITY_SCHEMES =
            Collections.unmodifiableList(ListUtils.of(SecurityScheme.Type.APIKEY, SecurityScheme.Type.HTTP));

    public static final Map<String, String> OPENAPI_SMITHY_AUTH_SCHEMES_MAP =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("http_basic", "http-basic");
                put("http_digest", "http-digest");
                put("http_bearer", "http-bearer");
                put("apiKey", "http-x-api-key");
                put("none", "none");
            }
            });

    public static final String RESPONSE_SHAPE_TYPE_OUTPUT = "Output";
    public static final String SUPPORTED_SECURITY_SCHEMES = "SupportedSecuritySchemes";
    public static final String API_LEVEL_SECURITY_SCHEMES = "ApiLevelSecuritySchemes";
    public static final String EXCEPTION = "Exception";
    public static final String RESPONSE_SHAPE_TYPE_EXCEPTION = "Exception";

    private OpenApi2SmithyConstants() {}

    public enum HttpActionVerbs {
        GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
    }

    public enum HttpStatusCodes {
        Ok(200), Created(201), Accepted(202), NoContent(204),
        BadRequest(400), Unauthorized(401), Forbidden(403), NotFound(404), MethodNotAllowed(405),
        NotAcceptable(406), RequestTimeout(408), Conflict(409), UnsupportedMediaType(415),
        InternalServerError(500), NotImplemented(501), BadGateway(502), ServiceUnavailable(503),
        GatewayTimeout(504), HttpVersionNotSupported(505);

        //Lookup table
        private static final Map<Integer, HttpStatusCodes> LOOK_UP_TABLE = new HashMap<>();

        private int statusCode;

        HttpStatusCodes(int statusCode) {
            this.statusCode = statusCode;
        }

        private int getStatusCode() {
            return this.statusCode;
        }

        //Populate the lookUp table on loading time
        static {
            for (HttpStatusCodes code : HttpStatusCodes.values()) {
                LOOK_UP_TABLE.put(code.getStatusCode(), code);
            }
        }

        //This method can be used for reverse lookUp purpose
        public static HttpStatusCodes get(int code) {
            return LOOK_UP_TABLE.get(code);
        }

    }

}
