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
package com.jdt.fedlearn.common.util;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpAddressTest {

    @Test
    public void getLocalHostLANAddress() throws UnknownHostException {
//        InetAddress localHostLANAddress = IpAddress.getLocalHostLANAddress();
//        Assert.assertNotNull(localHostLANAddress);
//        System.out.println(localHostLANAddress.getHostAddress());
        System.out.println(IpAddress.getInet4Address());
    }
}
