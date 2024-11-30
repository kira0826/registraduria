import com.zeroc.Ice.Current;
import com.zeroc.Ice.Value;

import RegistryModule.CallbackPrx;

public class PerformQueryImpl implements RegistryModule.PerformQuery {

    @Override
    public Value connection(String hostname, Current current) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connection'");
    }

    @Override
    public String makeQuery(int[] ids, Current current) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'makequery'");
    }

    @Override
    public String executeQuery(String query, Current current) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeQuery'");
    }

    @Override
    public void receiveMessage(int[] ids, CallbackPrx callback, Current current) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveMessage'");
    }
    
}
