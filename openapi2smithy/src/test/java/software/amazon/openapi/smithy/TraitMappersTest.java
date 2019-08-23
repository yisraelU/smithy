package software.amazon.openapi.smithy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_LONG;
import static software.amazon.openapi.smithy.OpenApi2SmithyConstants.SMITHY_API_STRING;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

class TraitMappersTest {

    private static OpenAPI testService;
    private static OpenAPI openApi;
    private static SmithyModelGenerator smithyModelGenerator;
    private static ShapeIndex.Builder shapeIndexBuilder;

    @BeforeAll
    private static void parse()
    {
        testService = new OpenAPIV3Parser().read("test-service.openapi.yaml");
        openApi = new OpenAPIV3Parser().read("openapi.yaml");
        smithyModelGenerator = SmithyModelGenerator.create();
        shapeIndexBuilder = ShapeIndex.builder();
        smithyModelGenerator.setNamespace("ns.foo");
        new OpenApi2SmithyCoreExtension().getOpenApi2SmithyTraitMappers().forEach(smithyModelGenerator::addTraitMapper);
        smithyModelGenerator.setMapper(new OpenApi2SmithyMapper() {
            @Override
            public byte getOrder() {
                return 0;
            }
        });
        smithyModelGenerator.config(ObjectNode.objectNodeBuilder().withMember("uniqueItems.list", false).build());
    }

    @Test
    public void testRangeTrait()
    {
        Optional<Shape> actualShape;
        Shape expectedShape;
        expectedShape = LongShape.builder().id("ns.foo#IntegerSchemaWithRange").addTrait(RangeTrait.builder().min(new BigDecimal(1)).max(new BigDecimal(20)).build()).build();
        actualShape = smithyModelGenerator.create(ShapeId.fromParts("ns.foo","IntegerSchemaWithRange"), testService.getComponents().getSchemas().get("IntegerSchemaWithRange"), "", shapeIndexBuilder, new Context(testService, smithyModelGenerator));
        assertEquals(Optional.of(expectedShape), actualShape);
    }

    @Test
    public void testLengthTrait()
    {
        Optional<Shape> actualShape;
        Shape expectedShape;
        StructureShape.Builder builder = StructureShape.builder().id("ns.foo#ObjectSchemaRequiredExample");
        MemberShape memberShape1 = MemberShape.builder().target(SMITHY_API_LONG).id("ns.foo#ObjectSchemaRequiredExample$id").addTrait(new RequiredTrait()).addTrait(new BoxTrait()).build();
        MemberShape memberShape2 = MemberShape.builder().target(SMITHY_API_STRING).id("ns.foo#ObjectSchemaRequiredExample$name").addTrait(new RequiredTrait()).addTrait(LengthTrait.builder().min(3L).max(20L).build()).build();
        MemberShape memberShape3 = MemberShape.builder().target(SMITHY_API_LONG).id("ns.foo#ObjectSchemaRequiredExample$age").addTrait(RangeTrait.builder().min(new BigDecimal(0)).build()).addTrait(new BoxTrait()).build();
        builder.addMember(memberShape1);
        builder.addMember(memberShape2);
        builder.addMember(memberShape3);
        expectedShape = builder.build();
        actualShape = smithyModelGenerator.create(ShapeId.fromParts("ns.foo","ObjectSchemaRequiredExample"), testService.getComponents().getSchemas().get("ObjectSchemaRequiredExample"), "", shapeIndexBuilder, new Context(testService, smithyModelGenerator));
        assertEquals(Optional.of(expectedShape), actualShape);
    }

}