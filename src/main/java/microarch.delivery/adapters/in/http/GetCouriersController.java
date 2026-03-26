package microarch.delivery.adapters.in.http;

import lombok.RequiredArgsConstructor;
import microarch.delivery.adapters.in.http.api.GetCouriersApi;
import microarch.delivery.adapters.in.http.mapper.CourierMapper;
import microarch.delivery.adapters.in.http.model.Courier;
import microarch.delivery.core.application.queries.GetAllCouriersQueryHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GetCouriersController implements GetCouriersApi {
    private final GetAllCouriersQueryHandler getAllCouriersQueryHandler;

    @Override
    public ResponseEntity<List<Courier>> getCouriers() {

        var handlerResult = getAllCouriersQueryHandler.handle();
        if (handlerResult.isFailure()) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        var response = handlerResult
                .getValue()
                .couriers()
                .stream()
                .map(CourierMapper::toHttp)
                .toList();

        return ResponseEntity.ok(response);
    }

}