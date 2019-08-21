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

import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_BINARY;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_BYTE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_DATE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_DATE_TIME;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_DOUBLE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_FLOAT;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_INT_32;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_FORMAT_INT_64;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_ARRAY;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_BOOLEAN;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_INTEGER;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_NUMBER;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_OBJECT;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_TYPE_STRING;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.RESPONSE_SHAPE_TYPE_EXCEPTION;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.RESPONSE_SHAPE_TYPE_OUTPUT;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SERVICE_NAME;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_BLOB;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_BOOLEAN;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_BYTE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_DOUBLE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_FLOAT;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_INTEGER;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_LONG;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_STRING;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_TIMESTAMP;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.UNIQUE_ITEMS_AS_LIST;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs;
import software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpStatusCodes;
import software.amazon.openapi.smithy.exception.ConverterException;
import software.amazon.openapi.smithy.trait.mappers.HttpTraitMapperContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.utils.Pair;

/**
 * Generate Smithy Models from OpenAPI models.
 */
public class SmithyModelGenerator {
    private static final Logger LOGGER = Logger.getLogger(SmithyModelGenerator.class.getName());

    private static SmithyModelGenerator smithyModelGenerator;

    private ObjectNode config = Node.objectNode();

    private String namespace;

    private final List<OpenApi2SmithyTraitMapper> traitMappers = new ArrayList<>();

    private OpenApi2SmithyMapper mapper;

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    /**
     * Adds a mapper used to update schema builders.
     *
     * @param openApi2SmithyTraitMapper Mapper to add.
     * @return Returns the converter.
     */
    public SmithyModelGenerator addTraitMapper(OpenApi2SmithyTraitMapper openApi2SmithyTraitMapper) {
        traitMappers.add(openApi2SmithyTraitMapper);
        return this;
    }

    //Singleton class
    public static SmithyModelGenerator create() {
        if (smithyModelGenerator == null) {
            smithyModelGenerator = new SmithyModelGenerator();
        }
        return smithyModelGenerator;
    }

    /**
     * Gets the configuration object.
     *
     * @return Returns the config object.
     */
    public ObjectNode getConfig() {
        return config;
    }

    /**
     * Sets the configuration object.
     *
     * @param config Config to use.
     * @return Returns the converter.
     */
    public SmithyModelGenerator config(ObjectNode config) {
        this.config = config;
        return this;
    }

