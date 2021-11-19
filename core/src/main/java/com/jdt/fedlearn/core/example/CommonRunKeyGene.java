package com.jdt.fedlearn.core.example;

import com.jdt.fedlearn.core.dispatch.DistributedKeyGeneCoordinator;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierKeyGenerator;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommonRunKeyGene {
    private static final Serializer serializer = new JavaSerializer();

    public static void generate(DistributedKeyGeneCoordinator coordinator,
                                ClientInfo[] clientInfos) throws IOException {
        // model create
        Map<ClientInfo, DistributedPaillierKeyGenerator> generatorMap = new HashMap<>(); //每个客户端维护自己的，所以此处有n份
        for (ClientInfo client : clientInfos) {
            DistributedPaillierKeyGenerator generator = new DistributedPaillierKeyGenerator();
            generatorMap.put(client, generator);
        }
        System.out.println("Key gene start:");
        long start = System.currentTimeMillis();

        List<CommonResponse> responses = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            responses.add(new CommonResponse(clientInfo, new SingleElement("init_success")));
        }

        while(!coordinator.generationFinished()) {
            List<CommonRequest> requests = coordinator.stateMachine(responses);
            responses.clear();
            for (CommonRequest request : requests) {
                Message message = request.getBody();
                ClientInfo client = request.getClient();
                //-----------------client process------------------//
                DistributedPaillierKeyGenerator generator = generatorMap.get(client);
                Message answer = generator.stateMachine(message);
                ///mock receive message serialize and deserialize
                String strMessage = serializer.serialize(answer);
                Message restoreMessage = serializer.deserialize(strMessage);
                //-----------------client process end--------------//
                CommonResponse response = new CommonResponse(request.getClient(), restoreMessage);
                responses.add(response);
            }
        }

        int cnt = 1;
        for (ClientInfo client : clientInfos) {
            DistributedPaillierKeyGenerator generator = generatorMap.get(client);
            Map<String, Object> keys = generator.postGeneration();
            Files.write(Paths.get("privKey-" + cnt),
                    ((DistributedPaillier.DistPaillierPrivkey)keys
                            .get("privKey"))
                            .toJson()
                            .getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get("pubKey"),
                    ((DistributedPaillier.DistPaillierPubkey)keys.
                            get("pubKey"))
                            .toJson()
                            .getBytes(StandardCharsets.UTF_8));
            cnt += 1;
        }

        System.out.println("----------------full client end-------------------\n consumed time in seconds:");
        System.out.println((System.currentTimeMillis() - start) / 1000.0);
    }
}
