import java.util.stream.Collectors;


import com.zeroc.Ice.*;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

import RegistryModule.ConsultantAuxiliarManagerPrx;
import RegistryModule.TaskManager;
import RegistryModule.TaskManagerPrx;

public class ConsultantServer {

    public ConsultantServer(String masterId) {
    }

    
    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println("arg " + arg);
        }

        try (com.zeroc.Ice.Communicator communicator = initializeCommunicator(args)) {
            String masterId = getProperty(communicator, "Master.ID", "master1");

            System.out.println("Master ID: " + masterId);

            Thread destroyHook = new Thread(() -> communicator.destroy());
            Runtime.getRuntime().addShutdownHook(destroyHook);

            // Create TaskManager
            ObjectAdapter adapter = communicator.createObjectAdapter("TaskManager");
            TaskManager taskManager = new TaskManagerImpl();
            ObjectPrx prx = adapter.add(taskManager, Util.stringToIdentity("SimpleTaskManager"));
            TaskManagerPrx taskManagerPrx = TaskManagerPrx.checkedCast(prx);
            adapter.activate();
            // Create ConsultantServiceManager

            com.zeroc.Ice.ObjectAdapter consultantServerManagerAdapter = communicator.createObjectAdapter("ConsultantServiceManager");
            com.zeroc.Ice.Properties properties = communicator.getProperties();
            com.zeroc.Ice.Identity id = com.zeroc.Ice.Util.stringToIdentity(properties.getProperty("Identity"));
            consultantServerManagerAdapter.add(new ConsultantServiceManagerImpl(communicator, taskManagerPrx, masterId), id);
            consultantServerManagerAdapter.activate();
            communicator.waitForShutdown();
        } catch (LocalException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getProperty(Communicator communicator, String key, String defaultValue) {
        String value = communicator.getProperties().getProperty(key);
        return value.isEmpty() ? defaultValue : value;
    }

    private static Communicator initializeCommunicator(String[] args) {
        try {
            InitializationData initData = new InitializationData();
            initData.properties = Util.createProperties();

            initData.properties.load("config.pub");

            args = initData.properties.parseCommandLineOptions("Master", args);

            return Util.initialize(args, initData);
        } catch (LocalException e) {
            System.err.println("Error initializing Ice: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }
}