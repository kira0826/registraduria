import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class Main {

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("RegistryAdapter", "default -p 10000");

            PerformQueryImpl performQueryService = new PerformQueryImpl(DatabaseConfig.getDataSource());

            adapter.add(performQueryService, Util.stringToIdentity("PerformQuery"));

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