import RegistryModule.Callback;
import RegistryModule.Response;
import com.zeroc.Ice.Current;

public class CallbackI implements Callback {
    @Override
    public void reportResponse(Response response, Current current) {
        System.out.println("Tiempo de respuesta: "+response.responseTime);
    }
}
