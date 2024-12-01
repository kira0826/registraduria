import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Exception;

public class Client {

    public static void main(String[] args) {
        try {
            Communicator communicator = com.zeroc.Ice.Util.initialize(args);

            communicator.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
