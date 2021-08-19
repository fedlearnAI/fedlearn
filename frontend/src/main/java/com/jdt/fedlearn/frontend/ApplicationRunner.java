package com.jdt.fedlearn.frontend;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @className: ApplicationRunner
 * @description: 根据配置文件启动不同的版本。
 * @author: geyan
 * @createTime: 2021/7/7 5:27 下午
 */
@Component
public class ApplicationRunner {
	private static Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);
	private static final String CONFIG_FILE = "application.yml";
	private static final String JDCHAIN = "jdchain";
	private static final String FLAG = "available";
	private static final String SPRING_CONFIG_LOCATION = "--spring.config.location=";

	public static void main(String[] args) throws IOException {
		InputStream inputStream;
		if(args != null && args.length >0 && args[0].contains(SPRING_CONFIG_LOCATION)){
			String path = args[0].replace(SPRING_CONFIG_LOCATION,"") + CONFIG_FILE;
			try {
				inputStream  = FileUtils.openInputStream(new File(path));
			} catch (IOException e) {
				logger.error("读取外部配置文件异常：{}",e.getMessage());
				return;
			}
		}else {
			inputStream = ClassUtils.getDefaultClassLoader().getResourceAsStream(CONFIG_FILE);
		}
		if(inputStream != null){
			Yaml yaml = new Yaml();
			Map<String,Object> result = yaml.load(inputStream);
			Map<String,Object> jdchain = (Map<String, Object>) result.get(JDCHAIN);
			Boolean flag = (Boolean) jdchain.get(FLAG);
			inputStream.close();
			logger.info("当前启动版本：{}",flag?"区块链版本；":"非区块链版本；");
			if(flag){
				JdchainFederatedApplication.main(args);
			}else {
				FederatedApplication.main(args);
			}
		}else {
			logger.error("没有找到配置文件！");
		}
	}
}