import java.util.stream.Collectors;

import com.zeroc.Ice.*;
import com.zeroc.IceGrid.QueryPrx;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.NoSuchTopic;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

import RegistryModule.ConsultantAuxiliarManager;
import RegistryModule.PerformQueryPrx;

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

            ObjectPrx obj = communicator.stringToProxy("IceGrid/Query");
            QueryPrx query = QueryPrx.checkedCast(obj);

            if (query == null) {
                throw new Error("Invalid proxy");
            }

            // Buscar el servicio PerformQuery a través de IceGrid
            // Usamos el tipo completo como está definido en el XML de IceGrid
            ObjectPrx base = query.findObjectByType("::RegistryModule::PerformQuery");
            if (base == null) {
                throw new Error("No PerformQuery service found");
            }

            // Convertir el proxy genérico a PerformQueryPrx
            PerformQueryPrx performQuery = PerformQueryPrx.checkedCast(base);
            if (performQuery == null) {
                throw new Error("Invalid PerformQuery proxy");
            }

            // ------------------------------------------------------------------------------
            Thread destroyHook = new Thread(() -> communicator.destroy());
            Runtime.getRuntime().addShutdownHook(destroyHook);
            int status = consultant.run(communicator, destroyHook, performQuery);
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
            args = initData.properties.parseCommandLineOptions("Auxiliar", args);

            return Util.initialize(args, initData);
        } catch (LocalException e) {
            System.err.println("Error initializing Ice: " + e.getMessage());
            System.exit(1);
            return null;
        }

    }

    private int run(com.zeroc.Ice.Communicator communicator, Thread destroyHook, PerformQueryPrx performQuery) {

        System.out.println("Properties: " + communicator.getProperties().getPropertiesForPrefix("").entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));

        try {
            TopicManagerPrx manager = TopicManagerPrx.checkedCast(
                    communicator.propertyToProxy("TopicManager.Proxy"));

            // Crear/obtener topic específico master-worker
            String privateTopicName = String.format("%s.%s", masterId, workerId);
            System.out.println("Private topic: " + privateTopicName);
            TopicPrx privateTopic = getOrCreateTopic(manager, privateTopicName);

            // Crear/obtener topic general
            String generalTopicName = "general.tasks";
            System.out.println("General topic: " + generalTopicName);
            TopicPrx generalTopic = getOrCreateTopic(manager, generalTopicName);

            // Crear el adapter y el servant
            ObjectAdapter adapter = communicator.createObjectAdapter("Auxiliar.Subscriber");
            ConsultantAuxiliarManager servant = new ConsultantAuxiliarManagerImpl(performQuery);

            // Suscribirse al topic privado
            Identity privateId = new Identity(workerId + ".private" + java.util.UUID.randomUUID().toString(), "worker");
            if (privateId.name == null) {
                privateId.name = java.util.UUID.randomUUID().toString();
            }
            ObjectPrx privateSubscriber = adapter.add(servant, privateId);
            subscribeToTopic(privateTopic, privateSubscriber, true); // QoS ordenado para mensajes privados

            // Suscribirse al topic general
            Identity generalId = new Identity(workerId + ".general" + java.util.UUID.randomUUID().toString(), "worker");
            if (generalId.name == null) {
                generalId.name = java.util.UUID.randomUUID().toString();
            }
            ObjectPrx generalSubscriber = adapter.add(servant, generalId);
            subscribeToTopic(generalTopic, generalSubscriber, false); // QoS estándar para mensajes generales

            adapter.activate();

            // Configurar shutdown hooks para ambas suscripciones
            setupShutdownHook(privateTopic, privateSubscriber, generalTopic, generalSubscriber, communicator,
                    destroyHook);

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
            Communicator communicator, Thread destroyHook) {

        final com.zeroc.IceStorm.TopicPrx topicF = privateTopic;
        final com.zeroc.IceStorm.TopicPrx topicGenF = privateTopic;
        final com.zeroc.Ice.ObjectPrx subscriberF = generalTopic;
        final com.zeroc.Ice.ObjectPrx subscriberGenF = generalSubscriber;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                topicF.unsubscribe(subscriberF);
                topicGenF.unsubscribe(subscriberGenF);
            } finally {
                communicator.destroy();
            }
        }));
        Runtime.getRuntime().removeShutdownHook(destroyHook);
    }

}