package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.UnitResult;
import lombok.RequiredArgsConstructor;
import microarch.delivery.core.domain.services.OrderDispatcher;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignOrderCommandHandlerImpl implements AssignOrderCommandHandler {
    private final OrderDispatcher orderDispatcher;
    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public UnitResult<Error> handle() {
        var createdOrder = orderRepository.findCreatedOrder();
        if (createdOrder.isEmpty()) return UnitResult.success();

        var order = createdOrder.get();

        var couriers = courierRepository.findAvailableCouriers();

        var dispatch = orderDispatcher.dispatch(order, couriers);

        if (dispatch.isFailure()) return UnitResult.failure(dispatch.getError());

        orderRepository.saveOrder(order);
        courierRepository.saveCourier(dispatch.getValue());

        return UnitResult.success();
    }
}