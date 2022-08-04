package me.zort.gencore.object;

import com.google.common.collect.Maps;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class Predicate {

    public static final Supplier<Boolean> DEFAULT_TRUE = () -> true;
    public static final Supplier<Boolean> DEFAULT_FALSE = () -> false;
    public static final Predicate TRUE = Predicate.of(DEFAULT_TRUE);
    public static final Predicate FALSE = Predicate.of(DEFAULT_FALSE);

    public static Predicate of(Supplier<Boolean> supplier) {
        return new Predicate(supplier);
    }

    private final Map<Long, Predicate> predCacheInternal = Maps.newLinkedHashMap();
    private final UUID uniqueId = UUID.randomUUID();
    private final Supplier<Boolean> base;

    private Predicate(Supplier<Boolean> initial) {
        this.base = initial;
    }

    public boolean test() {
        return toSupplier().get();
    }

    public Predicate and(Object other) {
        Objects.requireNonNull(other);
        Supplier<Boolean> base;
        if(other instanceof Predicate) {
            Predicate pred = (Predicate) other;
            base = pred.toSupplier();
        } else if(other instanceof Supplier && validate((Supplier<?>) other)) {
            @SuppressWarnings("unchecked")
            Supplier<Boolean> supplier = (Supplier<Boolean>) other;
            base = supplier;
        } else throw new RuntimeException("Invalid predicate addon!");
        long currentTime = System.currentTimeMillis();
        predCacheInternal.put(currentTime, Predicate.of(base));
        return this;
    }

    public boolean deletePart(UUID uniqueId) {
        Objects.requireNonNull(uniqueId);
        return deletePart(pred -> pred.getUniqueId().equals(uniqueId));
    }

    public boolean deletePart(java.util.function.Predicate<Predicate> pred) {
        Optional<Map.Entry<Long, Predicate>> entryOptional = predCacheInternal.entrySet().stream()
                .filter(entry -> pred.test(entry.getValue()))
                .findFirst();
        if(!entryOptional.isPresent()) {
            return false;
        }
        Map.Entry<Long, Predicate> entry = entryOptional.get();
        return predCacheInternal.remove(entry) != null;
    }

    public boolean anyPart(java.util.function.Predicate<Predicate> pred) {
        return predCacheInternal.values().stream()
                .anyMatch(pred);
    }

    @SuppressWarnings("rawtypes")
    private boolean validate(Supplier<?> supplier) {
        Class<? extends Supplier> clazz = supplier.getClass();
        try {
            Method method = clazz.getDeclaredMethod("get");
            Object result = method.invoke(supplier);
            Class<?> resultClass = result.getClass();
            return resultClass.equals(Boolean.class) || resultClass.equals(boolean.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Supplier<Boolean> toSupplier() {
        Supplier<Boolean> res = base;
        for(long initTime : predCacheInternal.keySet()) {
            Predicate predicate = predCacheInternal.get(initTime);
            Supplier<Boolean> supplier = predicate.toSupplier();
            if(res == null) {
                res = supplier;
                continue;
            }
            final Supplier<Boolean> resFinal = res;
            res = () -> resFinal.get() && supplier.get();
        }
        return res;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

}
