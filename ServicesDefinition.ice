module RegistryModule
{


    sequence<string> StringSeq;

    dictionary<string, string> Result;

    class Response{
        long responseTime;
        string value;
    }


    interface Callback {
        void reportResponse(Response response);
    }   

    //Client interfaces

    interface Testeable {

        void runTests(string path); 

    }

      class Task{

        StringSeq ids;

        string id;

    }

    interface TaskManager{

        void createTasks(string path);

        void addPartialResult(Result result, string taskId);

        int getRemainingTasks();

        Task getTask();

        void shutdown();

        string getResult();
    }

    //Proxypool connection 

    interface PerformQuery{

        void receiveMessage(StringSeq ids, TaskManager* taskManager,  string taskId );

    }

    //Consultant server


  

    //Consultant auxiliar

    interface MathPrimes{

        int calculatePrime(int id);

    }

    interface ConsultantManager{


        void performQuery(StringSeq ids, Callback* callback);

    }

    interface ConsultantServiceManager{

        void setPoolsize(int n);

        void searchDocumentsByPath(string path, Callback* callback);
    }


    interface ConsultantAuxiliarManager{

        void shutdown();

        void setPoolSize(int n);

        void launch(TaskManager* taskManager);  

    }

}