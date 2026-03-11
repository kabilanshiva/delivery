package microarch.delivery.core.domain.services;

import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderDispatcherImpl implements OrderDispatcher {
    @Override
    public Result<Courier, Error> dispatch(Order order, List<Courier> couriers) {
        if (Objects.isNull(order)) return Result.failure(GeneralErrors.valueIsRequired("order"));
        if (Objects.isNull(couriers) || couriers.isEmpty())
            return Result.failure(GeneralErrors.valueIsRequired("couriers"));
        if (OrderStatus.CREATED != order.getStatus())
            return Result.failure(Errors.orderIsNotReadyToBeDelivered(order.getId()));

        var suitableCourier = couriers.stream()
                .filter(courier -> courier.isAvailable().isSuccess())
                .filter(courier -> courier.canTakeOrder(order.getVolume()).isSuccess())
                .min(Comparator.comparing(courier -> courier.calculateTimeToLocation(order.getLocation()).getValueOrThrow()));

        if (suitableCourier.isEmpty()) {
            return Result.failure(Errors.noSuitableCouriersForOrder(order.getId()));
        }

        var courier = suitableCourier.get();
        var takeOrder = courier.takeOrder(order.getId(), order.getVolume());
        if (takeOrder.isFailure()) {
            return Result.failure(takeOrder.getError());
        }
        else {
            order.assign(courier);
            return Result.success(courier);
        }
    }


    private static class Errors {
        public static Error orderIsNotReadyToBeDelivered(UUID orderId) {
            return Error.of("order_dispatcher.order_is_not_ready_to_be_delivered",
                    String.format("Order [%s] is not not ready to be delivered", orderId));
        }

        public static Error noSuitableCouriersForOrder(UUID orderId) {
            return Error.of("order_dispatcher.no_suitable_couriers_for_the_order",
                    String.format("Order [%s] can not be delivered by the provided couriers at the moment", orderId));
        }
    }
}
