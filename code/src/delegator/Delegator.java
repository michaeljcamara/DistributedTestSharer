package delegator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.FilenameUtils;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.junit.runner.Result;
import org.junit.runners.Suite.SuiteClasses;

import server.CustomServerInterface;
import server.SimpleClassLoader;

public class Delegator extends UnicastRemoteObject implements DelegatorInterface {

    private static String host, ftpRootDir;

    private static int registryPort, ftpServerPort;

    // private static LinkedList<File> testList;
    private static LinkedList<File> testSuiteList, testSingleList;

    private static LinkedList<CustomServerInterface> serverList;

    public static void main(String[] args) throws RemoteException {

        // Prevent console logging output
        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().addAppender(new NullAppender());

        host = "192.168.0.101";
        // host = "141.195.23.157";
        registryPort = 12345;
        ftpServerPort = 12346;
        ftpRootDir = "C:/FileZilla/";
        // testList = new LinkedList<File>();
        testSuiteList = new LinkedList<File>();
        testSingleList = new LinkedList<File>();
        serverList = new LinkedList<CustomServerInterface>();

        System.setProperty("java.security.policy", "rmi.policy");
        // System.setSecurityManager(new SecurityManager());
        System.setProperty("java.rmi.server.hostname", host);
        // System.setProperty("java.rmi.server.codebase", "ftp://" + "user" + ":" + "user"+ "@" +
        // host + ":" + ftpServerPort + "/");

        createRegistry();
        createFTPServer();

    }

    protected Delegator() throws RemoteException {
        super();
    }

