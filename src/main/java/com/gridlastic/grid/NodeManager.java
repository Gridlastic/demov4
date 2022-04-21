package com.gridlastic.grid;

import com.gridlastic.grid.client.NodeApi;
import com.gridlastic.grid.model.NodeCount;
import com.gridlastic.grid.model.PresetParams;
import com.gridlastic.grid.model.RequestPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NodeManager {
    public static final long SLEEP_DURATION_RETRY_LAUNCH_SERVICE = 200;
    private static final Integer SLEEP_DURATION_CHECK_FOR_NODES = 30_000;
    private static final int RETRY_COUNT_FOR_LAUNCH_SERVICE = 5;
    private final Logger logger = LoggerFactory.getLogger(NodeManager.class);
    private final NodeApi nodeApi;

    public NodeManager(String hubUrl, String gridUsername, String gridAccessKey) {
        this(new NodeApi(hubUrl+"api/v1", gridUsername, gridAccessKey));
    }

    public NodeManager(NodeApi nodeApi) {
        this.nodeApi = nodeApi;
    }


    /**
     * Tries to create <b>nodeCount</b> nodes in timeoutSeconds time
     *
     * @param browserName     browser Name
     * @param browserVersion  browser version
     * @param platformName    platform
     * @param acceptableCount acceptable node count
     * @param desiredCount    desired node count
     * @param timeOutSeconds  timeout
     * @return active nodes created
     */
    public int launchNodes(String browserName, String browserVersion, String platformName, Integer desiredCount, Integer acceptableCount, int timeOutSeconds) throws ApiException {
        int freeNodes;
        int totalWait = 0;
        do {
            freeNodes = getFreeNodeCount(browserName, browserVersion, platformName);
            logger.info("Browser \"{}\" version \"{}\" platform \"{}\" ,free node:\"{}\" acceptable node:\"{}\" ,desired node:\"{}\"", browserName, browserVersion, platformName, freeNodes, acceptableCount, desiredCount);
            if (freeNodes < acceptableCount) {
                callLaunchService(browserName, browserVersion, platformName, desiredCount);
                try {
                    Thread.sleep(SLEEP_DURATION_CHECK_FOR_NODES);
                } catch (InterruptedException e) {
                    logger.error("waitForNodes error in sleep.", e);
                    Thread.currentThread().interrupt();
                }
                totalWait += SLEEP_DURATION_CHECK_FOR_NODES / 1000;
            }
        } while (freeNodes < acceptableCount && totalWait < timeOutSeconds);
        return freeNodes;
    }

    /**
     * Retrieves count of free nodes on the grid
     *
     * @param browserName    browser Name
     * @param browserVersion browser version
     * @param platformName   platform
     * @return number of free nodes
     */
    public int getFreeNodeCount(String browserName, String browserVersion, String platformName) throws ApiException {
        NodeCount count = nodeApi.count(browserName, browserVersion, platformName);
        return count.getFree() != null ? count.getFree() : 0;
    }

    private void callLaunchService(String browserName, String browserVersion, String platformName, Integer nodeCount) {
        for (int i = 0; i < RETRY_COUNT_FOR_LAUNCH_SERVICE; i++) {
            try {
                launchNodesAsync(browserName, browserVersion, platformName, nodeCount);
                break;
            } catch (ApiException e) {
                logger.error("Error when launching nodes will retry , status ", e);
                try {
                    Thread.sleep(SLEEP_DURATION_RETRY_LAUNCH_SERVICE * i);
                } catch (InterruptedException x) {
                    Thread.currentThread().interrupt();
                    logger.error("callLaunchService sleep error", x);
                }
            }
        }
    }


    /**
     * Calls preset service with node count
     *
     * @param browserName    browser Name
     * @param browserVersion browser version
     * @param platformName   platform
     * @param nodeCount      desired node count
     */
    public void launchNodesAsync(String browserName, String browserVersion, String platformName, Integer nodeCount) throws ApiException {
        PresetParams presetParams = new PresetParams();
        presetParams.setBrowserName(PresetParams.BrowserNameEnum.fromValue(browserName));
        presetParams.setBrowserVersion(browserVersion);
        presetParams.setPlatformName(PresetParams.PlatformNameEnum.fromValue(platformName));
        presetParams.setCount(nodeCount);
        RequestPreset request = new RequestPreset().nodes(List.of(presetParams));
        nodeApi.preset(request);
    }

    /**
     * Retrieves number of nodes on the grid with given attributes
     *
     * @param browserName    browser Name
     * @param browserVersion browser version
     * @param platformName   platform
     * @return number of nodes
     */
    public int getNodeCount(String browserName, String browserVersion, String platformName) throws ApiException {
        NodeCount count = nodeApi.count(browserName, browserVersion, platformName);
        return count.getTotal() != null ? count.getTotal() : 0;
    }

    /**
     * Retrieves number of  busy nodes on the grid with given attributes
     *
     * @param browserName    browser Name
     * @param browserVersion browser version
     * @param platformName   platform
     * @return number of busy nodes
     */
    public int getBusyNodeCount(String browserName, String browserVersion, String platformName) throws ApiException {
        NodeCount count = nodeApi.count(browserName, browserVersion, platformName);
        return count.getBusy() != null ? count.getBusy() : 0;
    }

}
