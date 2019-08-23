package software.amazon.openapi.smithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;

class OpenApi2SmithyTest {

    private static PluginContext context;

    @Test
    void testExecute() {
        MockManifest manifest = new MockManifest();
        context = PluginContext.builder()
                .settings(ObjectNode.objectNodeBuilder()
                        .withMember(OpenApi2SmithyConstants.OPENAPI_FILE_LOCATION,"./src/test/resources")
                        .withMember(OpenApi2SmithyConstants.OPENAPI_SERVICE_FILE,"petstore.yaml")
                        .withMember(OpenApi2SmithyConstants.SERVICE_NAME,"PetService")
                        .withMember(OpenApi2SmithyConstants.NAMESPACE,"smithy.test")
                        .build())
                .model(Model.builder().build())
                .fileManifest(manifest)
                .build();
        new OpenApi2Smithy().execute(context);
        manifest.getFiles();
        assertThat(manifest.getFiles(), hasItem(Paths.get("/PetService.json")));
    }

}