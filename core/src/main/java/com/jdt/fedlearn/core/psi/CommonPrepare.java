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

package com.jdt.fedlearn.core.psi;

import com.jdt.fedlearn.core.psi.diffieHellman.DiffieHellmanMatch;
import com.jdt.fedlearn.core.psi.diffieHellman.DiffieHellmanMatchClient;
import com.jdt.fedlearn.core.psi.empty.EmptyMatch;
import com.jdt.fedlearn.core.psi.empty.EmptyMatchClient;
import com.jdt.fedlearn.core.psi.freedman.FreedmanMatch;
import com.jdt.fedlearn.core.psi.freedman.FreedmanMatchClient;
import com.jdt.fedlearn.core.psi.md5.Md5Match;
import com.jdt.fedlearn.core.psi.md5.Md5MatchClient;
import com.jdt.fedlearn.core.psi.mixMd5.MixMd5Match;
import com.jdt.fedlearn.core.psi.mixMd5.MixMd5MatchClient;
import com.jdt.fedlearn.core.psi.rsa.RsaMatch;
import com.jdt.fedlearn.core.psi.rsa.RsaMatchClient;
import com.jdt.fedlearn.core.type.MappingType;

public class CommonPrepare {
    public static Prepare construct(MappingType mappingType) {
        switch (mappingType) {
            case VERTICAL_MD5: {
                return new Md5Match();
            }
            case VERTICAL_RSA: {
                return new RsaMatch();
            }
            case MIX_MD5: {
                return new MixMd5Match();
            }


            case EMPTY: {
                return new EmptyMatch();
            }
            case VERTICAL_DH:{
                return new DiffieHellmanMatch();
            }
            case FREEDMAN:{
                return new FreedmanMatch();
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    public static PrepareClient constructClient(MappingType mappingType) {
        switch (mappingType) {
            case VERTICAL_MD5: {
                return new Md5MatchClient();
            }
            case VERTICAL_RSA: {
                return new RsaMatchClient();
            }
            case MIX_MD5: {
                return new MixMd5MatchClient();
            }

            case EMPTY: {
                return new EmptyMatchClient();
            }
            case VERTICAL_DH:{
                return new DiffieHellmanMatchClient();
            }
            case FREEDMAN:{
                return new FreedmanMatchClient();
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }
}
