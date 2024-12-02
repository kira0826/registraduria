import java.util.stream.Collectors;

import com.zeroc.Ice.*;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

import RegistryModule.ConsultantAuxiliarManagerPrx;
import RegistryModule.TaskManager;
import RegistryModule.TaskManagerPrx;

public class ConsultantServer implements RegistryModule.ConsultantServer {
    private final String masterId;
    private int poolSize = 8;
    private static final String path = "cedulas.txt";

    public ConsultantServer(String masterId) {
        this.masterId = masterId;
    }

    
    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println("arg " + arg);
        }

        try (com.zeroc.Ice.Communicator communicator = initializeCommunicator(args)) {
            String masterId = getProperty(communicator, "Master.ID", "master1");

            System.out.println("Master ID: " + masterId);

            ConsultantServer publisher = new ConsultantServer(masterId);
            Thread destroyHook = new Thread(() -> communicator.destroy());
            Runtime.getRuntime().addShutdownHook(destroyHook);

            // Create TaskManager
            ObjectAdapter adapter = communicator.createObjectAdapter("TaskManager");
            TaskManager taskManager = new TaskManagerImpl(path);
            ObjectPrx prx = adapter.add(taskManager, Util.stringToIdentity("SimpleTaskManager"));
            TaskManagerPrx taskManagerPrx = TaskManagerPrx.checkedCast(prx);

            adapter.activate();

            int status = publisher.run(communicator, destroyHook, taskManagerPrx);
            System.exit(status);
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

    private int run(Communicator communicator, Thread destroyHook, TaskManagerPrx taskManager) {
        System.out.println("Properties: " + communicator.getProperties().getPropertiesForPrefix("").entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));

        try {
            TopicManagerPrx manager = TopicManagerPrx.checkedCast(
                    communicator.propertyToProxy("TopicManager.Proxy"));

            if (manager == null) {
                System.err.println("invalid proxy");
                return 1;
            }

            // Obtener/crear topic específico master-worker (para el worker específico)
            String privateTopicName = String.format("%s.%s", masterId,
                    getProperty(communicator, "Worker.ID", "worker1"));
            System.out.println("Private topic: " + privateTopicName);
            TopicPrx privateTopic = getOrCreateTopic(manager, privateTopicName);

            // Obtener/crear topic general
            String generalTopicName = "general.tasks";
            System.out.println("General topic: " + generalTopicName);
            TopicPrx generalTopic = getOrCreateTopic(manager, generalTopicName);

            // Obtener publishers para ambos topics
            ConsultantAuxiliarManagerPrx privateWorker = ConsultantAuxiliarManagerPrx.uncheckedCast(
                    privateTopic.getPublisher().ice_oneway());
            ConsultantAuxiliarManagerPrx generalWorker = ConsultantAuxiliarManagerPrx.uncheckedCast(
                    generalTopic.getPublisher().ice_oneway());
            generalWorker.setPoolSize(poolSize);
            System.out.println("Publishing events. Press ^C to terminate the application.");
            if(taskManager.getRemainingTasks()==1){
                privateWorker.launch(taskManager);
            }else{
                while (taskManager.getRemainingTasks()>0) {
                    generalWorker.launch(taskManager);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            taskManager.shutdown();
            System.out.println("Resultado:");
            System.out.println(taskManager.getResult());
            return 0;
        } catch (LocalException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private TopicPrx getOrCreateTopic(TopicManagerPrx manager, String topicName) {
        try {
            return manager.retrieve(topicName);
        } catch (com.zeroc.IceStorm.NoSuchTopic e) {
            try {
                return manager.create(topicName);
            } catch (com.zeroc.IceStorm.TopicExists ex) {
                try {
                    return manager.retrieve(topicName);
                } catch (com.zeroc.IceStorm.NoSuchTopic e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public void setPoolsize(int n, Current current) {
        this.poolSize = n;
    }

    @Override
    public void searchDocumentsByPath(String path, Current current) {

    }
}