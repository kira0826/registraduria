import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.zeroc.Ice.Current;

import RegistryModule.Task;
import RegistryModule.TaskManager;

//Servant Class
public class TaskManagerImpl implements TaskManager {

    private int totalTask;
    List<String> cedulasList = new ArrayList<>();
    ConcurrentLinkedQueue<String[]> taskQueue = new ConcurrentLinkedQueue<>();
    private final Set<Task> inProgressTasks = ConcurrentHashMap.newKeySet();
    private final Set<Task> completedTasks = ConcurrentHashMap.newKeySet();
    private final int TASK_TIMEOUT_MILISECONDS = 2000;
    private final ConcurrentHashMap<String, String> partialResults = new ConcurrentHashMap<>();
    private final StringBuilder concurrentString = new StringBuilder();
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService supervisor = Executors.newScheduledThreadPool(1);

    private String path;
    final int maxCapacity = 250;

    @Override
    public void createTasks(String path, Current current) {
        setPath(path);
        readPath();
        validateTasks();
        startSupervisor();
    }

    @Override
    public void addPartialResult(Map<String, String> result, String taskId, Current current) {


        System.out.println("Adding partial result for task: " + taskId);
        System.out.println("Result: " + result);
        inProgressTasks.stream()
                .filter(task -> task.id.equals(taskId))
                .findFirst()
                .ifPresent(task -> {
                    String document = result.keySet().iterator().next();
                    if(partialResults.containsKey(document)) {
                        lock.lock();
                        try {
                            if(result.get(document).length()==1){
                                concurrentString
                                        .append(partialResults.get(document))
                                        .append(" ")
                                        .append(result.get(document))
                                        .append("\n");
                            }else {
                                concurrentString
                                        .append(result.get(document))
                                        .append(" ")
                                        .append(partialResults.get(document))
                                        .append("\n");
                            }
                        } finally {
                            lock.unlock();
                        }
                        inProgressTasks.remove(task);
                        completedTasks.add(task);

                    } else {
                        partialResults.put(document, result.get(document));
                    }
                    System.out.println("Partial result added for task: " + taskId);
                });
    }

    @Override
    public synchronized Task getTask(Current current) {
        String[] task = taskQueue.poll();
        if(task != null) {
            Task tsk = new Task(task, UUID.randomUUID().toString());
            inProgressTasks.add(tsk);
            return tsk;
        }
        System.out.println(taskQueue.size());
        return null;
    }

    @Override
    public void shutdown(Current current) {
        supervisor.shutdown();
        lock.lock();
        try {
            concurrentString.setLength(0);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getResult(Current current) {
        lock.lock();
        try {
            return concurrentString.toString();
        } finally {
            lock.unlock();
        }
    }

    private void readPath() {
        if (path == null || path.isEmpty()) {
            System.err.println("Error: La ruta del archivo no está definida");
            return;
        }
    
        File file = new File(path);
        
        // Si la ruta no es absoluta, intentamos resolver relativa al directorio de trabajo
        if (!file.isAbsolute()) {
            String workingDir = System.getProperty("user.dir");
            file = new File(workingDir, path);
        }
    
        System.out.println("Intentando leer archivo desde: " + file.getAbsolutePath());
        
        if (!file.exists()) {
            System.err.println("Error: El archivo no existe en la ruta: " + file.getAbsolutePath());
            return;
        }
    
        if (!file.canRead()) {
            System.err.println("Error: No hay permisos de lectura para el archivo: " + file.getAbsolutePath());
            return;
        }
    
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {  // Ignoramos líneas vacías
                    cedulasList.add(line.trim());
                }
            }
            System.out.println("Se cargaron " + cedulasList.size() + " cédulas del archivo");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + file.getAbsolutePath());
            System.err.println("Mensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateTasks() {
        if(cedulasList.size() > maxCapacity) {
            int fromIndex = 0;
            while (fromIndex < cedulasList.size()) {
                int toIndex = Math.min(fromIndex + maxCapacity, cedulasList.size());
                String[] subArray = cedulasList.subList(fromIndex, toIndex).toArray(new String[0]);
                taskQueue.add(subArray);
                fromIndex = toIndex;
            }
            totalTask = taskQueue.size();
        } else {
            String[] subArray = cedulasList.toArray(new String[0]);
            taskQueue.add(subArray);
            totalTask = 1;
        }
    }

    private void startSupervisor() {
        supervisor.scheduleAtFixedRate(() -> {
            System.out.println("Revisando tareas");
            List<Task> toReassign = inProgressTasks.stream()
                    .filter(task -> !completedTasks.contains(task))
                    .collect(Collectors.toList());
            for (Task task : toReassign) {
                taskQueue.add(task.ids);
                inProgressTasks.remove(task);
                System.out.println("Reassigning task: " + task.id);
            }
        }, TASK_TIMEOUT_MILISECONDS, TASK_TIMEOUT_MILISECONDS, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized int getRemainingTasks(Current current) {
        return taskQueue.size();
    }

    @Override
    public synchronized boolean isCompleted(Current current){
        return completedTasks.size() == totalTask;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
