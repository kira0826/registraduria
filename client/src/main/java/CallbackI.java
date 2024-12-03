import com.zeroc.Ice.Current;

import RegistryModule.Callback;
import RegistryModule.Response;

public class CallbackI implements Callback {
    @Override
    public void reportResponse(Response response, Current current) {
        System.out.println("Respuest de la consulta: \n" + response.responseTime);
        System.out.println("Respuesta de la consulta: \n" + response.value);
    }
}
