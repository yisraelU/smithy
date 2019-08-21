package software.amazon.openapi.smithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.GET;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.HttpActionVerbs.POST;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_BLOB;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_BOOLEAN;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_BYTE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_DOUBLE;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_FLOAT;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_INTEGER;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_LONG;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_STRING;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_TIMESTAMP;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.openapi.smithy.trait.mappers.EnumTraitMapper;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.CollectionTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumConstantBody;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

class SmithyModelGeneratorTest {

    private static OpenAPI testService;
    private static OpenAPI openApi;
    private static SmithyModelGenerator smithyModelGenerator;
    private static ShapeIndex.Builder shapeIndexBuilder;
    private static Context context;

    @BeforeAll
    private static void parse()
    {
        testService = new OpenAPIV3Parser().read("test-service.openapi.yaml");
        openApi = new OpenAPIV3Parser().read("openapi.yaml");
        smithyModelGenerator = SmithyModelGenerator.create();
        smithyModelGenerator.setMapper(new OpenApi2SmithyMapper() {
            @Override
            public byte getOrder() {
                return 0;
            }
        });
        smithyModelGenerator.setNamespace("ns.foo");
        new OpenApi2SmithyCoreExtension().getOpenApi2SmithyTraitMappers().forEach(smithyModelGenerator::addTraitMapper);
        shapeIndexBuilder = ShapeIndex.builder();
        context = new Context(testService, smithyModelGenerator);
    }

    @ParameterizedTest
    @MethodSource("simpleDataTypes")
    public void testCreateShapeFromSchemaWithTypesAndFormats(String shapeId, AbstractShapeBuilder builder, List<Trait> traitList) { ;
        Optional<Shape> actualShape =
                smithyModelGenerator.create(ShapeId.fromParts("ns.foo", shapeId),
                        testService.getComponents().getSchemas().get(shapeId), "", shapeIndexBuilder, context);
        traitList = traitList == null ? new ArrayList<>() : traitList;
        Shape expectedShape = (Shape)builder.id("ns.foo#" + shapeId).addTraits(traitList).build();

        assertEquals(Optional.of(expectedShape), actualShape);
    }

    @ParameterizedTest
    @MethodSource("dataArrayType")
    public void testCreateShapeFromSchemaWithArrayType(String shapeId, Map<String,String> memberTargetMap, boolean uniqueItemsAsList, AbstractShapeBuilder builder, Trait trait) {
        Optional<Shape> actualShape;
        Shape expectedShape;

        actualShape =  smithyModelGenerator.create(ShapeId.fromParts("ns.foo",shapeId), testService.getComponents().getSchemas().get(shapeId), "", shapeIndexBuilder, context);
        builder.id("ns.foo#" + shapeId);
        memberTargetMap.forEach((key, value) -> {
            MemberShape.Builder memberBuilder = MemberShape.builder().id("ns.foo#" + shapeId + "$" + key).target(value);
            if(trait != null) {
                memberBuilder.addTrait(trait);
            }
            builder.addMember(memberBuilder.build());
        });
        expectedShape = (Shape) builder.build();
        assertEquals(Optional.of(expectedShape), actualShape);
    }


