package microarch.delivery.adapters.in.http;

import lombok.RequiredArgsConstructor;
import microarch.delivery.adapters.in.http.api.GetOrdersApi;
import microarch.delivery.adapters.in.http.mapper.OrderMapper;
import microarch.delivery.adapters.in.http.model.Order;
import microarch.delivery.core.application.queries.GetActiveOrdersQueryHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GetOrdersController implements GetOrdersApi {

    private final GetActiveOrdersQueryHandler getActiveOrdersQueryHandler;

    @Override
    public ResponseEntity<List<Order>> getOrders() {

        var result = getActiveOrdersQueryHandler.handle();
        if (result.isFailure()) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        var response = result
                .getValue()
                .orders()
                .stream()
                .map(OrderMapper::toHttp)
                .toList();

        return ResponseEntity.ok(response);
    }
}