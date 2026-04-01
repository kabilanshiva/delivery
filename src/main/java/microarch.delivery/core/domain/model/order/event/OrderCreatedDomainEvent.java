package microarch.delivery.core.domain.model.order.event;

import libs.ddd.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import microarch.delivery.core.domain.model.order.Order;

import java.util.UUID;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class OrderCreatedDomainEvent extends DomainEvent {
    private final UUID orderId;

    public OrderCreatedDomainEvent(Order order) {
        super(order);
        this.orderId = order.getId();
    }
}