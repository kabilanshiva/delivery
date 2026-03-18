package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.UnitResult;
import lombok.RequiredArgsConstructor;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoveCourierCommandHandlerImpl implements MoveCourierCommandHandler {
    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public UnitResult<Error> handle() {
        var ordersToBeMoved = orderRepository.findAllAssignedOrders();

        if (ordersToBeMoved.isEmpty()) return UnitResult.success();

        List<Error> errors = new ArrayList<>();

        for (var order : ordersToBeMoved) {
            var courierResult = courierRepository.findCourierById(order.getCourierId());

            if (courierResult.isEmpty()) {
                errors.add(Errors.invalidCourierForTheOrder(order.getId()));
                continue;
            }

            var courier = courierResult.get();
            if (courier.getLocation().equals(order.getLocation())) {
                var completeOrder = order.complete();
                if (completeOrder.isFailure()) {
                    errors.add(completeOrder.getError());
                    continue;
                }

                var courierCompleteOrder = courier.completeOrder(order.getId());
                if (courierCompleteOrder.isFailure()) errors.add(courierCompleteOrder.getError());

                orderRepository.saveOrder(order);
                courierRepository.saveCourier(courier);

                continue;
            }

            var moveCourier = courier.move(order.getLocation());
            if (moveCourier.isFailure()) {
                errors.add(moveCourier.getError());
            } else {
                courierRepository.saveCourier(courier);
            }
        }

        return errors.isEmpty() ? UnitResult.success() : UnitResult.failure(Errors.moveCourierHandlerErrors(errors));
    }

    private static class Errors {
        public static Error moveCourierHandlerErrors(List<Error> errors) {
            return Error.of("move_courier_command_handler.failed_partially",
                    String.format("Moving couriers partially failed [%s]",
                            errors.stream().map(Error::getMessage).collect(Collectors.joining(", "))));
        }

        public static Error invalidCourierForTheOrder(UUID orderId) {
            return Error.of(
                    "move_courier_command_handler.courier_does_not_exist",
                    String.format("Invalid courier for the order : %s ", orderId)
            );
        }
    }
}