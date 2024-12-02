import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MathPrimes {

    private final Set<Integer> primes;

    public MathPrimes(int N) {
        this.primes = new HashSet<>(sieveOfEratosthenes((int) Math.sqrt(N) + 1));
    }

    // Generate prime numbers up to N using the Sieve of Eratosthenes
    private List<Integer> sieveOfEratosthenes(int N) {
        boolean[] isPrime = new boolean[N + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = isPrime[1] = false;

        // Sieve of Eratosthenes
        for (int i = 2; i * i <= N; i++) {
            if (isPrime[i]) {
                for (int j = i * i; j <= N; j += i) {
                    isPrime[j] = false;
                }
            }
        }

        // Collect prime numbers
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= N; i++) {
            if (isPrime[i]) {
                primes.add(i);
            }
        }

        return primes;
    }

    // Find prime factors of a number N using the primes already computed
    private List<Integer> primeFactors(int N) {
        List<Integer> factors = new ArrayList<>();
        int n = N;

        // Factorizing N using previously calculated primes
        for (int p : primes) {
            while (n % p == 0) {
                factors.add(p);
                n /= p;
            }
            if (n == 1) break;
        }

        // If any number greater than 1 remains, it's a prime factor
        if (n > 1) {
            factors.add(n);
        }

        return factors;
    }

    // Check if the number of prime factors is prime using the precomputed primes list
    private String isPrimeFactorCountPrime(int N) {
        List<Integer> factors = primeFactors(N);
        int factorCount = factors.size();

        return primes.contains(factorCount) ? "1" : "0";
    }

    public Map<String, String> isPrimeFactorCount(List<String> numbers) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Map<String, String> results = new ConcurrentHashMap<>();

        List<Integer> parsedNumbers = numbers.stream().map(Integer::parseInt).collect(Collectors.toList());

        List<Future<Void>> futures = new ArrayList<>();
        for (Integer number : parsedNumbers) {
            futures.add(executorService.submit(() -> {
                String result = isPrimeFactorCountPrime(number);
                results.put(String.valueOf(number), result);
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executorService.shutdown();
        return results;
    }
}
