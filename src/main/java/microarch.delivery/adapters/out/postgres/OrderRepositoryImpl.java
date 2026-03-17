package microarch.delivery.adapters.out.postgres;

import lombok.AllArgsConstructor;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order saveOrder(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findOrderById(UUID orderId) {
        return orderJpaRepository.findById(orderId);
    }

    @Override
    public Optional<Order> findCreatedOrder() {
        return orderJpaRepository.findFirstByStatus(OrderStatus.CREATED);
    }

    @Override
    public List<Order> findAllAssignedOrders() {
        return orderJpaRepository.findAllByStatus(OrderStatus.ASSIGNED);
    }
}