import RegistryModule.*;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.LocalException;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

import java.util.stream.Collectors;

public class ConsultantServiceManagerImpl implements RegistryModule.ConsultantServiceManager {

    public ConsultantServiceManagerImpl(Communicator communicator, TaskManagerPrx taskManager, String masterId){
        this.poolSize = 8;
        this.communicator = communicator;
        this.taskManager = taskManager;
        this.masterId = masterId;
    }
    private int poolSize;
    private Communicator communicator;
    private TaskManagerPrx taskManager;
    private String masterId;

    @Override
    public void setPoolsize(int n, Current current) {
        this.poolSize = n;
    }

    @Override
    public void searchDocumentsByPath(String path, CallbackPrx callback, Current current) {
        run(path, callback);
    }

    private int run(String path, CallbackPrx callbackPrx) {
        taskManager.createTasks(path);
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
            long startTime = System.currentTimeMillis();
            if(taskManager.getRemainingTasks()==1){
                privateWorker.launch(taskManager);
            }else{
                while (taskManager.getRemainingTasks()>0) {
                    generalWorker.launch(taskManager);
                }
            }
            long endTime = System.currentTimeMillis();
            long totalTime = endTime-startTime;
            callbackPrx.reportResponse(new Response(totalTime ,taskManager.getResult()));
            taskManager.shutdown();
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
