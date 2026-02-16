package com.cisnebranco.specification;

import com.cisnebranco.entity.Client;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@UtilityClass
public class ClientSpecification {

    public static Specification<Client> nameContains(String name) {
        return (root, query, cb) -> {
            if (name == null) return null;
            String escaped = escapeLike(name.toLowerCase());
            return cb.like(cb.lower(root.get("name")), "%" + escaped + "%");
        };
    }

    public static Specification<Client> phoneContains(String phone) {
        return (root, query, cb) -> {
            if (phone == null) return null;
            String escaped = escapeLike(phone);
            return cb.like(root.get("phone"), "%" + escaped + "%");
        };
    }

    private static String escapeLike(String value) {
        return value.replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<Client> registeredAfter(LocalDateTime date) {
        return (root, query, cb) ->
                date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }
}
