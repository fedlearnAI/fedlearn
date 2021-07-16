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
package com.jdt.fedlearn.core;

import org.apache.commons.cli.*;

import java.util.Arrays;

/**
 * 主函数，支持通过直接运行jar包进行模拟多方建模，ID对齐等过程
 * 可以通过指定参数文件运行，
 * 也可以通过命令指定参数
 * 还可以交互式运行
 */
public class App {

    private static final String help = "usage help";

    public static void main(String[] args) {
        //参数解析
        CommandLineParser commandLineParser = new DefaultParser();
        Options OPTIONS = new Options();
        // help
        OPTIONS.addOption(Option.builder("h").longOpt("help").type(String.class).desc(help).build());
        // config
        OPTIONS.addOption(Option.builder("c").longOpt("config").type(String.class).desc("location of the config file").build());


        //当config加载或者解析报错时，直接打印报错信息，并退出
        try {
            CommandLine commandLine = commandLineParser.parse(OPTIONS, args);
            System.out.println(Arrays.toString(commandLine.getArgs()));
            System.out.println(Arrays.toString(Arrays.stream(commandLine.getOptions()).map(Option::getLongOpt).toArray()));
            final boolean h = commandLine.getArgList().contains("h");
            System.out.println("h: " + h);
            String configPath = commandLine.getOptionValue("config", "");
            System.out.println("received:" + configPath);
        } catch (ParseException e) {
            System.out.println("config initial error:" + e.getMessage());
            System.exit(-1);
        }
    }
}