    @Test
    public void testCreateShapeFromSchemaWithArrayTypeWithInlineSchemas() {
        Optional<Shape> actualShape;
        Shape expectedShape;
        Shape inlineShape;

        actualShape =  smithyModelGenerator.create(ShapeId.fromParts("ns.foo","ArrayTypeItemsTypeArrayItemsTypeIntegerSchemaExample"), testService.getComponents().getSchemas().get("ArrayTypeItemsTypeArrayItemsTypeIntegerSchemaExample"), "", shapeIndexBuilder, context);
        inlineShape = ListShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeArrayItemsTypeIntegerSchemaExample_Member")
                .addMember(MemberShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeArrayItemsTypeIntegerSchemaExample_Member" + "$" + "member").target("smithy.api#Long").build())
                .build();
        expectedShape = ListShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeArrayItemsTypeIntegerSchemaExample")
                .addMember(MemberShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeArrayItemsTypeIntegerSchemaExample" + "$" + "member").target(inlineShape.getId()).build())
                .build();
        assertEquals(Optional.of(expectedShape), actualShape);

        actualShape =  smithyModelGenerator.create(ShapeId.fromParts("ns.foo","ArrayTypeItemsTypeObjectSchemaExample"), testService.getComponents().getSchemas().get("ArrayTypeItemsTypeObjectSchemaExample"), "", shapeIndexBuilder, context);
        inlineShape = StructureShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeObjectSchemaExample_Member")
                .addMember(MemberShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeObjectSchemaExample_Member" + "$" + "id").target("smithy.api#String").build())
                .build();
        expectedShape = ListShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeObjectSchemaExample")
                .addMember(MemberShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeObjectSchemaExample" + "$" + "member").target(inlineShape.getId()).build())
                .build();
        assertEquals(Optional.of(expectedShape), actualShape);

        actualShape =  smithyModelGenerator.create(ShapeId.fromParts("ns.foo","ArrayTypeItemsTypeNoneSchemaExample"), testService.getComponents().getSchemas().get("ArrayTypeItemsTypeNoneSchemaExample"), "", shapeIndexBuilder, context);
        inlineShape = StructureShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeNoneSchemaExample_Member")
                .addMember(MemberShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeNoneSchemaExample_Member" + "$" + "id").target("smithy.api#String").build())
                .build();
        expectedShape = ListShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeNoneSchemaExample")
                .addMember(MemberShape.builder().id("ns.foo#" + "ArrayTypeItemsTypeNoneSchemaExample" + "$" + "member").target(inlineShape.getId()).build())
                .build();
        assertEquals(Optional.of(expectedShape), actualShape);
    }

    @ParameterizedTest
    @MethodSource("dataObjectType")
    public void testCreateShapeFromSchemaWithObjectType(String shapeId, Map<String, String> memberTargetMap) {
        Optional<Shape> actualShape;
        Shape expectedShape;

        actualShape =   smithyModelGenerator.create(ShapeId.fromParts("ns.foo",shapeId), testService.getComponents().getSchemas().get(shapeId), "", shapeIndexBuilder, context);
        StructureShape.Builder shapeBuilder = StructureShape.builder().id("ns.foo#"+shapeId);
        memberTargetMap.forEach((key, value) -> {
            /*shapeBuilder.addMember(MemberShape.builder()
                .id("ns.foo#" + shapeId + "$" + key)
                .target(value).build());*/
            MemberShape.Builder builder = MemberShape.builder().id("ns.foo#" + shapeId + "$" + key)
                    .target(value);
            if(ListUtils.of(SMITHY_API_BOOLEAN, SMITHY_API_BYTE, SMITHY_API_INTEGER, SMITHY_API_LONG, SMITHY_API_FLOAT, SMITHY_API_DOUBLE).contains(value)) {
                builder.addTrait(new BoxTrait());
            }
            shapeBuilder.addMember(builder.build());
        });
        expectedShape = shapeBuilder.build();
        assertEquals(Optional.of(expectedShape), actualShape);

    }

    @Test
    public void testCreateShapeFromSchemaWithObjectTypeAndInlineSchemas() {
        Optional<Shape> actualShape;
        Shape expectedShape;
        Shape inlineShape;

        actualShape = smithyModelGenerator.create(ShapeId.fromParts("ns.foo","ObjectSchemaInlineObjectTypeExample"), testService.getComponents().getSchemas().get("ObjectSchemaInlineObjectTypeExample"), "", shapeIndexBuilder, context);
        inlineShape = StructureShape.builder().id("ns.foo#ObjectSchemaInlineObjectTypeExample_inlineObj").addMember(MemberShape.builder().id("ns.foo#ObjectSchemaInlineObjectTypeExample_inlineObj$foo").target(SMITHY_API_STRING).build()).build();
        expectedShape = StructureShape.builder().id("ns.foo#ObjectSchemaInlineObjectTypeExample")
                .addMember(MemberShape.builder().id("ns.foo#ObjectSchemaInlineObjectTypeExample$inlineObj").target(inlineShape.getId()).build())
                .addMember(MemberShape.builder().id("ns.foo#ObjectSchemaInlineObjectTypeExample$id").target(SMITHY_API_LONG).addTrait(new BoxTrait()).build())
                .addMember(MemberShape.builder().id("ns.foo#ObjectSchemaInlineObjectTypeExample$name").target(SMITHY_API_STRING).build())
                .build();
        assertEquals(Optional.of(expectedShape), actualShape);
    }

    @ParameterizedTest
    @MethodSource("operationsData")
    public void testProcessOperation(String path, Operation operation, String httpMethod, String operationId, Shape inputShape, Shape outputShape, List<Shape> errorsList, List<Trait> traitList)
    {
        OperationShape.Builder operationShapeBuilder = OperationShape.builder().id("ns.foo#"+operationId);
        operationShapeBuilder.input(inputShape).output(outputShape);
        errorsList.forEach(operationShapeBuilder::addError);
        traitList.forEach(operationShapeBuilder::addTrait);
        Shape expectedShape = operationShapeBuilder.build();
        Shape actualShape = smithyModelGenerator.createOperationShape(path, operation, httpMethod, shapeIndexBuilder, context);

        assertEquals(inputShape, shapeIndexBuilder.build().getShape(inputShape.getId()).get());
        assertEquals(outputShape, shapeIndexBuilder.build().getShape(outputShape.getId()).get());
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testProcessOperationGetListPets()
    {
        Shape actualShape;
        Shape expectedShape;

        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Error"), testService.getComponents().getSchemas().get("Error"), "", shapeIndexBuilder, new Context(testService, smithyModelGenerator)).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pets"), testService.getComponents().getSchemas().get("Pets"), "", shapeIndexBuilder, context).get());
        OperationShape.Builder builder = OperationShape.builder().id("ns.foo#listPets");
        Shape paramShape = IntegerShape.builder().id("ns.foo#limit").build();
        Shape inputShape = StructureShape.builder().id("ns.foo#listPetsInput").addMember(MemberShape.builder().id("ns.foo#listPetsInput$"+paramShape.getId().getName()).target(paramShape.getId()).addTrait(new HttpQueryTrait("limit")).build()).build();
        builder.input(inputShape);
        Shape header = StringShape.builder().id("ns.foo#xnext").addTrait(new HttpHeaderTrait("x-next")).build();
        Shape outputShape = StructureShape.builder().id("ns.foo#listPetsOutput")
                .addMember(MemberShape.builder().id("ns.foo#listPetsOutput$xnext").target(header.getId()).addTrait(new HttpHeaderTrait("x-next")).build())
                .addMember(MemberShape.builder().id("ns.foo#listPetsOutput$pets").target("ns.foo#Pets").addTrait(new HttpPayloadTrait()).build())
                .build();
        builder.output(outputShape);
        builder.addError(shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#Error")).get());
        builder.addTrait(HttpTrait.builder().uri(UriPattern.parse("/pets")).method(GET.toString()).code(200).build());
        builder.addTrait(TagsTrait.builder().addValue("pets").build());
        builder.addTrait(new ReadonlyTrait()).addTrait(new CollectionTrait());
        expectedShape = builder.build();
        actualShape = smithyModelGenerator.createOperationShape("/pets", testService.getPaths().get("/pets").getGet(), GET.toString(), shapeIndexBuilder, context);
        assertEquals(inputShape, shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#listPetsInput")).get());
        assertEquals(outputShape, shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#listPetsOutput")).get());
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testProcessOperationPostCreatePets()
    {
        Shape actualShape;
        Shape expectedShape;
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Error"), testService.getComponents().getSchemas().get("Error"), "", shapeIndexBuilder, context).get());
        OperationShape.Builder builder = OperationShape.builder().id("ns.foo#createPets");
        builder.addError(shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#Error")).get());
        builder.addTrait(HttpTrait.builder().uri(UriPattern.parse("/pets")).method(POST.toString()).code(201).build());
        builder.addTrait(TagsTrait.builder().addValue("pets").build());
        builder.addTrait(new IdempotentTrait()).addTrait(new CollectionTrait());
        expectedShape = builder.build();
        actualShape = smithyModelGenerator.createOperationShape("/pets", testService.getPaths().get("/pets").getPost(), POST.toString(), shapeIndexBuilder, context);
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testProcessOperationGetShowPetById()
    {
        Shape actualShape;
        Shape expectedShape;

        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Error"), testService.getComponents().getSchemas().get("Error"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pets"), testService.getComponents().getSchemas().get("Pets"), "", shapeIndexBuilder, context).get());

        OperationShape.Builder builder = OperationShape.builder().id("ns.foo#showPetById");
        Shape paramShape = StringShape.builder().id("ns.foo#petId").addTrait(new HttpLabelTrait()).build();
        Shape inputShape = StructureShape.builder().id("ns.foo#showPetByIdInput")
                .addMember(MemberShape.builder().id("ns.foo#showPetByIdInput$" + paramShape.getId().getName()).target(paramShape.getId()).addTrait(new HttpLabelTrait()).addTrait(new RequiredTrait()).build())
                .build();
        builder.input(inputShape);
        StructureShape output = StructureShape.builder().id("ns.foo#showPetByIdOutput").build();
        //TODO : Add Pets as a member
        builder.output(output);
        builder.addError(shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#Error")).get());
        builder.addTrait(HttpTrait.builder().uri(UriPattern.parse("/pets/{petId}")).method(GET.toString()).code(200).build());
        builder.addTrait(TagsTrait.builder().addValue("pets").build());
        builder.addTrait(new ReadonlyTrait());
        expectedShape = builder.build();
        actualShape = smithyModelGenerator.createOperationShape("/pets/{petId}", testService.getPaths().get("/pets/{petId}").getGet(), GET.toString(), shapeIndexBuilder, context);
        assertEquals(inputShape, shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#showPetByIdInput")).get());
        //TODO : assert for Output shape
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testProcessOperationFindPetsByStatus() {
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Category"), openApi.getComponents().getSchemas().get("Category"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Tag"), openApi.getComponents().getSchemas().get("Tag"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pet"), openApi.getComponents().getSchemas().get("Pet"), "", shapeIndexBuilder, context).get());

        Shape actualShape;
        Shape expectedShape;

        OperationShape.Builder builder = OperationShape.builder().id("ns.foo#findPetsByStatus");
        StructureShape.Builder inputBuilder =  StructureShape.builder().id("ns.foo#findPetsByStatusInput");
        Shape paramShape = ListShape.builder().id("ns.foo#status")
                .addMember(MemberShape.builder().id("ns.foo#status$member")
                        .addTrait(EnumTrait.builder().addEnum("available", EnumConstantBody.builder().build()).addEnum("pending", EnumConstantBody.builder().build()).addEnum("sold", EnumConstantBody.builder().build()).build())
                        .target(SMITHY_API_STRING).build())
                .build();
        inputBuilder.addMember(MemberShape.builder().id(inputBuilder.getId() + "$" +paramShape.getId().getName()).target(paramShape.getId()).addTrait(new RequiredTrait()).addTrait(new HttpQueryTrait("status")).build());
        //Shape output = ListShape.builder().id("ns.foo#findPetsByStatusOutput").addMember(MemberShape.builder().id("ns.foo#findPetsByStatusOutput$member").target("ns.foo#Pet").build()).build();
        //builder.input(inputBuilder.build());

        ListShape petList = ListShape.builder().id("ns.foo#findPetsByStatusOutput_Member").addMember(MemberShape.builder().id("ns.foo#findPetsByStatusOutput_Member$member").target("ns.foo#Pet").build()).build();
        Shape output = StructureShape.builder().id("ns.foo#findPetsByStatusOutput").addMember(MemberShape.builder().id("ns.foo#findPetsByStatusOutput$findpetsbystatusoutput_member").target(petList.getId()).addTrait(new HttpPayloadTrait()).build()).build();
        builder.input(inputBuilder.build());
        builder.output(output);
        builder.addError(StructureShape.builder().id("ns.foo#BadRequestException")
                .addMember(MemberShape.builder().id("ns.foo#BadRequestException$message").target(SMITHY_API_STRING).addTrait(new ErrorTrait("client")).addTrait(new HttpErrorTrait(400)).build()).build());
        builder.addTrait(HttpTrait.builder().uri(UriPattern.parse("/pet/findByStatus/MultipleExamples")).method(GET.toString()).code(200).build());
        builder.addTrait(TagsTrait.builder().addValue("pet").build());
        builder.addTrait(new ReadonlyTrait());
        builder.addTrait(new CollectionTrait());
        expectedShape = builder.build();
        actualShape = smithyModelGenerator.createOperationShape("/pet/findByStatus/MultipleExamples", openApi.getPaths().get("/pet/findByStatus/MultipleExamples").getGet(), GET.toString(), shapeIndexBuilder, context);

        assertEquals(inputBuilder.build(), shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#findPetsByStatusInput")).get());
        assertEquals(output, shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#findPetsByStatusOutput")).get());
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testProcessOperationFindPetsByStatusSingle() {

        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Category"), openApi.getComponents().getSchemas().get("Category"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Tag"), openApi.getComponents().getSchemas().get("Tag"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pet"), openApi.getComponents().getSchemas().get("Pet"), "", shapeIndexBuilder, context).get());

        Shape actualShape;
        Shape expectedShape;

        OperationShape.Builder builder = OperationShape.builder().id("ns.foo#findPetsByStatusSingle");
        StructureShape.Builder inputBuilder =  StructureShape.builder().id("ns.foo#findPetsByStatusSingleInput");
        Shape paramShape = ListShape.builder().id("ns.foo#status")
                .addMember(MemberShape.builder().id("ns.foo#status$member")
                        .addTrait(EnumTrait.builder().addEnum("available", EnumConstantBody.builder().build()).addEnum("pending", EnumConstantBody.builder().build()).addEnum("sold", EnumConstantBody.builder().build()).build())
                        .target(SMITHY_API_STRING).build())
                .addTrait(new RequiredTrait())
                .addTrait(new HttpQueryTrait("status"))
                .build();
        inputBuilder.addMember(MemberShape.builder().id(inputBuilder.getId() + "$" +paramShape.getId().getName()).target(paramShape.getId()).addTrait(new RequiredTrait()).addTrait(new HttpQueryTrait("status")).build());
        ListShape petList = ListShape.builder().id("ns.foo#findPetsByStatusSingleOutput_Member").addMember(MemberShape.builder().id("ns.foo#findPetsByStatusSingleOutput_Member$member").target("ns.foo#Pet").build()).build();
        Shape output = StructureShape.builder().id("ns.foo#findPetsByStatusSingleOutput").addMember(MemberShape.builder().id("ns.foo#findPetsByStatusSingleOutput$findpetsbystatussingleoutput_member").target(petList.getId()).addTrait(new HttpPayloadTrait()).build()).build();
        builder.input(inputBuilder.build());
        builder.output(output);
        builder.addError(StructureShape.builder().id("ns.foo#BadRequestException")
                .addMember(MemberShape.builder().id("ns.foo#BadRequestException$message").target(SMITHY_API_STRING).addTrait(new ErrorTrait("client")).addTrait(new HttpErrorTrait(400)).build()).build());
        builder.addTrait(HttpTrait.builder().uri(UriPattern.parse("/pet/findByStatus/singleExample")).method(GET.toString()).code(200).build());
        builder.addTrait(TagsTrait.builder().addValue("pet").build());
        builder.addTrait(new ReadonlyTrait());
        builder.addTrait(new CollectionTrait());
        expectedShape = builder.build();
        actualShape = smithyModelGenerator.createOperationShape("/pet/findByStatus/singleExample", openApi.getPaths().get("/pet/findByStatus/singleExample").getGet(), GET.toString(), shapeIndexBuilder, context);
        assertEquals(inputBuilder.build(), shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#findPetsByStatusSingleInput")).get());
        assertEquals(output, shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#findPetsByStatusSingleOutput")).get());
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testProcessOperationAddPet() {

        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Category"), openApi.getComponents().getSchemas().get("Category"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Tag"), openApi.getComponents().getSchemas().get("Tag"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pet"), openApi.getComponents().getSchemas().get("Pet"), "", shapeIndexBuilder, context).get());

        Shape actualShape;
        Shape expectedShape;

        OperationShape.Builder builder = OperationShape.builder().id("ns.foo#addPet");
        Shape input = shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#Pet")).get();
        builder.input(input);
        builder.addError(StructureShape.builder().id("ns.foo#MethodNotAllowedException")
                .addMember(MemberShape.builder().id("ns.foo#MethodNotAllowedException$message").target(SMITHY_API_STRING).build())
                .addTrait(new ErrorTrait("client"))
                .addTrait(new HttpErrorTrait(405))
                .build());
        builder.addTrait(HttpTrait.builder().uri(UriPattern.parse("/pet")).method(POST.toString()).code(200).build());
        builder.addTrait(TagsTrait.builder().addValue("pet").build());
        builder.addTrait(new IdempotentTrait());
        builder.addTrait(new CollectionTrait());
        builder.addTrait(AuthTrait.builder().addValue("http-bearer").build());
        expectedShape = builder.build();

        actualShape = smithyModelGenerator.createOperationShape("/pet", openApi.getPaths().get("/pet").getPost(), POST.toString(), shapeIndexBuilder, context);
        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testCreateService()
    {
        Shape expectedShape = ServiceShape.builder().id("ns.foo#TestService").version("1.0.0")
                .addTrait(new TitleTrait("Swagger Petstore"))
                .addTrait(new ExternalDocumentationTrait("http://swagger.io"))
                .addTrait(new DocumentationTrait("This is a sample server Petstore server."))
                .addTrait(ProtocolsTrait.builder()
                        .addProtocol(Protocol.builder().name("rest-xml").addAuth("http-basic").addAuth("http-bearer").addAuth("http-x-api-key").build())
                        .addProtocol(Protocol.builder().name("rest-json").addAuth("http-basic").addAuth("http-bearer").addAuth("http-x-api-key").build())
                        .build())
                .build();
        Shape actualShape = smithyModelGenerator.createServiceShape(openApi, shapeIndexBuilder, new HashMap(), context);

        assertEquals(expectedShape, actualShape);
    }

    @Test
    public void testIndex()
    {
        StringShape shape = StringShape.builder().id("ns.foo#name").build();
        Node node = Node.from("Text");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#documentation"), ShapeId.from("ns.foo#name"), node);
        DocumentationTrait documentationTrait = (DocumentationTrait) trait.get();
        assertThat(documentationTrait.getValue(), equalTo("Text"));
    }


    public static Collection<Object[]> simpleDataTypes() {
        return Arrays.asList(new Object[][] {
                { "StringTypeSchemaExample", StringShape.builder(), null},
                { "StringTypeDateFormatSchemaExample", TimestampShape.builder(), null},
                { "StringTypeDateTimeFormatSchemaExample", TimestampShape.builder(), null},
                { "StringTypePasswordFormatSchemaExample", StringShape.builder(), null},
                { "StringTypeByteFormatSchemaExample", ByteShape.builder(), null},
                { "StringTypeBinaryFormatSchemaExample", BlobShape.builder(), null},
                { "StringTypeEmailFormatSchemaExample", StringShape.builder(), null},
                { "StringTypeUUIDFormatSchemaExample", StringShape.builder(), null},
                { "StringTypeURIFormatSchemaExample", StringShape.builder(), null},
                { "StringTypeHostnameFormatSchemaExample", StringShape.builder(), null},
                { "StringTypeIPV4FormatSchemaExample", StringShape.builder(),
                        ListUtils.of(new PatternTrait("^(([01]?\\d\\d?|2[0-4]\\\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"))},
                { "StringTypeIPV6FormatSchemaExample", StringShape.builder(), null},
                { "StringTypePatternSchemaExample", StringShape.builder(), ListUtils.of(new PatternTrait("^\\d{3}-\\d{2}-\\d{4}$"), new DocumentationTrait("SSN Pattern"))},
                { "NumberTypeSchemaExample", DoubleShape.builder(), null},
                { "NumberTypeFloatFormatSchemaExample", FloatShape.builder(), null},
                { "NumberTypeDoubleFormatSchemaExample", DoubleShape.builder(), null},
                { "IntegerTypeSchemaExample", LongShape.builder(), null},
                { "IntegerInt32FormatTypeSchemaExample", IntegerShape.builder(), null},
                { "IntegerTypeInt64FormatSchemaExample", LongShape.builder(), null},
                { "BooleanTypeSchemaExample", BooleanShape.builder(), null},
        });
    }

    public static Collection<Object[]> dataObjectType() {

        Map<String,String> memberTargetMap = new HashMap<>();
        memberTargetMap.put("member1", SMITHY_API_STRING);
        memberTargetMap.put("member2", SMITHY_API_TIMESTAMP);
        memberTargetMap.put("member3", SMITHY_API_TIMESTAMP);
        memberTargetMap.put("member4", SMITHY_API_STRING);
        memberTargetMap.put("member5", SMITHY_API_BYTE);
        memberTargetMap.put("member6", SMITHY_API_BLOB);
        memberTargetMap.put("member7", SMITHY_API_STRING);
        memberTargetMap.put("member8", SMITHY_API_STRING);
        memberTargetMap.put("member9", SMITHY_API_STRING);
        memberTargetMap.put("member10", SMITHY_API_STRING);
        memberTargetMap.put("member11", SMITHY_API_STRING);
        memberTargetMap.put("member12", SMITHY_API_DOUBLE);
        memberTargetMap.put("member13", SMITHY_API_FLOAT);
        memberTargetMap.put("member14", SMITHY_API_DOUBLE);
        memberTargetMap.put("member15", SMITHY_API_LONG);
        memberTargetMap.put("member16", SMITHY_API_INTEGER);
        memberTargetMap.put("member17", SMITHY_API_LONG);
        memberTargetMap.put("member18", SMITHY_API_BOOLEAN);

        return Arrays.asList(new Object[][] {
                { "ObjectSchemaExample",
                        MapUtils.of(
                                "id", SMITHY_API_LONG,
                                "name", SMITHY_API_STRING)
                },
                { "ObjectSchemaPropertyRefExample",
                        MapUtils.of(
                                "foo", SMITHY_API_STRING,
                                "baz","ns.foo#"+"ObjectSchemaExample",
                                "bar", SMITHY_API_INTEGER
                        )
                },
                { "ObjectSchemaWithoutTypeExample",
                        MapUtils.of(
                                "id", SMITHY_API_LONG,
                                "name", SMITHY_API_STRING
                        )
                },
                { "ObjectSchemaAllMemberExample", memberTargetMap}
        });
    }

    public static Collection<Object[]> dataArrayType() {
        return Arrays.asList(new Object[][] {
                { "ArrayTypeItemsTypeIntegerSchemaExample",
                        MapUtils.of("member", SMITHY_API_LONG),
                        false, ListShape.builder(),
                        new BoxTrait()
                },
                { "ArrayTypeItemsTypeRefSchemaExample",
                        MapUtils.of("member","ns.foo#"+"ObjectSchemaExample"),
                        false, ListShape.builder(),
                        null
                },
                { "ArrayTypeUniqueItemsSchemaExample",
                        MapUtils.of("member", SMITHY_API_LONG),
                        true, SetShape.builder(),
                        new BoxTrait()
                },
        });

    }

    public static Collection<Object[]> operationsData() {

        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Error"), testService.getComponents().getSchemas().get("Error"), "", shapeIndexBuilder, context).get());
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pets"), testService.getComponents().getSchemas().get("Pets"), "", shapeIndexBuilder, context).get());


        Shape paramShape = IntegerShape.builder().id("ns.foo#limit").addTrait(new HttpQueryTrait("limit")).build();
        Shape listPetsInput = StructureShape.builder().id("ns.foo#listPetsInput").addMember(MemberShape.builder().id("ns.foo#listPetsInput$"+paramShape.getId().getName()).target(paramShape.getId()).addTrait(new HttpQueryTrait("limit")).build()).build();

        Shape header = StringShape.builder().id("ns.foo#xnext").addTrait(new HttpHeaderTrait("x-next")).build();
        Shape listPetsOutput = StructureShape.builder().id("ns.foo#listPetsOutput")
                .addMember(MemberShape.builder().id("ns.foo#listPetsOutput$xnext").target(header.getId()).addTrait(new HttpHeaderTrait("x-next")).build())
                .addMember(MemberShape.builder().id("ns.foo#listPetsOutput$pets").target("ns.foo#Pets").addTrait(new HttpPayloadTrait()).build())
                .build();

        List<Shape> listPetsErrors = ListUtils.of(shapeIndexBuilder.build().getShape(ShapeId.from("ns.foo#Error")).get());
        List<Trait> listPetsTraits = ListUtils.of(HttpTrait.builder().uri(UriPattern.parse("/pets")).method(GET.toString()).code(200).build(),
                TagsTrait.builder().addValue("pets").build(), new ReadonlyTrait(), new CollectionTrait());

        return Arrays.asList(new Object[][] {
                {"/pets", testService.getPaths().get("/pets").getGet(), GET.toString(), "listPets", listPetsInput, listPetsOutput, listPetsErrors, listPetsTraits},
        });
    }

    /*@Test
    public void testMem()
    {
        shapeIndexBuilder.addShape(smithyModelGenerator.create(ShapeId.fromParts("ns.foo","Pet"), testService.getComponents().getSchemas().get("Pet"), "", shapeIndexBuilder, context).get());
        Shape shape = StringShape.builder().id("ns.foo#petId").build();
        Shape structureShape = StructureShape.builder()
                .id("ns.foo#Input")
                .addMember(MemberShape.builder().id(ShapeId.fromParts("ns.foo","Input","petId")).target("ns.foo#petId").addTrait(new HttpLabelTrait()).build())
                .build();
        Model model = Model.assembler().addShape(structureShape).assemble().unwrap();
    }*/

}