import RegistryModule.CallbackPrx;
import RegistryModule.ConsultantServiceManagerPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Exception;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        int status = 0;
        List<String> extraArgs = new ArrayList<>();
        try (Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config.client", extraArgs))
        {
            if(!extraArgs.isEmpty())
            {
                System.err.println("too many arguments");
                status = 1;
            }
            else {
                status = run(communicator);
            }
        }
        System.exit(status);
    }

    private static int run(Communicator communicator) {

        ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("Callback", "tcp -h 0.0.0.0");
        com.zeroc.Ice.Object clientCallback = new CallbackI();
        ObjectPrx callbackProxy = adapter.addWithUUID(clientCallback);
        CallbackPrx callback = CallbackPrx.checkedCast(callbackProxy);
        adapter.activate();

        ConsultantServiceManagerPrx consultantServiceManager = null;

        com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrx.checkedCast(communicator.stringToProxy("registryConsultantClient/Query"));
        consultantServiceManager = ConsultantServiceManagerPrx.checkedCast(query.findObjectByType("::RegistryModule::ConsultantServiceManager"));

        if(consultantServiceManager == null)
        {
            System.err.println("couldn't find a `::RegistryModule::ConsultantServiceManager' object");
            return 1;
        }


        int n = getThreadPoolSize();
        String filePath = getFilePath();

        consultantServiceManager.setPoolsize(n);
        consultantServiceManager.searchDocumentsByPath(filePath,callback);


        return 0;
    }

    private static int getThreadPoolSize() {
        int n = -1;

        System.out.println("Bienvenido al sistema de votaciones");

        while (true) {
            System.out.println("Ingresa el N para el thread pool (debe ser un número entero positivo):");
            String input = sc.nextLine().trim();

            try {
                n = Integer.parseInt(input);
                if (n > 0) {
                    break;
                } else {
                    System.out.println("El número debe ser positivo. Intenta nuevamente.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingresa un número entero.");
            }
        }
        return n;
    }

    private static String getFilePath() {
        String filePath = null;

        while (true) {
            System.out.println("Ingresa la ruta del archivo para iniciar consulta o escribe 'exit' para salir:");
            String input = sc.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Saliendo del sistema...");
                System.exit(0);
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

    private static boolean isValidFilePath(String path) {
        return path.matches("^[\\w\\-.\\/\\\\]+\\.[a-zA-Z]{2,5}$");
    }
}