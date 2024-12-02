import com.zeroc.Ice.Current;

import RegistryModule.Task;

//Servant Class
public class TaskManagerImpl implements RegistryModule.TaskManager {

    private String taskName = "Simple Task";

    @Override
    public void addPartialResult(String result, Current current) {
        System.out.println("Partial result added: " + result);
    }

    @Override
    public Task getTask(Current current) {
        Task task = new Task();
        task.hostname = taskName;
        return task;
    }

    public TaskManagerImpl(String taskName) {
        this.taskName = taskName;
    }

}
