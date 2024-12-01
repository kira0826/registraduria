import com.zeroc.Ice.Current;

import RegistryModule.Task;

//Servant Class
public class TaskManagerImpl implements RegistryModule.TaskManager {

    @Override
    public void addPartialResult(String result, Current current) {
        System.out.println("Partial result added: " + result);
    }

    @Override
    public Task getTask(Current current) {
        return new Task();
    }

}
