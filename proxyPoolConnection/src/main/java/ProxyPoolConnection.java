import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Properties;
import com.zeroc.Ice.Util;

public class ProxyPoolConnection {

    public static void main(String[] args) {

        System.out.println("Ola");
        
        try (Communicator communicator = Util.initialize(args)) {
            // Imprimir las propiedades recibidas
            Properties properties = communicator.getProperties();

            
            System.out.println("Propiedades recibidas:");
            for (String key : properties.getPropertiesForPrefix("").keySet()) {
                System.out.println(key + " = " + properties.getProperty(key));
            }

            // Crear el adaptador
            ObjectAdapter adapter = communicator.createObjectAdapter("ProxyPoolConnectionAdapter");

            // Obtener la identidad desde las propiedades
            String identityString = properties.getProperty("Identity");
            if (identityString == null || identityString.isEmpty()) {
                System.err.println("La propiedad 'Identity' no está definida.");
                return;
            }
            Identity identity = Util.stringToIdentity(identityString);

            // Agregar el objeto al adaptador
            adapter.add(new PerformQueryImpl(DatabaseConfig.getDataSource()), identity);
            adapter.activate();

            System.out.println("Servidor ProxyPoolConnection ejecutándose...");

            communicator.waitForShutdown();

            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //DatabaseConfig.close();
            System.out.println("Conexión cerrada");
        }
             

    }
}
