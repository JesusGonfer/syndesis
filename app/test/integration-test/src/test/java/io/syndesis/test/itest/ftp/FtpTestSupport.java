/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.syndesis.test.itest.ftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import io.syndesis.test.container.integration.SyndesisIntegrationRuntimeContainer;
import io.syndesis.test.itest.SyndesisIntegrationTestSupport;

import org.apache.commons.net.ftp.FTPCmd;
import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.SocketUtils;
import org.testcontainers.Testcontainers;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.ftp.client.FtpEndpointConfiguration;
import com.consol.citrus.ftp.server.FtpServer;

/**
 * @author Christoph Deppisch
 */
public abstract class FtpTestSupport extends SyndesisIntegrationTestSupport {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(FtpTestSupport.class);

    static final int FTP_TEST_SERVER_PORT = SocketUtils.findAvailableTcpPort();
    static final int PASSIVE_PORT = SocketUtils.findAvailableTcpPort(35000);
    static {
        Testcontainers.exposeHostPorts(FTP_TEST_SERVER_PORT);
        Testcontainers.exposeHostPorts(PASSIVE_PORT);
    }

    protected FtpServer ftpTestServer = startup(ftpTestServer());

    private FtpServer ftpTestServer() {
        final DataConnectionConfiguration dataConnectionConfiguration = dataConnectionConfiguration();

        FtpEndpointConfiguration endpointConfiguration = new FtpEndpointConfiguration();
        endpointConfiguration.setAutoConnect(true);
        endpointConfiguration.setAutoLogin(true);
        endpointConfiguration.setAutoHandleCommands(
                String.join(",", FTPCmd.PORT.getCommand(),
                        FTPCmd.MKD.getCommand(),
                        FTPCmd.PWD.getCommand(),
                        FTPCmd.CWD.getCommand(),
                        FTPCmd.PASV.getCommand(),
                        FTPCmd.NOOP.getCommand(),
                        FTPCmd.SYST.getCommand(),
                        FTPCmd.LIST.getCommand(),
                        FTPCmd.NLST.getCommand(),
                        FTPCmd.QUIT.getCommand(),
                        FTPCmd.TYPE.getCommand()));
        endpointConfiguration.setPort(FTP_TEST_SERVER_PORT);

        FtpServer ftpServer = new FtpServer(endpointConfiguration);
        ftpServer.setUserManagerProperties(new ClassPathResource("ftp.server.properties", FtpTestSupport.class));
        ftpServer.setAutoStart(true);

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setDataConnectionConfiguration(dataConnectionConfiguration);
        ftpServer.setListenerFactory(listenerFactory);

        return ftpServer;
    }

    private DataConnectionConfiguration dataConnectionConfiguration() {
        DataConnectionConfigurationFactory dataConnectionFactory = new DataConnectionConfigurationFactory();
        dataConnectionFactory.setPassiveExternalAddress(integrationContainer().getInternalHostIp());
        dataConnectionFactory.setPassivePorts(String.valueOf(PASSIVE_PORT));
        return dataConnectionFactory.createDataConnectionConfiguration();
    }

    protected abstract SyndesisIntegrationRuntimeContainer integrationContainer();

    @BeforeAll
    public static void setupFtpUserHome() {
        try {
            Path publicUserDir = getFtpUserHome().resolve("public");
            if (Files.exists(publicUserDir)) {
                try (Stream<Path> files = Files.walk(publicUserDir)) {
                    files.forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOG.warn("Failed to delete file", e);
                        }
                    });
                }
            } else {
                Files.createDirectories(publicUserDir);
            }

            Files.copy(new ClassPathResource("todo.json", FtpToDB_IT.class).getFile().toPath(), publicUserDir.resolve("todo.json"));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to setup files in ftp user home directory", e);
        }
    }

    public static Path getFtpUserHome() {
        return Paths.get("target/ftp/user/syndesis");
    }
}
