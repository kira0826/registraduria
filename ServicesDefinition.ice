module RegistryModule
{


    sequence<int> IntSeq;

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

    //Consultant auxiliar

    interface MathPrimes{

        int calculatePrime(int id);

    }

    interface ConsultantManager{


        void performQuery(IntSeq ids, Callback* callback);

    }

    interface ConsultantAuxiliarManager{

        void shutdown();

        void launch();  

    }

    //Proxypool connection 

    interface PerformQuery{


        Object connection(string hostname);

        string makequery(IntSeq ids);

        string executeQuery(string query);

        void receiveMessage(IntSeq ids, Callback* callback);
        
    }

    //Consultant server


    class Task{

        IntSeq ids;

        string hostname;

    }

    interface TaskManager{

        void addPartialResult(string result);
        
        Task getTask();
    }

}