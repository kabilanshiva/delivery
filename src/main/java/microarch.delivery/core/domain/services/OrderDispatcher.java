package microarch.delivery.core.domain.services;

import libs.errs.Result;
import libs.errs.Error;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.order.Order;

import java.util.List;

public interface OrderDispatcher {
    Result<Courier, Error> dispatch(Order order, List<Courier> couriers);
}
