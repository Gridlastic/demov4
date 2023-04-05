package com.gridlastic.demo;

import com.gridlastic.grid.ApiException;
import com.gridlastic.grid.NodeManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.io.FileOutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.*;


// Notes: parameters are passed in from Jenkins maven job like:
// goal: test -DseleniumVersion=4.1.4 -DgridUsername=xyx -DgridAccessKey=123 -Dhub=SUBDOMAIN-hub.gridlastic.com -DvideoUrl=https://s3-ap-southeast-2.amazonaws.com/b2729248-ak68-6948-a2y8-80e7479te16a/9ag7b09j-6a38-58w2-bb01-17qw724ce46t 
// You will find these parameters in your Gridlastic dashboard after starting your selenium grid
// Also, the Jenkins hostname is passed in via -DjenkinsHostname from Jenkins maven job


public class TestPrepareEnvironment {
    private static final Logger logger = LoggerFactory.getLogger(TestPrepareEnvironment.class);
    private WebDriver driver;


    @Parameters({"browser-name", "platform-name", "browser-version", "prelaunch-desired-node-count", "prelaunch-acceptable-node-count", "prelaunch-timeout-in-seconds", "gridUsername", "gridAccessKey", "hub"})
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite(String browserName, String platformName, String browserVersion,
                            Integer desiredNodeCount, Integer acceptableNodeCount, Integer preLaunchTimeout,
                            String gridUsername, String gridAccessKey,
                            String hubUrl, ITestContext testContext) throws ApiException {
        logger.info("Before suite \"{}\" is starting...", testContext.getSuite().getName());
        String baseUrl = "https://" + hubUrl + "/";
        NodeManager nodeManager = new NodeManager(baseUrl, gridUsername, gridAccessKey);
        int freeNodesLaunched = nodeManager.launchNodes(browserName, browserVersion, platformName, desiredNodeCount, acceptableNodeCount, preLaunchTimeout);
        logger.info("Total free node count: {} ", freeNodesLaunched);
        logger.info("Before suite \"{}\" finished.", testContext.getSuite().getName());
    }

    @Parameters({"browser-name", "platform-name", "browser-version", "videoUrl", "record-video", "gridUsername", "gridAccessKey", "hub"})
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(String browserName, String platformName, String browserVersion, String videoURL, String recordVideo, String gridUserName, String gridAccessKey, String hubUrl, ITestContext myTestContext) throws Exception {
    //    logger.info("Before method \"{}\" is starting...", myTestContext.getCurrentXmlTest().getName());

        ChromeOptions options = new ChromeOptions();
        options.setCapability("browserVersion", browserVersion);
        options.setCapability("platformName", platformName);
        
        options.addArguments("--remote-allow-origins=*"); // necessary for testing with Chrome 111+ for Selenium V4 users unless using selenium version 4.8.2 and above.

        //video recording
        if (recordVideo.equalsIgnoreCase("True")) {
            Map<String, Object> gridlasticOptions = new HashMap<>();
            gridlasticOptions.put("video", "true");
            options.setCapability("gridlastic:options", gridlasticOptions);
        }


        // Increase timeout otherwise there might not be enough time to launch new grid nodes.
        ClientConfig config = ClientConfig.defaultConfig().readTimeout(Duration.ofMinutes(10));
        driver = RemoteWebDriver.builder().address(new URL("https://" + gridUserName + ":" + gridAccessKey + "@" + hubUrl + "/wd/hub")).oneOf(options).config(config).build();
        driver = new Augmenter().augment(driver);
        driver.manage().window().maximize();

        // Video url
        if (recordVideo.equalsIgnoreCase("True")) {
            myTestContext.setAttribute("video_url", videoURL + "/play.html?" + ((RemoteWebDriver) driver).getSessionId());
        } else {
            myTestContext.removeAttribute("video_url");
        }
      //  logger.info("Before method \"{}\" finished...", myTestContext.getCurrentXmlTest().getName());
    }


    @Parameters({"test-title", "jenkinsHostname"})
    @Test
    public void test_site(String test_title, String jenkins_hostname, ITestContext myTestContext) throws Exception {
      //  logger.info("Test Method in suite \"{}\" , \"{}\" is starting...", myTestContext.getSuite().getName(), test_title);
        driver.get("data:text/html,<h1>GRIDLASTIC SELENIUM GRID DEMO</h1>");
        Thread.sleep(10_000); //slow down for demo purposes
        driver.get("https://www.gridlastic.com/?demo");
        Thread.sleep(10_000); //slow down for demo purposes

			
   			// Take example screenshot and save it to Jenkins workspace			
			String screenshot_filepath = System.getenv("WORKSPACE")+"/";
			String screenshot_filename = "screenshot_" + ((RemoteWebDriver) driver).getSessionId() + ".png";
			myTestContext.setAttribute("screenshot_url", jenkins_hostname+"/job/"+System.getenv("JOB_NAME")+"/ws/"+screenshot_filename);
			take_screenshot(screenshot_filepath+screenshot_filename); 
		
  //      logger.info("Test Method in suite \"{}\" , \"{}\" finished.", myTestContext.getSuite().getName(), test_title);
    }


    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        driver.quit();
    }


    private void take_screenshot(String file) throws Exception {
        TakesScreenshot ss = (TakesScreenshot) driver;
        String base64Screenshot = ss.getScreenshotAs(OutputType.BASE64);
        byte[] decodedScreenshot = Base64.getDecoder().decode(base64Screenshot.getBytes());
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(decodedScreenshot);
        fos.close();
    }


}