    public void setMapper(OpenApi2SmithyMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<Shape> create(ShapeId shapeId, Object schemaObj, String parentShapeId,
                                  ShapeIndex.Builder shapeIndexBuilder, Context context) {
        Shape shape = null;
        Schema schema;
        if (schemaObj instanceof  Schema) {
            schema = (Schema) schemaObj;
        } else {
            throw new ConverterException(String.format("Cannot create shape from schema of %s type.",
                    schemaObj.getClass()));
        }

        String ref  = schema.get$ref();
        if (ref != null && !ref.isEmpty()) {
            String targetShapeId =
                    getShapeIdFromRef(ref, namespace);
            return Optional.of(checkIfShapeExistsOrCreateNew(null, shapeId,
                    parentShapeId, targetShapeId, shapeIndexBuilder));
        }

        String type = schema.getType() == null ? "" : schema.getType();
        String format = schema.getFormat();
        switch (type) {
            case OPENAPI_TYPE_NUMBER:
                if ((format == null)
                        || format.equalsIgnoreCase(OPENAPI_FORMAT_DOUBLE)) {
                    shape = checkIfShapeExistsOrCreateNew(DoubleShape.builder(), shapeId,
                            parentShapeId, SMITHY_API_DOUBLE, shapeIndexBuilder);
                } else if (format.equalsIgnoreCase(OPENAPI_FORMAT_FLOAT)) {
                    shape = checkIfShapeExistsOrCreateNew(FloatShape.builder(), shapeId,
                            parentShapeId, SMITHY_API_FLOAT, shapeIndexBuilder);
                }
                break;
            case OPENAPI_TYPE_INTEGER:
                if (format == null
                        || format.equalsIgnoreCase(OPENAPI_FORMAT_INT_64)) {
                    shape = checkIfShapeExistsOrCreateNew(LongShape.builder(), shapeId,
                            parentShapeId, SMITHY_API_LONG, shapeIndexBuilder);
                } else if (format.equalsIgnoreCase(OPENAPI_FORMAT_INT_32)) {
                    shape = checkIfShapeExistsOrCreateNew(IntegerShape.builder(), shapeId,
                            parentShapeId, SMITHY_API_INTEGER, shapeIndexBuilder);
                }
                break;
            case OPENAPI_TYPE_STRING:
                if (format == null) {
                    shape = checkIfShapeExistsOrCreateNew(StringShape.builder(), shapeId,
                            parentShapeId, SMITHY_API_STRING, shapeIndexBuilder);
                } else {
                    if (format.equalsIgnoreCase(OPENAPI_FORMAT_BINARY)) {
                        shape = checkIfShapeExistsOrCreateNew(BlobShape.builder(), shapeId,
                                parentShapeId, SMITHY_API_BLOB, shapeIndexBuilder);
                    } else if (format.equalsIgnoreCase(OPENAPI_FORMAT_BYTE)) {
                        shape = checkIfShapeExistsOrCreateNew(ByteShape.builder(), shapeId,
                                parentShapeId, SMITHY_API_BYTE, shapeIndexBuilder);
                    } else if (format.equalsIgnoreCase(OPENAPI_FORMAT_DATE)
                            || format.equalsIgnoreCase(OPENAPI_FORMAT_DATE_TIME)) {
                        shape = checkIfShapeExistsOrCreateNew(TimestampShape.builder(), shapeId,
                                parentShapeId, SMITHY_API_TIMESTAMP, shapeIndexBuilder);
                    } else {
                        //other formats = { password, email, uuid, uri, hostname, ipv4, ipv6} -
                        //can use @pattern trait while mapping traits
                        shape = checkIfShapeExistsOrCreateNew(StringShape.builder(), shapeId,
                                parentShapeId, SMITHY_API_STRING, shapeIndexBuilder);
                    }
                }
                break;
            case OPENAPI_TYPE_BOOLEAN:
                shape = checkIfShapeExistsOrCreateNew(BooleanShape.builder(), shapeId,
                        parentShapeId, SMITHY_API_BOOLEAN, shapeIndexBuilder);
                break;
            case OPENAPI_TYPE_OBJECT:
            case "":
                //Case "": -> Same as object. Sometimes the type:object is not specified.
                //Can check if it has properties or not to decide.
                shape = createStructureShape(shapeId, schemaObj, shapeIndexBuilder, schema, parentShapeId, context);
                break;
            case OPENAPI_TYPE_ARRAY:
                shape = createAggregateShape(shapeId, schema, shapeIndexBuilder, parentShapeId, context);
                break;
            default:
                throw new ConverterException("The type value in the schema " + shapeId + " is invalid " + type);
        }
        return Optional.ofNullable(shape).map(shapeVal -> applyTraits(shapeVal, schemaObj, context))
                .map(shapeVal -> mapper.updateShape(shapeVal, schema, context));
    }

    private Shape applyTraits(Shape shape, Object obj, Context context) {
        for (OpenApi2SmithyTraitMapper mapper : traitMappers) {
            shape = mapper.applyTrait(shape, obj, context);
        }
        return shape;
    }


    private Shape checkIfShapeExistsOrCreateNew(AbstractShapeBuilder shapeBuilder,
                                                ShapeId shapeId, String parentShapeId, String target,
                                                ShapeIndex.Builder shapeIndexBuilder) {
        Optional<Shape> preludeShape =
                Prelude.resolveShapeId(Model.assembler().assemble().unwrap().getShapeIndex(),
                        "smithy.api", target.split("#")[1]);
        if (preludeShape.isPresent() && !parentShapeId.isEmpty()) {
            return  preludeShape.get();
        } else {
            return Prelude.resolveShapeId(shapeIndexBuilder.build(), namespace, target.split("#")[1])
                    .orElseGet(() -> (Shape) shapeBuilder.id(shapeId).build());
        }
    }

    @SuppressWarnings("unchecked")
    private Shape createStructureShape(ShapeId shapeId, Object obj, ShapeIndex.Builder shapeIndexBuilder,
                                       Schema schema, String parentShapeId, Context context) {
        Shape shape;
        ShapeId generatedShapeId = ShapeId.fromParts(shapeId.getNamespace(), parentShapeId.isEmpty()
                ? shapeId.getName() : parentShapeId + "_" + shapeId.getName());
        List<String> required = schema.getRequired();

        if (obj instanceof ComposedSchema) {
            //List<Schema> allOf = ((ComposedSchema) schema).getAllOf();
            List<Schema> oneOf = ((ComposedSchema) schema).getOneOf();
            List<Schema> anyOf = ((ComposedSchema) schema).getAnyOf();

            if (oneOf != null || anyOf != null) {
                throw new ConverterException("OneOf and AnyOf properties are not supported in Smithy");
            } else {
                //TODO : handle allOff as Composition
                throw new ConverterException("AllOf property is currently not supported in Smithuy");
            }
        } else {
            StructureShape.Builder shapeBuilder = StructureShape.builder().id(generatedShapeId);
            Map<String, Schema> propertiesMap = schema.getProperties();
            propertiesMap.forEach((propertyName, propertySchema) -> shapeBuilder
                    .addMember(getMemberShape(shapeIndexBuilder, generatedShapeId,
                    required, ShapeId.fromParts(namespace, propertyName), propertySchema, propertyName, context)));
            shape = shapeBuilder.build();
            if (!parentShapeId.isEmpty()) {
                shapeIndexBuilder.addShape(shape);
            }
        }
        return shape;
    }

    private MemberShape getMemberShape(ShapeIndex.Builder shapeIndexBuilder, ShapeId generatedShapeId,
                                       List<String> required, ShapeId shapeId, Schema schema, String memberId,
                                       Context context) {
        MemberShape.Builder memberShapeBuilder = MemberShape.builder();
        if (schema.get$ref() != null) {
            memberShapeBuilder.target(getShapeIdFromRef(schema.get$ref(), namespace))
                    .id(generatedShapeId.withMember(memberId));
        } else {
            Optional<Shape> inlineShape =
                    create(shapeId, schema, generatedShapeId.getName(), shapeIndexBuilder, context);
            inlineShape.ifPresent(inlineShapeVal -> {
                memberShapeBuilder.target(inlineShapeVal.getId())
                        .id(generatedShapeId.withMember(memberId));
                //While adding the generated Inline Shape as a member add all the traits.
                inlineShapeVal.getAllTraits().values().forEach(memberShapeBuilder::addTrait);
            });
        }
        //If the property exists in Schema's required list annotate with @required trait
        Optional.ofNullable(required).ifPresent(requiredList -> {
            if (required.contains(shapeId.getName())) {
                memberShapeBuilder.addTrait(new RequiredTrait());
            }
        });
        return memberShapeBuilder.build();
    }

    private Shape createAggregateShape(ShapeId shapeId, Schema schema, ShapeIndex.Builder shapeIndexBuilder,
                                       String parentShapeId, Context context) {
        Shape shape;
        ShapeId generatedShapeId = ShapeId.fromParts(shapeId.getNamespace(), parentShapeId.isEmpty()
                ? shapeId.getName() : parentShapeId + "_" + shapeId.getName());

        boolean uniqueItems = schema.getUniqueItems() == null ? false : schema.getUniqueItems();
        boolean uniqueItemsAsList =
                config.getBooleanMemberOrDefault(UNIQUE_ITEMS_AS_LIST, false);
        CollectionShape.Builder builder;
        if (!uniqueItems) {
            builder = ListShape.builder().id(generatedShapeId);
        } else {
            if (uniqueItemsAsList) {
                builder = ListShape.builder().id(generatedShapeId);
            } else {
                builder = SetShape.builder().id(generatedShapeId);
            }
        }

        //TODO: Handle arbitary array -> items: {}
        /*if (((ArraySchema) schema).getItems() == null) {
            throw new ConverterException("The items property for Array type cannot be null " + shapeId);
        }*/

        builder.addMember(getMemberShape(shapeIndexBuilder, generatedShapeId, ((ArraySchema) schema).getRequired(),
                ShapeId.fromParts(namespace, "Member"), ((ArraySchema) schema).getItems(), "member",
                context));
        shape = (Shape) builder.build();
        if (!parentShapeId.isEmpty()) {
            shapeIndexBuilder.addShape(shape);
        }

        shape = applyTraits(shape, schema, context);

        return shape;
    }

    private String getShapeIdFromRef(String ref, String namespace) {
        if (ref.startsWith("#")) {
            String[] pathArr = ref.split("/");
            return namespace + "#" + pathArr[pathArr.length - 1];
        } else {
            //TODO: Add logic to handle schema definitions in external files
            //for now throw exception
            throw new ConverterException("External definitions are not handled currently");
        }
    }

    public Shape createOperationShape(String path, Operation operation, String operationType,
                                      ShapeIndex.Builder indexBuilder, Context context) {
        Shape operationShape;
        OperationShape.Builder operationShapeBuilder = OperationShape.builder()
                .id(ShapeId.fromParts(namespace, operation.getOperationId()));

        //Create Operation Input from Parameters and RequestBody
        Shape operationInputShape
                = createOperationInputShape(operation, indexBuilder, operationShapeBuilder, operation.getParameters(),
                operation.getRequestBody(), context);

        //Add Input to Operation and ShapeIndex
        if (operationInputShape != null) {
            operationShapeBuilder.input(operationInputShape);
            indexBuilder.addShape(operationInputShape);
        }

        //Create Operation Output and Exceptions from Responses
        List<Pair<String, Optional<Shape>>> responseShapesPairList = createOutputAndExceptionShapes(indexBuilder,
                operationShapeBuilder, operation.getResponses(), context);

        //Add Output and Exceptions to Operation and ShapeIndex
        responseShapesPairList.stream().filter(Objects::nonNull).forEach(responseShapePair -> {
            if (responseShapePair.left.equals(RESPONSE_SHAPE_TYPE_OUTPUT)) {
                responseShapePair.right.ifPresent(shape -> {
                    operationShapeBuilder.output(shape);
                    indexBuilder.addShape(shape);
                });
            } else {
                responseShapePair.right.ifPresent(shape -> {
                    operationShapeBuilder.addError(shape);
                    indexBuilder.addShape(shape);
                });
            }
        });

        //Apply traits mappers
        operationShape = applyTraits(operationShapeBuilder.build(), operation, context);
        //TODO : Better way to achieve this.
        operationShape = applyTraits(operationShape, new HttpTraitMapperContext(path, operation,
                operationType, indexBuilder.build()), context);
        //Apply OpenApi2Smithy mappers
        operationShape = mapper.updateOperationShape(operationShape, operation, context);
        return operationShape;
    }

    private List<Pair<String, Optional<Shape>>> createOutputAndExceptionShapes(ShapeIndex.Builder indexBuilder,
                                                                     OperationShape.Builder operationShapeBuilder,
                                                                     ApiResponses apiResponses, Context context) {
        List<Pair<String, Optional<Shape>>> responseShapesPairList = null;
        if (apiResponses != null) {
            responseShapesPairList = apiResponses.entrySet().stream()
                    .map(response -> processApiResponses(response, operationShapeBuilder.getId().toString(),
                            indexBuilder, context))
                    .collect(Collectors.toList());
        }
        return responseShapesPairList;
    }

    private Shape createOperationInputShape(Operation operation, ShapeIndex.Builder indexBuilder,
                                            OperationShape.Builder operationShapeBuilder, List<Parameter> parameterList,
                                            RequestBody requestBody, Context context) {
        Shape operationInputShape = null;
        if (parameterList == null && requestBody == null) {
            //Operation has no Input
            LOGGER.warning(String.format("%s Operation has no Input", operation.getOperationId()));
        } else {
            if (parameterList == null) {
                //We do not need to enclose the requestBody in a Input shape if params is null
                operationInputShape = processRequestBody(requestBody, operation.getOperationId() + "Input",
                        indexBuilder, context);
            } else {
                //Create an enclosing Input shape if params is present
                //Add request body requestBody as member if present.
                operationInputShape =
                        getInputFromParametersAndRequest(operation, indexBuilder, operationShapeBuilder, parameterList,
                                requestBody, context);
            }
        }
        return operationInputShape;
    }

    private Shape getInputFromParametersAndRequest(Operation operation, ShapeIndex.Builder indexBuilder,
                                                   OperationShape.Builder operationShapeBuilder,
                                                   List<Parameter> parameterList, RequestBody requestBody,
                                                   Context context) {
        StructureShape.Builder operationInputShapeBuilder = StructureShape.builder()
                .id(operationShapeBuilder.getId() + "Input");
        parameterList.stream()
                .map(parameter ->
                        processParameter(parameter, indexBuilder, context, operationInputShapeBuilder.getId()))
                .collect(Collectors.toList())
                .forEach(operationInputShapeBuilder::addMember);

        if (requestBody != null) {
            Shape requestBodyShape = processRequestBody(requestBody, operation.getOperationId() + "Input",
                    indexBuilder, context);
            operationInputShapeBuilder
                    .addMember(MemberShape.builder()
                            .id(operationInputShapeBuilder.getId() + "$" + requestBodyShape.getId().getName())
                            .target(requestBodyShape.getId()).build());
        }
        return operationInputShapeBuilder.build();
    }

    private Shape processRequestBody(RequestBody requestBody, String shapeId, ShapeIndex.Builder indexBuilder,
                                     Context context) {

        if (!checkIfContentHasMultipleSchemaWithDifferentTypes(requestBody.getContent())) {
            throw new ConverterException("The Operation can only support single input type. ");
        }
        return getShapeFromContent(requestBody.getContent(), shapeId, indexBuilder, context).orElse(null);
    }

    private boolean checkIfContentHasMultipleSchemaWithDifferentTypes(Content content) {
        return  Optional.of(content).map(content1 -> content1.values().stream()
        .filter(Objects::nonNull).map(MediaType::getSchema)
        .filter(Objects::nonNull).map(Schema::get$ref)
        .collect(Collectors.toSet())).filter((s) -> s.size() == 1).isPresent();
    }

    private MemberShape processParameter(Parameter parameter, ShapeIndex.Builder indexBuilder, Context context,
                                         ShapeId operationInputShapeId) {
        Shape parameterShape = create(ShapeId.fromParts(namespace, parameter.getName()), parameter.getSchema(),
                "", indexBuilder, context).orElse(null);
        indexBuilder.addShape(parameterShape);

        MemberShape memberShape = MemberShape.builder()
                .id(operationInputShapeId + "$" + parameterShape.getId().getName())
                .target(parameterShape.getId())
                .build();
        memberShape = (MemberShape) applyTraits(memberShape, parameter, context);

        return memberShape;
    }

    private Pair<String, Optional<Shape>> processApiResponses(Map.Entry<String, ApiResponse> apiResponseEntry,
                                                              String shapeId, ShapeIndex.Builder indexBuilder,
                                                              Context context) {
        Pair<String, Optional<Shape>> typeShapePair = null;
        String statusCode = apiResponseEntry.getKey();

        if (!statusCode.equals("default")
                && Integer.parseInt(statusCode) >= 200 && Integer.parseInt(statusCode) < 300) {
            if (Integer.parseInt(statusCode) != 201) {
                typeShapePair =
                        processApiResponse(apiResponseEntry, shapeId,
                                indexBuilder, RESPONSE_SHAPE_TYPE_OUTPUT, context);
            } else {
                if (apiResponseEntry.getValue().getContent() != null) {
                    LOGGER.warning("Http Status Response 201 should not have content");
                }
            }
        } else {
            typeShapePair =
                    processApiResponse(apiResponseEntry, shapeId, indexBuilder, RESPONSE_SHAPE_TYPE_EXCEPTION, context);
        }
        return typeShapePair;
    }

    private Pair<String, Optional<Shape>> processApiResponse(Map.Entry<String, ApiResponse> responseEntry,
                                                             String operationShapeId, ShapeIndex.Builder indexBuilder,
                                                             String shapeType, Context context) {
        Optional<Shape> operationOutputOrErrorShape;
        if (shapeType.equals(RESPONSE_SHAPE_TYPE_OUTPUT)) {
            StructureShape.Builder operationOutputShapeBuilder =
                    StructureShape.builder().id(operationShapeId + shapeType);
            Optional.ofNullable(responseEntry.getValue().getHeaders()).ifPresent(stringHeaderMap -> stringHeaderMap
                    .entrySet().forEach(stringHeaderEntry -> {
                        MemberShape headerShape =
                                processHeader(stringHeaderEntry, indexBuilder, context,
                                        operationOutputShapeBuilder.getId());
                        operationOutputShapeBuilder.addMember(headerShape);
                    }));
            Optional.ofNullable(responseEntry.getValue().getContent()).ifPresent(content -> {
                Optional<Shape> responseShape = getShapeFromContent(content,
                        operationOutputShapeBuilder.getId().getName() + "_Member", indexBuilder, context);
                responseShape.ifPresent(responseShapeVal -> operationOutputShapeBuilder.addMember(MemberShape.builder()
                        .id(operationOutputShapeBuilder.getId() + "$" + responseShapeVal.getId()
                                .getName().toLowerCase())
                        //.id(operationOutputShapeBuilder.getId() + "$member")
                        .target(responseShapeVal.getId())
                        .build()));
            });
            operationOutputOrErrorShape = Optional.of(operationOutputShapeBuilder.build());
        } else {
            Optional<Shape> errorShape;
            if (responseEntry.getValue().getContent() == null && responseEntry.getValue().getHeaders() == null) {
                errorShape = getShapeFromEmptyResponse(responseEntry.getKey(), shapeType);
            } else {
                String statusCode = responseEntry.getKey();
                String exceptionShapeName = statusCode.equals("default")
                        ? "UnexpectedError" : HttpStatusCodes.get(Integer.parseInt(responseEntry.getKey())).toString();
                errorShape = getShapeFromContent(responseEntry.getValue().getContent(),
                        exceptionShapeName, indexBuilder, context);
            }
            operationOutputOrErrorShape = errorShape;
        }
        operationOutputOrErrorShape =
                operationOutputOrErrorShape.map(shape -> applyTraits(shape, responseEntry, context));
        return new Pair<>(shapeType, operationOutputOrErrorShape);
    }

    private Optional<Shape> getShapeFromEmptyResponse(String statusCode, String shapeType) {
        Optional<Shape> operationOutputOrErrorShape = Optional.empty();
        if (shapeType.equals(OpenApi2SmithyConstants.EXCEPTION)) {
            String exceptionShapeName = HttpStatusCodes
                    .get(Integer.parseInt(statusCode))
                    .toString();
            if (statusCode.equals("default")) {
                exceptionShapeName = "UnexpectedError"; //Can be named as DefaultResponse
                //TODO: Handle if default is not Error or Exception
            }
            StructureShape.Builder shapeBuilder =
                    StructureShape.builder()
                            .id(ShapeId.fromParts(namespace, exceptionShapeName + shapeType));
            shapeBuilder.addMember(
                    MemberShape.builder()
                            .id(shapeBuilder.getId() + "$" + "message")
                            .target(SMITHY_API_STRING)
                            .build());
            operationOutputOrErrorShape = Optional.of(shapeBuilder.build());
        } else {
            //TODO : Handle when success response is empty
        }
        return operationOutputOrErrorShape;
    }

    private Optional<Shape> getShapeFromContent(Content content,
                                                String shapeId,
                                                ShapeIndex.Builder indexBuilder,
                                                Context context) {
        Schema responseSchema = content.entrySet().stream().findFirst().get().getValue().getSchema();
        Optional<Shape> responseShape =
                create(ShapeId.fromParts(namespace, shapeId), responseSchema, "", indexBuilder, context);
        responseShape.ifPresent(indexBuilder::addShape);
        return responseShape;
    }

    private MemberShape processHeader(Map.Entry<String, Header> headerEntry, ShapeIndex.Builder indexBuilder,
                                Context context, ShapeId outputShapeId) {
        Shape headerShape = null;
        String headerShapeId = headerEntry.getKey().contains("-")
                ? headerEntry.getKey().replace("-", "") : headerEntry.getKey();
        if (headerEntry.getValue().get$ref() != null) {
            //TODO Create Header from $ref
        } else {
            headerShape = create(ShapeId.fromParts(namespace, headerShapeId), headerEntry.getValue().getSchema(),
                    "", indexBuilder, context).orElse(null);
        }
        MemberShape memberShape = Optional.ofNullable(headerShape).map(shape -> MemberShape.builder()
                .id(outputShapeId + "$" + headerShapeId)
                .target(shape.getId())
                .build()).orElse(null);
        indexBuilder.addShape(headerShape);
        memberShape = (MemberShape) applyTraits(memberShape, headerEntry, context);
        return memberShape;
    }

    public Shape createServiceShape(OpenAPI openApi, ShapeIndex.Builder indexBuilder, Map<String,
            Map<HttpActionVerbs, String>> pathOperationsMap, Context context) {
        String serviceName = config.getStringMemberOrDefault(SERVICE_NAME, "TestService");
        ServiceShape.Builder serviceShapeBuilder = ServiceShape.builder().id(namespace + "#" + serviceName);
        serviceShapeBuilder.version(openApi.getInfo().getVersion());
        createResourceShapesFromPaths(pathOperationsMap,
                indexBuilder).forEach(serviceShapeBuilder::addResource);
        Shape serviceShape = serviceShapeBuilder.build();

        serviceShape = applyTraits(serviceShape, openApi, context);
        serviceShape = mapper.updateServiceShape(serviceShape, openApi, context);
        return serviceShape;
    }

    private List<Shape> createResourceShapesFromPaths(Map<String, Map<HttpActionVerbs, String>> pathOperationsMap,
                                                      ShapeIndex.Builder indexBuilder) {
        List<Shape> resources = new ArrayList<>();
        Map<String, Map<HttpActionVerbs, String>> sortedMap = pathOperationsMap.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(path -> path.getKey().length()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        sortedMap.forEach((path, operationMap) -> {
            String[] pathArr = path.split("/");
            resources.add(createResourceShape(new LinkedList<>(Arrays.stream(pathArr)
                    .filter(pathItem -> !pathItem.isEmpty())
                    .collect(Collectors.toList())), operationMap, indexBuilder));
        });
        return resources;
    }

    private Shape createResourceShape(LinkedList<String> pathTermsList, Map<HttpActionVerbs, String> operationMap,
                                      ShapeIndex.Builder indexBuilder) {
        if (pathTermsList.size() == 1 || (pathTermsList.size() == 2 && pathTermsList.get(1).contains("{"))) {
            LinkedList<String> currentlist = new LinkedList<>(pathTermsList);
            AbstractShapeBuilder resourceShapeBuilder = getResourceWithId(pathTermsList, indexBuilder);
            addOperationsToResource((ResourceShape.Builder) resourceShapeBuilder, operationMap, currentlist);
            indexBuilder.addShape((Shape) resourceShapeBuilder.build());
            return (Shape) resourceShapeBuilder.build();
        } else {
            AbstractShapeBuilder resourceShapeBuilder = getResourceWithId(pathTermsList, indexBuilder);
            Shape resourceShape = createResourceShape(pathTermsList, operationMap, indexBuilder);
            Optional.ofNullable(resourceShape).ifPresent(resource -> {
                indexBuilder.addShape(resource);
                ((ResourceShape.Builder) resourceShapeBuilder).addResource(resource);
                AbstractShapeBuilder builder = Shape.shapeToBuilder(resourceShape);
                ((ResourceShape) resourceShapeBuilder.build()).getIdentifiers()
                        .forEach(((ResourceShape.Builder) builder)::addIdentifier);
                indexBuilder.addShape(((ResourceShape.Builder) builder).build());
            });
            indexBuilder.addShape((Shape) resourceShapeBuilder.build());
            return (Shape) resourceShapeBuilder.build();
        }
    }

    private AbstractShapeBuilder getResourceWithId(LinkedList<String> pathTermsList, ShapeIndex.Builder indexBuilder) {
        ShapeId shapeId = ShapeId.fromParts(namespace, "Resource" + pathTermsList.removeFirst());
        AbstractShapeBuilder resourceShapeBuilder = getResourceBuilder(indexBuilder, shapeId);

        if (!pathTermsList.isEmpty() && pathTermsList.peek().contains("{")) {
            String resourceIdentifier = pathTermsList.removeFirst();
            resourceIdentifier = resourceIdentifier.replaceAll("[{}]", "");
            Shape resourceIdentifierShape =
                    StringShape.builder().id(ShapeId.fromParts(namespace, resourceIdentifier)).build();
            indexBuilder.addShape(resourceIdentifierShape);
            ((ResourceShape.Builder) resourceShapeBuilder)
                    .addIdentifier(resourceIdentifier, resourceIdentifierShape.getId());
        }
        return resourceShapeBuilder;
    }

    @SuppressWarnings("unchecked")
    private AbstractShapeBuilder getResourceBuilder(ShapeIndex.Builder indexBuilder, ShapeId resourceShapeId) {
        return Shape.shapeToBuilder(indexBuilder.build().getShape(resourceShapeId)
                .orElseGet(() -> ResourceShape.builder().id(resourceShapeId).build()));
    }

    private void addOperationsToResource(ResourceShape.Builder resourceShapeBuilder,
                                         Map<HttpActionVerbs, String> operationMap, LinkedList<String> pathTermsList) {
        Optional.ofNullable(operationMap.get(HttpActionVerbs.GET)).ifPresent(operationId -> {
            if (pathTermsList.size() == 2) {
                resourceShapeBuilder.read(ShapeId.fromParts(namespace, operationId));
            } else {
                resourceShapeBuilder.addOperation(ShapeId.fromParts(namespace, operationId));
            }
        });
        Optional.ofNullable(operationMap.get(HttpActionVerbs.POST)).ifPresent(operationId -> resourceShapeBuilder
                .create(ShapeId.fromParts(namespace, operationId)));
        Optional.ofNullable(operationMap.get(HttpActionVerbs.PUT)).ifPresent(operationId -> resourceShapeBuilder
                .update(ShapeId.fromParts(namespace, operationId)));
        Optional.ofNullable(operationMap.get(HttpActionVerbs.DELETE)).ifPresent(operationId -> resourceShapeBuilder
                .delete(ShapeId.fromParts(namespace, operationId)));
        Optional.ofNullable(operationMap.get(HttpActionVerbs.TRACE)).ifPresent(operationId -> resourceShapeBuilder
                .addOperation(ShapeId.fromParts(namespace, operationId)));
        Optional.ofNullable(operationMap.get(HttpActionVerbs.OPTIONS)).ifPresent(operationId -> resourceShapeBuilder
                .addOperation(ShapeId.fromParts(namespace, operationId)));
        Optional.ofNullable(operationMap.get(HttpActionVerbs.PATCH)).ifPresent(operationId -> resourceShapeBuilder
                .addOperation(ShapeId.fromParts(namespace, operationId)));
        Optional.ofNullable(operationMap.get(HttpActionVerbs.HEAD)).ifPresent(operationId -> resourceShapeBuilder
                .addOperation(ShapeId.fromParts(namespace, operationId)));
    }

}
