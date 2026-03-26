package microarch.delivery.adapters.in.http;

import lombok.RequiredArgsConstructor;
import microarch.delivery.adapters.in.http.api.CreateOrderApi;
import microarch.delivery.adapters.in.http.model.CreateOrderResponse;
import microarch.delivery.core.application.commands.CreateOrderCommand;
import microarch.delivery.core.application.commands.CreateOrderCommandHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CreateOrderController implements CreateOrderApi {

    private final CreateOrderCommandHandler createOrderCommandHandler;

    @Override
    public ResponseEntity<CreateOrderResponse> createOrder() {
        var createOrderResult = CreateOrderCommand.create(UUID.randomUUID(), "Россия", "Москва", "Тестовая", "1", "1",
                5, 2, 1);

        if (createOrderResult.isFailure())
            return ResponseEntity.badRequest().build();
        var command = createOrderResult.getValue();

        var handlerResult = createOrderCommandHandler.handle(command);
        if (handlerResult.isFailure())
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        var response = new CreateOrderResponse();
        response.setOrderId(handlerResult.getValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}