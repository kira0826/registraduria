import com.zeroc.Ice.*;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.NoSuchTopic;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

public class ConsultantAuxiliar {

    private final String workerId;
    private final String masterId;

    public ConsultantAuxiliar(String workerId, String masterId) {
        this.workerId = workerId;
        this.masterId = masterId;
    }

    public static void main(String[] args) {

        for (String arg : args) {
            System.out.println("arg " + arg);
        }

        try (com.zeroc.Ice.Communicator communicator = initializeCommunicator(args)) {
            // Obtener IDs desde argumentos o propiedades
            String workerId = getProperty(communicator, "Worker.ID", "worker1");
            String masterId = getProperty(communicator, "Master.ID", "master1");

            System.out.println("Worker ID: " + workerId);
            System.out.println("Master ID: " + masterId);

            ConsultantAuxiliar consultant = new ConsultantAuxiliar(workerId, masterId);
            Thread destroyHook = new Thread(() -> communicator.destroy());
            Runtime.getRuntime().addShutdownHook(destroyHook);
            // int status = consultant.run(communicator, destroyHook);
            int status = consultant.run(communicator);
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

    private static com.zeroc.Ice.Communicator initializeCommunicator(String[] args) {
        try {
            // Inicializar propiedades
            InitializationData initData = new InitializationData();
            initData.properties = Util.createProperties();

            // Cargar archivo de configuración desde el JAR
            initData.properties.load("config.sub");

            // Permitir sobrescribir propiedades desde línea de comandos
            args = initData.properties.parseCommandLineOptions("Worker", args);

            return Util.initialize(args, initData);
        } catch (LocalException e) {
            System.err.println("Error initializing Ice: " + e.getMessage());
            System.exit(1);
            return null;
        }

        // java.util.List<String> extraArgs = new java.util.ArrayList<String>();
        // try (com.zeroc.Ice.Communicator communicator =
        // com.zeroc.Ice.Util.initialize(args, "config.sub", extraArgs)) {
        // // Destroy communicator during JVM shutdown

        // return communicator;
        // } catch (LocalException e) {
        // e.printStackTrace();
        // return null;
        // }
    }

    private int run(com.zeroc.Ice.Communicator communicator) {
        try {
            TopicManagerPrx manager = TopicManagerPrx.checkedCast(
                    communicator.propertyToProxy("TopicManager.Proxy"));

            // Crear/obtener topic específico master-worker
            String privateTopicName = String.format("master.%s.%s", masterId, workerId);
            TopicPrx privateTopic = getOrCreateTopic(manager, privateTopicName);

            // Crear/obtener topic general
            String generalTopicName = "general.tasks";
            TopicPrx generalTopic = getOrCreateTopic(manager, generalTopicName);

            // Crear el adapter y el servant
            ObjectAdapter adapter = communicator.createObjectAdapter("Worker.Subscriber");
            ConsultantAuxiliarManagerImpl servant = new ConsultantAuxiliarManagerImpl();

            // Suscribirse al topic privado
            Identity privateId = new Identity(workerId + ".private", "worker");
            ObjectPrx privateSubscriber = adapter.add(servant, privateId);
            subscribeToTopic(privateTopic, privateSubscriber, true); // QoS ordenado para mensajes privados

            // Suscribirse al topic general
            Identity generalId = new Identity(workerId + ".general", "worker");
            ObjectPrx generalSubscriber = adapter.add(servant, generalId);
            subscribeToTopic(generalTopic, generalSubscriber, false); // QoS estándar para mensajes generales

            adapter.activate();

            // Configurar shutdown hooks para ambas suscripciones
            setupShutdownHook(privateTopic, privateSubscriber, generalTopic, generalSubscriber, communicator);

            communicator.waitForShutdown();
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
                } catch (NoSuchTopic e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    private void subscribeToTopic(TopicPrx topic, ObjectPrx subscriber, boolean ordered) {
        java.util.Map<String, String> qos = new java.util.HashMap<>();
        if (ordered) {
            qos.put("reliability", "ordered");
        }
        try {
            topic.subscribeAndGetPublisher(qos, subscriber);
        } catch (AlreadySubscribed e) {
            e.printStackTrace();
            System.out.println("reactivating persistent subscriber");
            return;
        } catch (InvalidSubscriber e) {
            e.printStackTrace();
            return;
        } catch (BadQoS e) {
            e.printStackTrace();
            return;
        }
    }

    private void setupShutdownHook(TopicPrx privateTopic, ObjectPrx privateSubscriber,
            TopicPrx generalTopic, ObjectPrx generalSubscriber,
            Communicator communicator) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                privateTopic.unsubscribe(privateSubscriber);
                generalTopic.unsubscribe(generalSubscriber);
            } finally {
                communicator.destroy();
            }
        }));
    }

    // private int run(com.zeroc.Ice.Communicator communicator, Thread destroyHook)
    // {

    // System.out.println("Running the worker");
    // System.out.println("Inside run Worker ID: " + workerId);
    // System.out.println("Inside run Master ID: " + masterId);

    // try {
    // // Obtener el TopicManager
    // TopicManagerPrx manager = TopicManagerPrx.checkedCast(
    // communicator.propertyToProxy("TopicManager.Proxy"));
    // if (manager == null) {
    // System.err.println("Invalid proxy");
    // return 1;
    // }

    // // Obtener o crear el topic
    // String topicName = "master";
    // TopicPrx topic;
    // try {
    // topic = manager.retrieve(topicName);
    // } catch (com.zeroc.IceStorm.NoSuchTopic e) {
    // try {
    // topic = manager.create(topicName);
    // } catch (com.zeroc.IceStorm.TopicExists ex) {
    // System.err.println("Topic exists, try again.");
    // return 1;
    // }
    // }

    // // Crear el adapter y añadir el servant
    // ObjectAdapter adapter =
    // communicator.createObjectAdapter("Auxiliar.Subscriber");
    // Identity subscriberId = new Identity(
    // java.util.UUID.randomUUID().toString(), "");
    // ObjectPrx subscriber = adapter.add(new ConsultantAuxiliarManagerImpl(),
    // subscriberId);

    // // Activar el adapter
    // adapter.activate();

    // // Suscribirse al topic
    // java.util.Map<String, String> qos = new java.util.HashMap<>();
    // topic.subscribeAndGetPublisher(qos, subscriber);

    // // Configurar el shutdown hook para desuscribirse
    // final com.zeroc.IceStorm.TopicPrx topicF = topic;
    // final com.zeroc.Ice.ObjectPrx subscriberF = subscriber;
    // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // try {
    // topicF.unsubscribe(subscriberF);
    // } finally {
    // communicator.destroy();
    // }
    // }));
    // Runtime.getRuntime().removeShutdownHook(destroyHook); // remove old
    // destroy-only shutdown hook
    // communicator.waitForShutdown();
    // return 0;

    // // Esperar hasta que la aplicación se cierre
    // } catch (AlreadySubscribed e) {
    // e.printStackTrace();
    // System.out.println("reactivating persistent subscriber");
    // return 1;
    // } catch (InvalidSubscriber e) {
    // e.printStackTrace();
    // return 1;
    // } catch (BadQoS e) {
    // e.printStackTrace();
    // return 1;
    // }
    // }
}