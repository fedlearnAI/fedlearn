package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.common.tool.internel.ResponseConstruct;
import com.jdt.fedlearn.coordinator.entity.prepare.KeyGenerateReq;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.core.dispatch.DistributedKeyGeneCoordinator;
import com.jdt.fedlearn.core.encryption.nativeLibLoader;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class SecureKeyGeneImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Serializer serializer = new JavaSerializer();
    private static  boolean loaded = false;

    @Override
    public Map<String, Object> service(String content) throws ParseException {
        try {
            KeyGenerateReq matchQueryReq = new KeyGenerateReq();
            matchQueryReq.parseJson(content);
            generateKeys(matchQueryReq);
            return ResponseConstruct.success();
        } catch (Exception e) {
            logger.error(String.format("MatchProgressImpl Exception :%s ", LogUtil.logLine(e.getMessage())));
            return CommonService.exceptionProcess(e, new HashMap<>());
        }
    }

    public Map<String, Object> generateKeys(KeyGenerateReq req) {
        Map<String, Object> data = new HashMap<>();
        try {
            try {
                if (!loaded) {
                    nativeLibLoader.load();
                }
                loaded = true;
            } catch (UnsatisfiedLinkError e) {
                logger.error("load jni error", e);
                System.exit(1);
            }
            int bitLen = 128;
            List<ClientInfo> clientInfos = req.getClientList().stream().map(ClientInfo::parseUrl).collect(Collectors.toList());
            String[] allAddr = new String[clientInfos.size()];
            int cnt = 0;
            for (ClientInfo client : clientInfos) {
                allAddr[cnt++] = client.getIp() + client.getPort();
            }
            DistributedKeyGeneCoordinator coordinator = new DistributedKeyGeneCoordinator(10 ,clientInfos.size(), bitLen, allAddr, false, "");
            generate(coordinator, clientInfos.toArray(new ClientInfo[0]));
        } catch (Exception e) {
            logger.error("generateKeys", e);
        }
        return data;
    }

    private static void generate(DistributedKeyGeneCoordinator coordinator,
                                 ClientInfo[] clientInfos) {

        System.out.println("Key gene start:");
        long start = System.currentTimeMillis();

        List<CommonResponse> responses = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            responses.add(new CommonResponse(clientInfo, new SingleElement("init_success")));
        }

        while (!coordinator.generationFinished()) {
            List<CommonRequest> requests = coordinator.stateMachine(responses);
            responses.clear();
            for (CommonRequest request : requests) {
                Message message = request.getBody();
                ClientInfo client = request.getClient();
                Map<String, Object> requestBody = new HashMap<>();

                String res = SendAndRecv.send(client, "/co/prepare/key/generate", requestBody, message);
                Message restoreMessage = serializer.deserialize(res);
                CommonResponse response = new CommonResponse(request.getClient(), restoreMessage);
                responses.add(response);
            }
        }
        System.out.println("----------------full client end-------------------\n consumed time in seconds:");
        System.out.println((System.currentTimeMillis() - start) / 1000.0);
    }
}
