package software.amazon.openapi.smithy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

class OpenApi2SmithyTest {

    private static PluginContext context;

    @BeforeAll
    private static void parse()
    {
        context = PluginContext.builder()
                .settings(ObjectNode.objectNodeBuilder()
                        .withMember(OpenApi2SmithyConstants.OPENAPI_FILE_LOCATION,"/Users/pgourang/Documents/testservice-openapi")
                        //.withMember(OpenApi2SmithyConstants.OPENAPI_SERVICE_FILE,"test-service.openapi.yaml")
                        //.withMember(OpenApi2SmithyConstants.OPENAPI_SERVICE_FILE,"petstore.yaml")
                        //.withMember(OpenApi2SmithyConstants.OPENAPI_SERVICE_FILE,"openapi.yaml")
                        .withMember(OpenApi2SmithyConstants.OPENAPI_SERVICE_FILE,"IAM.yaml")
                        .withMember(OpenApi2SmithyConstants.SERVICE_NAME,"TestService")
                        .withMember(OpenApi2SmithyConstants.NAMESPACE,"smithy.test")
                        .build())
                .model(Model.builder().build())
                .fileManifest(FileManifest.create(Paths.get("/Users/pgourang/Documents/testservice-openapi")))
                .build();
    }

    @Test
    void testExecute() {
        new OpenApi2Smithy().execute(context);
    }

}