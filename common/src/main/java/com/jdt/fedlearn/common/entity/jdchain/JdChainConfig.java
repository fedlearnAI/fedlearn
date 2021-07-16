/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.common.entity.jdchain;

/**
 * @className: JdChainConfig
 * @description: jdchian相关配置信息
 * @author: geyan29
 * @createTime: 2021/2/2 4:42 下午
 */
public class JdChainConfig {
    private String userPubkey;
    private String userPrivkey;
    private String userPrivpwd;
    private String gatewayIp;
    private String gatewayPort;
    private String gatewaySecure;
    private String contractAddress;
    private String dataAccountAddress;
    private String eventAccountAddress;
    private String userTableAddress;
    private String taskTableAddress;
    private String trainTableAddress;
    private String inferenceTableAddress;
    private String ledgerAddress;

    public JdChainConfig() {
    }

    public JdChainConfig(String userPubkey, String userPrivkey, String userPrivpwd, String gatewayIp, String gatewayPort,
                         String gatewaySecure, String contractAddress, String dataAccountAddress, String eventAccountAddress,
                         String userTableAddress, String taskTableAddress, String trainTableAddress, String ledgerAddress) {
        this.userPubkey = userPubkey;
        this.userPrivkey = userPrivkey;
        this.userPrivpwd = userPrivpwd;
        this.gatewayIp = gatewayIp;
        this.gatewayPort = gatewayPort;
        this.gatewaySecure = gatewaySecure;
        this.contractAddress = contractAddress;
        this.dataAccountAddress = dataAccountAddress;
        this.eventAccountAddress = eventAccountAddress;
        this.userTableAddress = userTableAddress;
        this.taskTableAddress = taskTableAddress;
        this.trainTableAddress = trainTableAddress;
        this.ledgerAddress = ledgerAddress;
    }

    public String getUserPubkey() {
        return userPubkey;
    }

    public void setUserPubkey(String userPubkey) {
        this.userPubkey = userPubkey;
    }

    public String getUserPrivkey() {
        return userPrivkey;
    }

    public void setUserPrivkey(String userPrivkey) {
        this.userPrivkey = userPrivkey;
    }

    public String getUserPrivpwd() {
        return userPrivpwd;
    }

    public void setUserPrivpwd(String userPrivpwd) {
        this.userPrivpwd = userPrivpwd;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public String getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(String gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public String getGatewaySecure() {
        return gatewaySecure;
    }

    public void setGatewaySecure(String gatewaySecure) {
        this.gatewaySecure = gatewaySecure;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getDataAccountAddress() {
        return dataAccountAddress;
    }

    public void setDataAccountAddress(String dataAccountAddress) {
        this.dataAccountAddress = dataAccountAddress;
    }

    public String getEventAccountAddress() {
        return eventAccountAddress;
    }

    public void setEventAccountAddress(String eventAccountAddress) {
        this.eventAccountAddress = eventAccountAddress;
    }

    public String getUserTableAddress() {
        return userTableAddress;
    }

    public void setUserTableAddress(String userTableAddress) {
        this.userTableAddress = userTableAddress;
    }

    public String getTaskTableAddress() {
        return taskTableAddress;
    }

    public void setTaskTableAddress(String taskTableAddress) {
        this.taskTableAddress = taskTableAddress;
    }

    public String getTrainTableAddress() {
        return trainTableAddress;
    }

    public void setTrainTableAddress(String trainTableAddress) {
        this.trainTableAddress = trainTableAddress;
    }

    public String getLedgerAddress() {
        return ledgerAddress;
    }

    public void setLedgerAddress(String ledgerAddress) {
        this.ledgerAddress = ledgerAddress;
    }

    public String getInferenceTableAddress() {
        return inferenceTableAddress;
    }

    public void setInferenceTableAddress(String inferenceTableAddress) {
        this.inferenceTableAddress = inferenceTableAddress;
    }
}
