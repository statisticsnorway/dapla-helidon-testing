package no.ssb.testing.helidon;

import io.grpc.BindableService;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class GrpcMockRegistry implements Iterable<BindableService> {

    private final Map<String, BindableService> instanceByType = new ConcurrentSkipListMap<>();

    public <T extends BindableService> GrpcMockRegistry add(T instance) {
        instanceByType.put(instance.getClass().getName(), instance);
        return this;
    }

    public <T extends BindableService> T get(Class<T> clazz) {
        return (T) instanceByType.get(clazz.getName());
    }

    @Override
    public Iterator<BindableService> iterator() {
        return instanceByType.values().iterator();
    }
}
