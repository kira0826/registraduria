import RegistryModule.ConsultantAuxiliarManagerPrx;

public class ConsultantServer {
    public static void main(String[] args) {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<String>();

        // Try with resources block - communicator is automatically destroyed
        // at the end of this try block
        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config/config.pub",
                extraArgs)) {
            // Set the package for generated classes
            communicator.getProperties().setProperty("Ice.Default.Package", "Demo");

            // Create an object adapter and start the master server logic
            com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("TaskManager");
            adapter.add(new TaskManagerImpl(), com.zeroc.Ice.Util.stringToIdentity("SimpleTaskManager"));
            adapter.activate();

            // Install shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));
            // Activate the publisher logic
            status = run(communicator, extraArgs.toArray(new String[extraArgs.size()]));
            communicator.waitForShutdown();
        }
        System.exit(status);
    }

    // ------------------ pub logic ------------------

    public static void usage() {
        System.out.println("Usage: [--datagram|--twoway|--oneway] [topic]");
    }

    private static int run(com.zeroc.Ice.Communicator communicator, String[] args) {
        String option = "None";
        String topicName = "master";
        int i;

        for (i = 0; i < args.length; ++i) {
            String oldoption = option;
            if (args[i].equals("--datagram")) {
                option = "Datagram";
            } else if (args[i].equals("--twoway")) {
                option = "Twoway";
            } else if (args[i].equals("--oneway")) {
                option = "Oneway";
            } else if (args[i].startsWith("--")) {
                usage();
                return 1;
            } else {
                topicName = args[i++];
                break;
            }

            if (!oldoption.equals(option) && !oldoption.equals("None")) {
                usage();
                return 1;
            }
        }

        if (i != args.length) {
            usage();
            return 1;
        }

        com.zeroc.IceStorm.TopicManagerPrx manager = com.zeroc.IceStorm.TopicManagerPrx.checkedCast(
                communicator.propertyToProxy("TopicManager.Proxy"));
        if (manager == null) {
            System.err.println("invalid proxy");
            return 1;
        }

        //
        // Retrieve the topic.
        //
        com.zeroc.IceStorm.TopicPrx topic;
        try {
            topic = manager.retrieve(topicName);
        } catch (com.zeroc.IceStorm.NoSuchTopic e) {
            try {
                topic = manager.create(topicName);
            } catch (com.zeroc.IceStorm.TopicExists ex) {
                System.err.println("temporary failure, try again.");
                return 1;
            }
        }

        //
        // Get the topic's publisher object, and create a Clock proxy with
        // the mode specified as an argument of this application.
        //
        com.zeroc.Ice.ObjectPrx publisher = topic.getPublisher();
        if (option.equals("Datagram")) {
            publisher = publisher.ice_datagram();
        } else if (option.equals("Twoway")) {
            // Do nothing.
        } else // if(oneway)
        {
            publisher = publisher.ice_oneway();
        }
        ConsultantAuxiliarManagerPrx worker = ConsultantAuxiliarManagerPrx.uncheckedCast(publisher);

        System.out.println("publishing start events. Press ^C to terminate the application.");
        try {

            while (true) {

                worker.launch();

                try {
                    Thread.currentThread();
                    Thread.sleep(1000);
                } catch (java.lang.InterruptedException e) {
                }
            }
        } catch (com.zeroc.Ice.CommunicatorDestroyedException ex) {
            // Ctrl-C triggered shutdown hook, which destroyed communicator - we're
            // terminating
        }

        return 0;
    }
}
