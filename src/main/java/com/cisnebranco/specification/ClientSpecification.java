package com.cisnebranco.specification;

import com.cisnebranco.entity.Client;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@UtilityClass
public class ClientSpecification {

    public static Specification<Client> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Client> phoneContains(String phone) {
        return (root, query, cb) ->
                phone == null ? null : cb.like(root.get("phone"), "%" + phone + "%");
    }

    public static Specification<Client> registeredAfter(LocalDateTime date) {
        return (root, query, cb) ->
                date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }
}
