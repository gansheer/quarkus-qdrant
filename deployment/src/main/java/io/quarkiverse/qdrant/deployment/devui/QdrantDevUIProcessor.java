package io.quarkiverse.qdrant.deployment.devui;

import io.quarkiverse.qdrant.runtime.devui.QdrantDevUIService;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildSteps(onlyIf = IsDevelopment.class)
public class QdrantDevUIProcessor {

    @BuildStep
    CardPageBuildItem createCard() {
        CardPageBuildItem card = new CardPageBuildItem();

        card.addPage(Page.webComponentPageBuilder()
                .title("Collections")
                .icon("font-awesome-solid:layer-group")
                .componentLink("qwc-qdrant-collections.js"));

        card.addPage(Page.webComponentPageBuilder()
                .title("Search")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("qwc-qdrant-search.js"));

        return card;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(QdrantDevUIService.class);
    }
}
