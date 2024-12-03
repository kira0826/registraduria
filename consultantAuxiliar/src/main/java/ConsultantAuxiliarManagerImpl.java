import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zeroc.Ice.Current;

import RegistryModule.ConsultantAuxiliarManager;
import RegistryModule.PerformQueryPrx;
import RegistryModule.Task;
import RegistryModule.TaskManagerPrx;

public class ConsultantAuxiliarManagerImpl implements ConsultantAuxiliarManager {



    private MathPrimes mathPrimes = new MathPrimes(1000000000);

    PerformQueryPrx performQuery;

    public ConsultantAuxiliarManagerImpl(PerformQueryPrx performQuery) {
        this.performQuery = performQuery;
    }

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            4,
            8,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    @Override
    public void shutdown(Current current) {
        // Implementar logia para verificar a que topics estoy suscrito y mirar si puedo
        // apagarme
        System.out.println("Shutting down the worker");
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    public void setPoolSize(int n, Current current) {
        System.out.println("Adjusting ThreadPool size: coreSize=" + n + ", maxSize=" + n);
        executor.setCorePoolSize(n);
        executor.setMaximumPoolSize(n);

    }

    @Override
    public void launch(TaskManagerPrx taskManager, Current current) {
        // Implementar logica para lanzar el worker y empezar a pedir tareas
        executor.submit(() -> {
            try {
                Task task = taskManager.getTask();
                if (task != null) {
                    String result = processTask(task, taskManager);
                    System.out.println("Task processed: " + result);                    
                } else {
                    System.out.println("No task available for this worker.");
                }
            } catch (Exception e) {
                System.err.println("Error processing task: " + e.getMessage());
            }
        });
    }


    private String processTask(Task task,  TaskManagerPrx taskManager) {

    
        taskManager.addPartialResult(Arrays.asList(task.ids).toString(), task.id);

        performQuery.receiveMessage( task.ids, taskManager, task.id);

        performQuery.receiveMessage( task.ids, taskManager, task.id);
            
        return "Tarea procesada"; 
    }

}
