import java.util.stream.Collectors;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.LocalException;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

import RegistryModule.CallbackPrx;
import RegistryModule.ConsultantAuxiliarManagerPrx;
import RegistryModule.Response;
import RegistryModule.TaskManagerPrx;

public class ConsultantServiceManagerImpl implements RegistryModule.ConsultantServiceManager {

    public ConsultantServiceManagerImpl(Communicator communicator, TaskManagerPrx taskManager, String masterId) {
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
            System.out.println("Se obtuvo el topicManager DE ICEstor");
            if (manager == null) {
                System.out.println("NO se encontro topic manager");
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
            System.out.println("Obtener general topic");
            // Obtener publishers para ambos topics
            ConsultantAuxiliarManagerPrx privateWorker = ConsultantAuxiliarManagerPrx.uncheckedCast(
                    privateTopic.getPublisher().ice_oneway());
            System.out.println("Obtener worker privado");
            ConsultantAuxiliarManagerPrx generalWorker = ConsultantAuxiliarManagerPrx.uncheckedCast(
                    generalTopic.getPublisher().ice_oneway());
            generalWorker.setPoolSize(poolSize);
            System.out.println("Obtener workers generales");
            System.out.println("Publishing events. Press ^C to terminate the application.");
            long startTime = System.currentTimeMillis();
            if (taskManager.getRemainingTasks() == 1) {
                System.out.println("Usando el private worker");
                privateWorker.launch(taskManager);
                System.out.println("Solo hay una tarea");
            } else {
                System.out.println("Hay varias tareas");
                while (taskManager.getRemainingTasks() > 0) {
                    System.out.println("Usando el general worker");
                    generalWorker.launch(taskManager);
                }
            }
            System.out.println("Tareas lanzadas esperar completitud");
            while (!taskManager.isCompleted()) {
            }
            System.out.println("Se terminaron todas las tareas");
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println("Apunto de enviar el callback");
            callbackPrx.reportResponse(new Response(totalTime, taskManager.getResult()));
            System.out.println("Enviado callback");
            taskManager.shutdown();
            System.out.println("Apagar taskmanager");
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
