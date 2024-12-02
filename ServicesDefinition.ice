module RegistryModule
{


    sequence<string> StringSeq;

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

    //Proxypool connection 

    interface PerformQuery{



        void receiveMessage(StringSeq ids, Callback* callback);
        
    }

    //Consultant server


    class Task{

        StringSeq ids;

        string id;

    }

    interface TaskManager{

        void createTasks(string path);

        void addPartialResult(string result, string taskId);

        int getRemainingTasks();

        Task getTask();

        void shutdown();

        string getResult();
    }

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