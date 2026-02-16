package com.cisnebranco.specification;

import com.cisnebranco.entity.TechnicalOs;
import com.cisnebranco.entity.enums.OsStatus;
import com.cisnebranco.entity.enums.PaymentStatus;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

@UtilityClass
public class TechnicalOsSpecification {

    public static Specification<TechnicalOs> hasStatus(OsStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<TechnicalOs> hasGroomer(Long groomerId) {
        return (root, query, cb) ->
                groomerId == null ? null : cb.equal(root.join("groomer").get("id"), groomerId);
    }

    public static Specification<TechnicalOs> hasClient(Long clientId) {
        return (root, query, cb) ->
                clientId == null ? null : cb.equal(root.join("pet").join("client").get("id"), clientId);
    }

    public static Specification<TechnicalOs> hasPaymentStatus(PaymentStatus paymentStatus) {
        return (root, query, cb) ->
                paymentStatus == null ? null : cb.equal(root.get("paymentStatus"), paymentStatus);
    }

    public static Specification<TechnicalOs> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start == null) return cb.lessThanOrEqualTo(root.get("createdAt"), end);
            if (end == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.between(root.get("createdAt"), start, end);
        };
    }
}
