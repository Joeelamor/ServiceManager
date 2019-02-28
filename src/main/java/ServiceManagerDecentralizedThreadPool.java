import java.util.*;
import java.util.concurrent.*;
/**
 * {@code ServiceManagerDecentralizedThreadPool<T>} models the service manager. It invokes a collection of service endpoints using a set
 * of parameters(a map<String, Object>).
 */
public class ServiceManagerDecentralizedThreadPool<T> {

    /**
     * {@code ServiceEndpointDecentralizedWrapper<T>} is a wrapper of Service endpoint with Semaphore.
     */
    private static class ServiceEndpointDecentralizedWrapper<T> {
        private ServiceEndpoint<T> actualServiceEndpoint;
        private Semaphore semaphore;
        private ExecutorService executor;


        private ServiceEndpointDecentralizedWrapper(ServiceEndpoint<T> actualServiceEndpoint, int threadNum) {
            this.actualServiceEndpoint = actualServiceEndpoint;
            // The semaphore will get the max concurrent connection from the original endpoint.
            this.semaphore = new Semaphore(actualServiceEndpoint.getMaxConcurrentInvocations());
            // The executor will be handled by the endpoint.
            this.executor = Executors.newFixedThreadPool(threadNum);
        }

        private Set<String> getSupportedParameters() {
            return this.actualServiceEndpoint.getSupportedParameters();
        }

        /**
         * The invoke function with Semaphore operation. Semaphore will handle the max concurrent connection.
         * @param parameters a map from parameter names to values
         * @return
         */
        private Future<List<T>> invoke(Map<String, Object> parameters) {
            Callable<List<T>> invokeEndpoint = () -> {
                this.semaphore.acquire();
                try {
                    return this.actualServiceEndpoint.invoke(parameters);
                } finally {
                    this.semaphore.release();
                }
            };
            return executor.submit(invokeEndpoint);
        }
    }

    // Store the input endpoints.
    private Collection<ServiceEndpointDecentralizedWrapper<T>> endpoints;

    /**
     * Default constructor creating the ServiceManagerDecentralizedThreadPool in initial mode.
     */
    public ServiceManagerDecentralizedThreadPool(Collection<ServiceEndpoint<T>> endpoints, int threadNumPerEndpoint) {
        this.endpoints = new ArrayList<>();

        // For each endpoint, create a new ExecutorService.
        for (ServiceEndpoint<T> endpoint : endpoints) {
            this.endpoints.add(new ServiceEndpointDecentralizedWrapper<>(endpoint, threadNumPerEndpoint));
        }
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

        // For each endpoint, check if the parameters matched, if matched, each endpoint will call invoke in its own
        // executor and return a future and add the list into future list.
        for (ServiceEndpointDecentralizedWrapper<T> endpoint : endpoints) {
            if (parameters.isEmpty() || endpoint.getSupportedParameters().containsAll(parameters.keySet())) {
                futures.add(endpoint.invoke(parameters));
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
