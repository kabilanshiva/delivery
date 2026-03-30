package microarch.delivery.core.application.commands;

import libs.ddd.DomainEventPublisher;
import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.GeoClient;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateOrderCommandHandlerImpl implements CreateOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final GeoClient geoClient;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public Result<UUID, Error> handle(CreateOrderCommand command) {
        var orderFromDb = orderRepository.findOrderById(command.getOrderId());
        if (orderFromDb.isEmpty()) {
            var location = geoClient.getLocation(command.getAddress());
            var createOrder = Order.create(command.getOrderId(), location, command.getVolume());
            if (createOrder.isFailure())
                return Result.failure(createOrder.getError());
            var order = createOrder.getValue();

            orderRepository.saveOrder(order);
            domainEventPublisher.publish(List.of(order));

            return Result.success(order.getId());
        }
        return Result.success(orderFromDb.get().getId());
    }
}