import exception.InvocationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Endpoint implements ServiceEndpoint<String> {
    public int getMaxConcurrentInvocations() {
        return 0;
    }

    public Set<String> getSupportedParameters() {
        return null;
    }

    public List<String> invoke(Map<String, Object> parameters) throws InvocationException {
        return new ArrayList<>();
    }

}
