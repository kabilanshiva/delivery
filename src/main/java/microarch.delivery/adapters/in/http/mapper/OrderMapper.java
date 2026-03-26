package microarch.delivery.adapters.in.http.mapper;

import microarch.delivery.adapters.in.http.model.Location;
import microarch.delivery.adapters.in.http.model.Order;
import microarch.delivery.core.application.queries.dto.OrderDto;

public class OrderMapper {
    public static Order toHttp(OrderDto orderDto) {
        return new Order(
                orderDto.id(),
                new Location(
                        orderDto.location().getX(),
                        orderDto.location().getY()
                )
        );
    }
}