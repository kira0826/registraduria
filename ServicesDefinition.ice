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

        string hostname;

    }

    interface TaskManager{

        void addPartialResult(string result);
        
        Task getTask();
    }

    //Consultant auxiliar

    interface MathPrimes{

        int calculatePrime(int id);

    }

    interface ConsultantManager{

        void performQuery(StringSeq ids, Callback* callback);

    }

    interface ConsultantAuxiliarManager{

        void shutdown();

        void launch(TaskManager* taskManager);  

    }

}