package com.gridlastic.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class CustomListener extends TestListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(CustomListener.class);

    @Override
    public void onTestFailure(ITestResult tr) {
        ITestContext testContext = tr.getTestContext();
        logger.error(" FAILURE: TEST : {} PLATFORM : {} METHOD: {} REASON: {} VIDEO: {}", testContext.getCurrentXmlTest().getParameter("test-title"), testContext.getCurrentXmlTest().getParameter("platform-name"), tr.getMethod().getMethodName(), tr.getThrowable().getMessage().substring(0, 30).trim(), testContext.getAttribute("video_url"));
    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        logger.warn("Test method skipped : {} ", tr.getName());
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        ITestContext testContext = tr.getTestContext();
        if (testContext.getCurrentXmlTest().getParameter("record-video").equalsIgnoreCase("True")) {
            logger.info("SUCCESS: TEST: {} PLATFORM: {} METHOD: {} VIDEO: {}", testContext.getCurrentXmlTest().getParameter("test-title"), testContext.getCurrentXmlTest().getParameter("platform-name"), tr.getMethod().getMethodName(), testContext.getAttribute("video_url"));
        } else {
            logger.info("SUCCESS: TEST: {} PLATFORM: {} METHOD: {} SCREENSHOT: {}", testContext.getCurrentXmlTest().getParameter("test-title"), testContext.getCurrentXmlTest().getParameter("platform-name"), tr.getMethod().getMethodName(), testContext.getAttribute("screenshot_url"));
        }
    }

}