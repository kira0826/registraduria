import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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

    List<String> cedulasList = new ArrayList<>();
    ConcurrentLinkedQueue<String[]> taskQueue = new ConcurrentLinkedQueue<>();
    private final Set<Task> inProgressTasks = ConcurrentHashMap.newKeySet();
    private final Set<Task> completedTasks = ConcurrentHashMap.newKeySet();
    private final int TASK_TIMEOUT_MILISECONDS = 2000;
    private String result = "";

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
    public void addPartialResult(String result, String taskId,Current current) {
        inProgressTasks.stream()
                .filter(task -> task.id.equals(taskId))
                .findFirst()
                .ifPresent(task -> {
                    lock.lock();
                    try {
                        concurrentString.append(result).append("\n");
                    } finally {
                        lock.unlock();
                    }
                    inProgressTasks.remove(task);
                    completedTasks.add(task);

                    System.out.println("Partial result added for task: " + taskId);
                });
    }

    @Override
    public synchronized Task getTask(Current current) {
        String[] task = taskQueue.poll();
        if (task != null) {
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
        ClassLoader classLoader = TaskManagerImpl.class.getClassLoader();
        try(InputStream inputStream = classLoader.getResourceAsStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            if(inputStream == null) {
                System.out.println("Archivo no encontrado");
                return;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                cedulasList.add(line.trim());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void validateTasks() {
        if (cedulasList.size() > maxCapacity) {
            int fromIndex = 0;
            while (fromIndex < cedulasList.size()) {
                int toIndex = Math.min(fromIndex + maxCapacity, cedulasList.size());
                String[] subArray = cedulasList.subList(fromIndex, toIndex).toArray(new String[0]);
                taskQueue.add(subArray);
                fromIndex = toIndex;
            }
        } else {
            String[] subArray = cedulasList.toArray(new String[0]);
            taskQueue.add(subArray);
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

    public void setPath(String path) {
        this.path = path;
    }
}
