package no.ssb.testing.helidon;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class MockRegistry implements Iterable<Object> {

    private final Map<String, Object> instanceByType = new ConcurrentSkipListMap<>();

    public <T> MockRegistry add(T instance) {
        instanceByType.put(instance.getClass().getName(), instance);
        return this;
    }

    public <T> T get(Class<T> clazz) {
        return (T) instanceByType.get(clazz.getName());
    }

    @Override
    public Iterator<Object> iterator() {
        return instanceByType.values().iterator();
    }
}
