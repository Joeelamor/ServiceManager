import exception.InvocationException;

import java.util.*;
import java.util.concurrent.*;

/**
 * {@code ServiceManager<T>} models the service manager. It invokes a collection of service endpoints using a set
 * of parameters(a map<String, Object>).
 */
public class ServiceManager<T> {

    /**
     * {@code ServiceEndpointWrapper<T>} is a wrapper of Service endpoint with Semaphore.
     */
    private static class ServiceEndpointWrapper<T> {
        private ServiceEndpoint<T> actualServiceEndpoint;
        private Semaphore semaphore;

        private ServiceEndpointWrapper(ServiceEndpoint<T> actualServiceEndpoint) {
            this.actualServiceEndpoint = actualServiceEndpoint;

            // The semaphore will get the max concurrent connection from the original endpoint.
            this.semaphore = new Semaphore(actualServiceEndpoint.getMaxConcurrentInvocations());
        }

        private Set<String> getSupportedParameters() {
            return this.actualServiceEndpoint.getSupportedParameters();
        }

        /**
         * The invoke function with Semaphore operation. Semaphore will handle the max concurrent connection.
         *
         * @param parameters a map from parameter names to values
         * @return the results
         * @throws InterruptedException
         * @throws InvocationException
         */
        private List<T> invoke(Map<String, Object> parameters) throws InterruptedException, InvocationException {
            this.semaphore.acquire();
            try {
                return this.actualServiceEndpoint.invoke(parameters);
            } finally {
                this.semaphore.release();
            }
        }
    }

    // Store the input endpoints.
    private Collection<ServiceEndpointWrapper<T>> endpoints;

    // Define a executorService.
    private ExecutorService executor;


    /**
     * Default constructor creating the ServiceManager in initial mode.
     */
    public ServiceManager(Collection<ServiceEndpoint<T>> endpoints, int threadNum) {
        this.endpoints = new ArrayList<>();
        for (ServiceEndpoint<T> endpoint : endpoints) {
            this.endpoints.add(new ServiceEndpointWrapper<>(endpoint));
        }
        this.executor = Executors.newFixedThreadPool(threadNum);
    }

    /**
     * Manager invoke function is to match all endpoints with parameters and get results from the endpoints.
     * @param parameters parameters a map from parameter names to values
     * @return all result from endpoints will be added into a final list.
     */
    public List<T> invoke(Map<String, Object> parameters) {
        // Initialize a list of Future result list.
        List<Future<List<T>>> futures = new ArrayList<>();

        // Initialize a final result list.
        List<T> ret = new ArrayList<>();

        // For each endpoint, check if the parameters matched, if matched, add the invoke endpoint into Callable list
        // and then submit the list into future list.
        for (ServiceEndpointWrapper<T> endpoint : endpoints) {
            if (parameters.isEmpty() || endpoint.getSupportedParameters().containsAll(parameters.keySet())) {
                Callable<List<T>> invokeEndpoint = () -> endpoint.invoke(parameters);
                Future<List<T>> future = executor.submit(invokeEndpoint);
                futures.add(future);
            }
        }

        // Once finish traversing all endpoints, traverse future list to add all results into final result list.
        try {
            Iterator<Future<List<T>>> iterator;
            Future<List<T>> future;

            // Check the future list until the future list is empty which means all events are executed.
            while (!futures.isEmpty()) {
                iterator = futures.iterator();
                while (iterator.hasNext()) {
                    future = iterator.next();
                    if (!future.isDone()) {
                        continue;
                    }
                    ret.addAll(future.get());
                    iterator.remove();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return ret;
    }
}

