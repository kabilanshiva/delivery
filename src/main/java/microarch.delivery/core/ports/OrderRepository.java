package microarch.delivery.core.ports;

import microarch.delivery.core.domain.model.order.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order saveOrder(Order order);

    Optional<Order> findOrderById(UUID orderId);

    Optional<Order> findCreatedOrder();

    List<Order> findAllAssignedOrders();
}