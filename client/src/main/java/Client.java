import RegistryModule.CallbackPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Exception;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            List<String> extraArgs = new ArrayList<>();
            Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config.client", extraArgs);


            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("Callback","tcp -h 0.0.0.0");
            com.zeroc.Ice.Object clientCallback = new CallbackI();
            ObjectPrx callbackProxy = adapter.addWithUUID(clientCallback);
            CallbackPrx callback = CallbackPrx.checkedCast(callbackProxy);
            adapter.activate();

            System.out.println("Bienvenido al sistema de votaciones");

            int n = -1;
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

            while (true) {
                System.out.println("Ingresa la ruta del archivo para iniciar consulta o escribe 'exit' para salir:");
                String input = sc.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Saliendo del sistema...");
                    break;
                } else if (isValidFilePath(input)) {
                    System.out.println("Ruta válida ingresada: " + input);
                } else {
                    System.out.println("La ruta ingresada no es válida. Intenta nuevamente.");
                }
            }

            communicator.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    private static boolean isValidFilePath(String path) {
        return path.matches("^[\\w\\-.\\/\\\\]+\\.[a-zA-Z]{2,5}$");
    }
}
