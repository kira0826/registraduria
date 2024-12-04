
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.InitializationData;
import com.zeroc.Ice.LocalException;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import RegistryModule.CallbackPrx;
import RegistryModule.ConsultantServiceManagerPrx;
import utils.NetworkUtils;

public class Client {

    private static final Scanner sc = new Scanner(System.in);

    // Clase contenedora para la bandera de ejecución
    private static class RunFlag {  

        volatile boolean isRunning = true;
    }

    public static void main(String[] args) {
        System.out.println("Iniciando el cliente...");
        int status = 0;
        List<String> extraArgs = new ArrayList<>();

        try (Communicator communicator = initializeCommunicator(args)) {
            System.out.println("Ice communicator inicializado.");
            if (!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                status = run(communicator);
            }
        } catch (Exception e) {
            System.err.println("Excepción en main: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        }
        System.exit(status);
    }

    private static int run(Communicator communicator) {
        System.out.println("Ejecutando cliente...");

        RunFlag runFlag = new RunFlag(); // Instancia de la bandera de ejecución

        try {

            //print properties 
            System.out.println("Properties: " + communicator.getProperties().getPropertiesForPrefix("").entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));

            ObjectAdapter adapter = communicator.createObjectAdapter("CallBack");

            RegistryModule.Callback callbackPrx = new CallbackI();

            ObjectPrx callbackProxy = adapter.add(callbackPrx, Util.stringToIdentity("CallBack"));
            CallbackPrx callback = CallbackPrx.checkedCast(callbackProxy);

            adapter.activate();

            ConsultantServiceManagerPrx consultantServiceManager = null;

            com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrx
                    .checkedCast(communicator.stringToProxy("registryConsultantClient/Query"));

            consultantServiceManager = ConsultantServiceManagerPrx
                    .checkedCast(query.findObjectByType("::RegistryModule::ConsultantServiceManager"));

            if (consultantServiceManager == null) {
                System.err.println("couldn't find a `::RegistryModule::ConsultantServiceManager' object");
                return 1;
            }
            System.out.println("ConsultantServiceManager obtenido correctamente.");

            // ------------------------------------------------------------------------------
            Thread destroyHook = new Thread(() -> communicator.destroy());
            Runtime.getRuntime().addShutdownHook(destroyHook);

            // Ejecutar el bucle mientras la bandera esté activa
            while (runFlag.isRunning) {
                String filePath = getFilePath(runFlag);
                if (filePath != null) { // Solo si se obtuvo una ruta válida
                    consultantServiceManager.searchDocumentsByPath(filePath, callback);
                }
            }

            // Opcional: Realizar limpieza adicional aquí si es necesario
            communicator.waitForShutdown();

            return 0;
        } catch (Exception e) {
            System.err.println("Excepción en run: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private static String getFilePath(RunFlag runFlag) {
        System.out.println("Bienvenido al sistema de votaciones");
        String filePath = null;

        System.out.println("Después del file path");

        while (true) {
            System.out.println("Ingresa la ruta del archivo para iniciar consulta o escribe 'exit' para salir:");
            String input = sc.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Saliendo del sistema...");
                runFlag.isRunning = false; // Cambiar la bandera para salir del bucle
                return null; // Retornar null para no realizar ninguna acción adicional
            } else if (isValidFilePath(input)) {
                filePath = input;
                System.out.println("Ruta válida ingresada: " + filePath);
                break;
            } else {
                System.out.println("La ruta ingresada no es válida. Intenta nuevamente.");
            }
        }
        return filePath;
    }

    private static com.zeroc.Ice.Communicator initializeCommunicator(String[] args) {
        try {
            // Inicializar propiedades
            InitializationData initData = new InitializationData();
            initData.properties = Util.createProperties();

            // Cargar archivo de configuración desde el JAR
            initData.properties.load("config.client");

            String ip = "";
            try {
                ip = NetworkUtils.getLocalIPAddress();
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("Current IP: " + ip);

            initData.properties.setProperty("CallBack.Endpoints", "tcp -h " + ip + " -p 32500");

            return Util.initialize(args, initData);
        } catch (LocalException e) {
            System.err.println("Error initializing Ice: " + e.getMessage());
            System.exit(1);
            return null;
        }

    }

    private static boolean isValidFilePath(String path) {
        return path.matches("^[\\w\\-.\\/\\\\]+\\.[a-zA-Z]{2,5}$");
    }
}