    private void updateServers() throws RemoteException {
        try {
            System.out.println("Begin update servers");

            for (CustomServerInterface server : serverList) {
                server.updateClassLoader();
            }
            System.out.println("End update servers");
        } catch (Exception e) {
            // e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println(e.getCause());

            try {
                FileWriter writer = new FileWriter(new File("exception.txt"));
                // writer.write(e.getMessage() + System.lineSeparator() + e.getCause());
                for (StackTraceElement a : e.getStackTrace()) {
                    writer.write(a.toString() + System.lineSeparator());
                    writer.flush();
                }
                writer.close();
                System.out.println("DONE");

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    private static void createFTPServer() {

        FtpServerFactory ftpServerFactory = new FtpServerFactory();

        UserManager userManager = ftpServerFactory.getUserManager();
        BaseUser adminUser = new BaseUser();
        adminUser.setName("user");
        adminUser.setPassword("user");
        adminUser.setEnabled(true);
        ArrayList<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        adminUser.setAuthorities(authorities);
        adminUser.setHomeDirectory(ftpRootDir);
        adminUser.setMaxIdleTime(999);
        try {
            userManager.save(adminUser);
        } catch (FtpException e2) {
            e2.printStackTrace();
        }
        ftpServerFactory.setUserManager(userManager);

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(ftpServerPort);
        listenerFactory.setServerAddress(host);
        listenerFactory.setIdleTimeout(999);

        ConnectionConfigFactory connectionFactory = new ConnectionConfigFactory();
        connectionFactory.setAnonymousLoginEnabled(true);
        connectionFactory.setMaxAnonymousLogins(999);
        connectionFactory.setMaxLogins(999);
        connectionFactory.setMaxLoginFailures(999);
        connectionFactory.setMaxThreads(999);
        ftpServerFactory.setConnectionConfig(connectionFactory.createConnectionConfig());

        DataConnectionConfigurationFactory dataFactory = new DataConnectionConfigurationFactory();
        dataFactory.setActiveEnabled(true);
        dataFactory.setActiveIpCheck(false);
        dataFactory.setActiveLocalAddress(host);
        dataFactory.setPassiveAddress(host);
        dataFactory.setIdleTime(999);
        listenerFactory
                .setDataConnectionConfiguration(dataFactory.createDataConnectionConfiguration());
        ftpServerFactory.addListener("default", listenerFactory.createListener());

        NativeFileSystemFactory fileFactory = new NativeFileSystemFactory();
        try {
            fileFactory.createFileSystemView(adminUser);
            fileFactory.setCreateHome(true);
            // fileFactory.setCaseInsensitive(true);
        } catch (FtpException e1) {
            e1.printStackTrace();
        }
        ftpServerFactory.setFileSystem(fileFactory);

        FtpServer ftpServer = ftpServerFactory.createServer();

        try {
            ftpServer.start();
        } catch (FtpException e) {
            e.printStackTrace();
        }
    }

    private static void createRegistry() throws RemoteException {
        // Registry registry = LocateRegistry.createRegistry(registryPort);
        Registry registry = LocateRegistry.createRegistry(registryPort,
            new CustomSocketFactory(registryPort), new CustomSocketFactory(registryPort));
        Delegator delegator = new Delegator();
        registry.rebind("Delegator", delegator);
    }

    public void uploadResources(File file) throws RemoteException {

        System.out.println(file);
        System.out.println(file.getPath());
        System.out.println(file.getName());
        System.out.println(file.toURI().toString());
        System.out.println(file.length());

        for (File subDir : file.listFiles()) {
            if (subDir.getName().equals("test_singles")) {
                createSingleTestList(subDir);
            } else if (subDir.getName().equals("test_suites")) {
                createTestSuiteList(subDir);
            } else {
                createResources(subDir);
            }
        }

        // Update the servers' classpaths and establish connection with the FTP server
        updateServers();
    }

    private void createResources(File file) {
        try {
            if (file.isDirectory()) {
                File targetFile = new File("C:/FileZilla/" + file.toPath());
                targetFile.mkdirs();
                for (File subDir : file.listFiles()) {
                    createResources(subDir);
                }
            } else {

                System.out.println(file.getName());
                System.out.println(file.toPath());
                FileInputStream in = new FileInputStream(file);
                byte[] fileBytes = new byte[(int) file.length()];
                in.read(fileBytes);

                FileOutputStream out = new FileOutputStream("C:/FileZilla/" + file.getPath());
                out.write(fileBytes);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSingleTestList(File file) {

        System.out.println("begin create single tests");
        System.out.println(FilenameUtils.getExtension(file.getName()));

        if (file.isDirectory()) {
            for (File subDir : file.listFiles()) {
                createSingleTestList(subDir);
            }
        } else if (FilenameUtils.getExtension(file.getName()).equals("class")) {
            testSingleList.add(file);
        }
        System.out.println("end create single tests");
    }

    private void createTestSuiteList(File file) {
        System.out.println("begin create suite tests");
        System.out.println(FilenameUtils.getExtension(file.getName()));

        if (file.isDirectory()) {
            for (File subDir : file.listFiles()) {
                createTestSuiteList(subDir);
            }
        } else if (FilenameUtils.getExtension(file.getName()).equals("class")) {
            testSuiteList.add(file);
        }
        System.out.println("end create suite tests");
    }

    public ConcurrentLinkedQueue<Result> runTests() throws RemoteException {

        try {

            ConcurrentLinkedQueue<Result> resultQueue = new ConcurrentLinkedQueue<Result>();
            ConcurrentLinkedQueue<TestAgent> agents = new ConcurrentLinkedQueue<TestAgent>();

            // Add separate agent for each server
            for (CustomServerInterface server : serverList) {
                agents.add(new TestAgent(server, resultQueue));
            }

            // System.setSecurityManager(new SecurityManager());

            // CustomClassLoader simpleLoader = new CustomClassLoader(new URL[]{new
            // URL("ftp://user:user@" + host + ":" + ftpServerPort + "/resources")});
            SimpleClassLoader simpleLoader = new SimpleClassLoader();

            Iterator<File> suiteIterator = testSuiteList.iterator();
            Iterator<File> singleIterator = testSingleList.iterator();

            while (singleIterator.hasNext()) {
                // **TODO: Need to see if able to pass by File, or if need to convert to bytes first
                // FileInputStream in = new FileInputStream(testList.getFirst());
                // byte[] classBytes = new byte[(int) testList.getFirst().length()];
                // in.read(classBytes);

                File testFile = testSingleList.getFirst();
                TestAgent agent = agents.remove();

                if (!agent.isAlive()) {
                    agent = new TestAgent(agent, testFile);
                    agent.start();
                    singleIterator.next();
                }

                agents.add(agent);
            }

            while (suiteIterator.hasNext()) {

                FileInputStream in = new FileInputStream(suiteIterator.next());
                byte[] classBytes = new byte[(int) testSuiteList.getFirst().length()];
                in.read(classBytes);
                Class<?> convertedClass = simpleLoader.convertToClass(classBytes);

                SuiteClasses suiteAnnotation = convertedClass.getAnnotation(SuiteClasses.class);
                Class<?>[] classesInSuite = suiteAnnotation.value();

                for (int i = 0; i < classesInSuite.length;) {
                    Class<?> c = classesInSuite[i];
                    TestAgent agent = agents.remove();

                    if (!agent.isAlive()) {
                        agent = new TestAgent(agent, c);
                        agent.start();
                        i++;
                    }

                    agents.add(agent);
                }
            }

            // Wait until all agents have finished before returning result
            while (!agents.isEmpty()) {
                TestAgent agent = agents.peek();
                if (!agent.isAlive()) {
                    agents.remove();
                }
            }

            return resultQueue;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getCause());

            System.out.println(e.getMessage());
            System.out.println(e.getCause());

            try {
                FileWriter writer = new FileWriter(new File("exception2.txt"));
                // writer.write(e.getMessage() + System.lineSeparator() + e.getCause());
                for (StackTraceElement a : e.getStackTrace()) {
                    writer.write(a.toString() + System.lineSeparator());
                    writer.flush();
                }
                writer.close();
                System.out.println("DONE");
            } catch (Exception f) {
            }

            return null;
        }
    }

    public String ping() throws RemoteException {
        System.out.println("===PING!===");
        return "===PING!===";
    }

    public void rebindServer(CustomServerInterface remoteObject) throws RemoteException {

        try {
            Registry registry = LocateRegistry.getRegistry(registryPort);

            for (String s : registry.list()) {
                System.out.println(s);
            }

            registry.rebind("CustomServer_" + registry.list().length, remoteObject);
            serverList.add(remoteObject);

            System.out.println("CURRENTLY BOUND REMOTE OBJECTS: ");
            for (String s : registry.list()) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}