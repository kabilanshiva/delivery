package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateOrderCommandHandlerImpl implements CreateOrderCommandHandler {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Result<UUID, Error> handle(CreateOrderCommand command) {
        var orderFromDb = orderRepository.findOrderById(command.getOrderId());
        if (orderFromDb.isEmpty()) {
            var createOrder = Order.create(command.getOrderId(), getRandomLocation(), command.getVolume());
            if (createOrder.isFailure()) return Result.failure(createOrder.getError());
            var order = createOrder.getValue();

            orderRepository.saveOrder(order);

            return Result.success(order.getId());
        }
        return Result.success(orderFromDb.get().getId());
    }

    private Location getRandomLocation() {
        var random = new Random();
        int x = random.nextInt(10) + 1;
        int y = random.nextInt(10) + 1;
        return Location.create(x, y).getValueOrThrow();
    }
}