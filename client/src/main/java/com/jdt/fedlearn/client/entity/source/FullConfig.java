package com.jdt.fedlearn.client.entity.source;

import java.util.List;

/**
 * 映射整个配置文件,
 */
public class FullConfig {

    private String appName;
    private String appPort;
    private String logSettings;
    private String masterAddress;
    private String authToken;
    private List<DataSourceConfig> trainSources;
    private List<DataSourceConfig> testSources;
    private List<DataSourceConfig> inferenceSources;


    public FullConfig() {
    }

    public FullConfig(String appName, String appPort, String logSettings, String masterAddress, String authToken, List<DataSourceConfig> trainSources, List<DataSourceConfig> testSources, List<DataSourceConfig> inferenceSources) {
        this.appName = appName;
        this.appPort = appPort;
        this.logSettings = logSettings;
        this.masterAddress = masterAddress;
        this.authToken = authToken;
        this.trainSources = trainSources;
        this.testSources = testSources;
        this.inferenceSources = inferenceSources;
    }


    public String getAppName() {
        return appName;
    }

    public String getAppPort() {
        return appPort;
    }

    public String getLogSettings() {
        return logSettings;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public List<DataSourceConfig> getTrainSources() {
        return trainSources;
    }

    public List<DataSourceConfig> getTestSources() {
        return testSources;
    }

    public List<DataSourceConfig> getInferenceSources() {
        return inferenceSources;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppPort(String appPort) {
        this.appPort = appPort;
    }

    public void setLogSettings(String logSettings) {
        this.logSettings = logSettings;
    }

    public void setMasterAddress(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setTrainSources(List<DataSourceConfig> trainSources) {
        this.trainSources = trainSources;
    }

    public void setTestSources(List<DataSourceConfig> testSources) {
        this.testSources = testSources;
    }

    public void setInferenceSources(List<DataSourceConfig> inferenceSources) {
        this.inferenceSources = inferenceSources;
    }
}
