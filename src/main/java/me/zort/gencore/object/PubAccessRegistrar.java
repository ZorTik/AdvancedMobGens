package me.zort.gencore.object;

import com.google.common.collect.Maps;
import lombok.Data;
import me.zort.gencore.data.Accessor;
import me.zort.gencore.validator.Validator;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Connector with accessors.
 *
 * @param <C> Connection type
 * @param <CRE> Credentials type
 * @param <A> Accessors enum type
 */
public abstract class PubAccessRegistrar<A extends Enum<?>> {

    private final Map<AccessorIdentity<A>, Accessor<?>> accessorsCacheInternal = Maps.newConcurrentMap();

    public <S> String registerAccessor(A eventType, Class<S> subjectClass, Accessor<S> accessor) {
        Validator.requireNonNulls(eventType, subjectClass, accessor);
        AccessorIdentity<A> identity = new AccessorIdentity<>();
        identity.setUniqueId(RandomStringUtils.randomAlphanumeric(12));
        identity.setEventType(eventType);
        identity.setSubjectClass(subjectClass);
        accessorsCacheInternal.put(identity, accessor);
        return identity.getUniqueId();
    }

    public <S> boolean invokeAccessors(A eventType, S object) {
        return invokeAccessors(eventType, object, ex -> {});
    }

    @SuppressWarnings("unchecked")
    public <S> boolean invokeAccessors(A eventType, S object, @Nullable Consumer<Exception> onError) {
        Validator.requireNonNulls(eventType, object);
        Class<?> objectClass = object.getClass();
        Predicate<AccessorIdentity<A>> pred = accessorIdentity -> accessorIdentity.getSubjectClass().equals(objectClass);
        List<Accessor<?>> accessorsList = getAccessorsByEvent(eventType, pred);
        if(accessorsList.isEmpty()) {
            return false;
        }
        accessorsList.forEach(accessor -> {
            try {
                Accessor<S> subjectAccessor = (Accessor<S>) accessor;
                subjectAccessor.access(object);
            } catch(Exception ex) {
                if(onError != null) {
                    onError.accept(ex);
                }
            }
        });
        return true;
    }

    public Optional<Accessor<?>> getAccessorById(String uniqueId) {
        return getAccessorBy(aAccessorIdentity -> aAccessorIdentity.getUniqueId().equals(uniqueId));
    }

    public Optional<Accessor<?>> getAccessorBy(Predicate<AccessorIdentity<A>> pred) {
        return getAccessorsBy(pred).stream().findFirst();
    }

    public List<Accessor<?>> getAccessorsByEvent(A eventType, Predicate<AccessorIdentity<A>>... otherPreds) {
        Predicate<AccessorIdentity<A>> pred = aAccessorIdentity -> aAccessorIdentity.getEventType().equals(eventType);
        for(Predicate<AccessorIdentity<A>> other : otherPreds) {
            pred = pred.and(other);
        }
        return getAccessorsBy(pred);
    }

    public List<Accessor<?>> getAccessorsBy(Predicate<AccessorIdentity<A>> pred) {
        return accessorsCacheInternal.entrySet().stream()
                .filter(entry -> pred.test(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Data
    public static class AccessorIdentity<A extends Enum<?>> {

        private String uniqueId;

        private A eventType;
        private Class<?> subjectClass;


    }

}
