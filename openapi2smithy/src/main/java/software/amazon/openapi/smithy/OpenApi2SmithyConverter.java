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

import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.DELETE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.GET;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.HEAD;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.OPTIONS;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.PATCH;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.POST;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.PUT;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.TRACE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.NAMESPACE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.OPENAPI_DEFAULT_NAMESPACE;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;

/**
 * Converts a Open API 3.0 model to a Smithy Model.
 */
public final class OpenApi2SmithyConverter {

    private static final Logger LOGGER = Logger.getLogger(OpenApi2SmithyConverter.class.getName());

    private Map<String, Node> settings = new HashMap<>();
    private final ClassLoader classLoader = OpenApi2SmithyConverter.class.getClassLoader();
    private SmithyModelGenerator smithyModelGenerator;
    private final List<OpenApi2SmithyMapper> mappers = new ArrayList<>();

    private OpenApi2SmithyConverter() {}

    public static OpenApi2SmithyConverter create() {
        return new OpenApi2SmithyConverter();
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @param <T> value type to set.
     * @return Returns the OpenApi2SmithyConverter.
     */
    public <T extends ToNode> OpenApi2SmithyConverter putSetting(String setting, T value) {
        settings.put(setting, value.toNode());
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @return Returns the OpenApi2SmithyConverter.
     */
    public OpenApi2SmithyConverter putSetting(String setting, String value) {
        settings.put(setting, Node.from(value));
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @return Returns the OpenApi2SmithyConverter.
     */
    public OpenApi2SmithyConverter putSetting(String setting, Number value) {
        settings.put(setting, Node.from(value));
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @return Returns the OpenApi2SmithyConverter.
     */
    public OpenApi2SmithyConverter putSetting(String setting, boolean value) {
        settings.put(setting, Node.from(value));
        return this;
    }

    private SmithyModelGenerator getSmithyModelGenerator() {
        if (smithyModelGenerator == null) {
            smithyModelGenerator = SmithyModelGenerator.create();
        }
        return smithyModelGenerator;
    }

    public OpenApi2SmithyConverter setSmithyModelGenerator(SmithyModelGenerator smithyModelGenerator) {
        this.smithyModelGenerator = smithyModelGenerator;
        return this;
    }

    /**
     * Converts the given OpenAPI Model to a Smithy Model.
     * @param openApi OpenAPI model to convert.
     * @return Returns the converted Smithy model.
     */
    public Model convert(OpenAPI openApi) {
        return convertWithEnvironment(createConversionEnvironment(openApi));
    }

    /**
     * Create the conversion environment. Create the context, collect  extension and mappers.
     * @param openApi The OpenAPI model to convert.
     * @return The conversion environment.
     */
    private ConversionEnvironment createConversionEnvironment(OpenAPI openApi) {

        //Create a config object, add settings to config and set the config on the generator
        ObjectNode.Builder configBuilder = ObjectNode.objectNodeBuilder();
        settings.forEach(configBuilder::withMember);
        ObjectNode config = configBuilder.build();
        getSmithyModelGenerator().config(config);
        smithyModelGenerator.setNamespace(config.getStringMemberOrDefault(NAMESPACE, OPENAPI_DEFAULT_NAMESPACE));

        // Discover OpenApi extensions.
        List<OpenApi2SmithyExtension> extensions = new ArrayList<>();
        ServiceLoader.load(OpenApi2SmithyExtension.class, classLoader).forEach(extensions::add);

        // Add OpenApi2Smithy trait mappers from found extensions.
        extensions.forEach(extension -> extension.getOpenApi2SmithyTraitMappers()
                .forEach(smithyModelGenerator::addTraitMapper));

        //Create the context
        Context context = new Context(openApi, smithyModelGenerator);

        return new ConversionEnvironment(context, extensions, mappers);
    }

    private Model convertWithEnvironment(ConversionEnvironment environment) {
        Context context = environment.context;
        OpenApi2SmithyMapper mapper = environment.mapper;

        smithyModelGenerator.setMapper(mapper);

        ShapeIndex.Builder indexBuilder = ShapeIndex.builder();
        mapper.before(indexBuilder, context);

        buildShapeIndex(environment.context, indexBuilder);
        Model model = Model.builder()
                .shapeIndex(indexBuilder.build())
                .build();

        model = mapper.after(model, context);

        Model validatedModel = Model.assembler().addModel(model).assemble().unwrap();
        return validatedModel;
    }

    private void buildShapeIndex(Context context, ShapeIndex.Builder indexBuilder) {
        //TODO: Iterate over the OpenAPI model and create shapes from Paths and Components objects
        getShapesFromComponents(context, indexBuilder);
        Map<String, Map<HttpActionVerbs, String>> pathOperationsMap = new HashMap<>();
        getShapesFromPaths(context, indexBuilder, pathOperationsMap);
        indexBuilder.addShape(smithyModelGenerator.createServiceShape(context.getOpenApi(), indexBuilder,
                pathOperationsMap, context));
    }


    private void getShapesFromComponents(Context context, ShapeIndex.Builder indexBuilder) {
        indexBuilder.addShapes(context.getOpenApi().getComponents().getSchemas().entrySet().stream()
                .map(stringSchemaEntry -> smithyModelGenerator
                        .create(ShapeId.fromParts(smithyModelGenerator.getNamespace(), stringSchemaEntry.getKey()),
                                stringSchemaEntry.getValue(), "", indexBuilder, context))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
    }

    private void getShapesFromPaths(Context context, ShapeIndex.Builder indexBuilder,
                                    Map<String, Map<HttpActionVerbs, String>> pathOperationsMap) {
        //TODO: Iterate over each PathItem and create Operation and Resource Shapes
        indexBuilder.addShapes(context.getOpenApi().getPaths().entrySet().stream()
                .flatMap(pathItem ->
                        getOperationShapesFromPathItem(pathItem.getKey(), pathItem.getValue(), indexBuilder,
                                pathOperationsMap, context).stream())
                .collect(Collectors.toList()));
    }

    private List<Shape> getOperationShapesFromPathItem(String path, PathItem pathItem,
                                                       ShapeIndex.Builder shapeIndexBuilder,
                                                       Map<String, Map<HttpActionVerbs, String>> pathOperationsMap,
                                                       Context context) {
        List<Shape> operationShapes = new ArrayList<>();
        Map<HttpActionVerbs, String> operationsMap = new HashMap<>();

        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getGet(), GET, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getPost(), POST, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getPut(), PUT, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getDelete(), DELETE, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getHead(), HEAD, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getOptions(), OPTIONS, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getPatch(), PATCH, context);
        getOperationShape(path, shapeIndexBuilder, operationShapes, operationsMap,
                pathItem.getTrace(), TRACE, context);

        pathOperationsMap.put(path, operationsMap);
        return operationShapes;
    }

    private void getOperationShape(String path, ShapeIndex.Builder shapeIndexBuilder, List<Shape> operationShapes,
                                   Map<HttpActionVerbs, String> operationsMap,
                                   Operation operation, HttpActionVerbs httpActionVerb, Context context) {
        Optional.ofNullable(operation).ifPresent(operationVal -> {
            operationShapes.add(smithyModelGenerator
                .createOperationShape(path, operation, httpActionVerb.toString(), shapeIndexBuilder,
                        context));
            operationsMap.put(httpActionVerb, operationVal.getOperationId());
        });
    }

    private static final class ConversionEnvironment {
        private final Context context;
        private final List<OpenApi2SmithyExtension> extensions;
        private final OpenApi2SmithyMapper mapper;


        ConversionEnvironment(Context context, List<OpenApi2SmithyExtension> extensions,
                              List<OpenApi2SmithyMapper> mappers) {
            this.context = context;
            this.extensions = extensions;
            this.mapper = createMapper(mappers);
        }

        private OpenApi2SmithyMapper createMapper(List<OpenApi2SmithyMapper> mappers) {
            return OpenApi2SmithyMapper.compose(Stream.concat(
                    extensions.stream().flatMap(extension -> extension.getOpenApiMappers().stream()),
                    mappers.stream()
            ).collect(Collectors.toList()));
        }
    }


}
