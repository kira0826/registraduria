import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;

import RegistryModule.*;

public class Client {

    public static void main(String[] args) {
        try {
            Communicator communicator = com.zeroc.Ice.Util.initialize(args);

            ObjectPrx base = communicator.stringToProxy("PerformQuery:default -p 10000");

            PerformQueryPrx performQuery = PerformQueryPrx.checkedCast(base);
            if (performQuery == null) {
                throw new RuntimeException("Invalid proxy");
            }

            String[] ids = {"322832298", "317361084", "13707037"};

            String query = performQuery.makeQuery(ids, null);

            String result = performQuery.executeQuery(query, null);
            System.out.println("Query Result:\n" + result);

            communicator.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
