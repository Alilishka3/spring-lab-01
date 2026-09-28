package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Order(4)
public class CallCounterAspect {

    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    @Before("kz.iitu.springlab.aspect.Pointcuts.serviceOperation()")
    public void count(JoinPoint jp) {
        counters.merge(jp.getSignature().getName(), 1L, Long::sum);
    }

    public Map<String, Long> getStatistics() {
        return new TreeMap<>(counters);
    }
}