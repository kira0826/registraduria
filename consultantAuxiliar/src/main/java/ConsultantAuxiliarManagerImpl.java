import com.zeroc.Ice.Current;

import RegistryModule.TaskManagerPrx;

public class ConsultantAuxiliarManagerImpl implements RegistryModule.ConsultantAuxiliarManager {

    private TaskManagerPrx taskManager;

    @Override
    public void shutdown(Current current) {

        // Implementar logia para verificar a que topics estoy suscrito y mirar si puedo
        // apagarme
        System.out.println("Shutting down the worker");
    }

    @Override
    public void launch(Current current) {

        // Implementar logica para lanzar el worker y empezar a pedir tareas
        System.out.println("Launching the worker with task manager task: " + taskManager.getTask().hostname);

    }

    public ConsultantAuxiliarManagerImpl(TaskManagerPrx taskManager) {
        this.taskManager = taskManager;
    }

}
