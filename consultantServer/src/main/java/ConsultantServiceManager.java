import RegistryModule.ConsultantAuxiliarManagerPrx;
import RegistryModule.TaskManager;
import RegistryModule.TaskManagerPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.LocalException;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

import java.rmi.registry.Registry;
import java.util.stream.Collectors;

public class ConsultantServiceManager implements RegistryModule.ConsultantServiceManager {



    private int poolSize = 8;
    public Communicator communicator;
    public TaskManager taskManager;
    public String masterId;

    @Override
    public void setPoolsize(int n, Current current) {
        this.poolSize = n;
    }

    @Override
    public void searchDocumentsByPath(String path, Current current) {

    }

    private int run(Communicator communicator, TaskManagerPrx taskManager, String masterId) {
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

    private static String getProperty(Communicator communicator, String key, String defaultValue) {
        String value = communicator.getProperties().getProperty(key);
        return value.isEmpty() ? defaultValue : value;
    }

}
