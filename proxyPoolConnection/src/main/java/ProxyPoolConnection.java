import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Properties;
import com.zeroc.Ice.Util;

public class ProxyPoolConnection {

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {

            ObjectAdapter adapter = communicator.createObjectAdapter("ProxyPoolConnectionAdapter");
            Properties properties = communicator.getProperties();
            Identity identity = Util.stringToIdentity(properties.getProperty("Identity"));
            adapter.add(new PerformQueryImpl(DatabaseConfig.getDataSource()), identity);            
            adapter.activate();
   
            System.out.println("Servidor ProxyPoolConnection ejecutándose...");

            communicator.waitForShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.close();
        }
    }
}