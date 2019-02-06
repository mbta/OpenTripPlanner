package org.opentripplanner.updater.alerts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GtfsEnhancedRealtimeAlertsUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeAlertsUpdater.class);

    private GraphUpdaterManager updaterManager;

    private String url;

    private Map<String, String> alertEffectDetails;

    @Override
    protected void runPolling() {
        try {
            InputStream data = HttpUtils.getData(url);
            if (data == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }

            alertEffectDetails = parseJson(IOUtils.toString(data));
            updaterManager.execute(this::updateGraph);
        } catch (Exception e) {
            LOG.error("Error reading enhanced feed from " + url, e);
        }
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;
        LOG.info("Creating enhanced alert feed updater running every {} seconds: {}", pollingPeriodSeconds, url);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {}

    @Override
    public void teardown() {}

    private Map<String, String> parseJson(String jsonString) throws Exception {
        Map<String, String> result = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonString);

        for (JsonNode node: json.get("entity")) {
            result.put(node.get("id").textValue(), node.get("alert").get("effect_detail").textValue());
        }

        return result;
    }

    private void updateGraph(Graph graph)
    {
        for (AlertPatch alertPatch: graph.getAlertPatches()) {
            String effectDetails = alertEffectDetails.get(alertPatch.getAlert().getId());
            alertPatch.getAlert().setEffectDetails(effectDetails);
        }
    }
}
