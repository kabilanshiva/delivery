package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.Result;
import lombok.RequiredArgsConstructor;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.ports.CourierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateCourierCommandHandlerImpl implements CreateCourierCommandHandler {

    private static final Location DEFAULT_LOCATION = Location.create(5,5).getValueOrThrow();
    private final CourierRepository courierRepository;

    @Override
    @Transactional
    public Result<UUID, Error> handle(CreateCourierCommand command) {
        var courierResult = Courier.create(command.getName(), command.getSpeed(), DEFAULT_LOCATION);

        if (courierResult.isFailure()) return Result.failure(courierResult.getError());

        var courier = courierResult.getValue();

        courierRepository.saveCourier(courier);

        return Result.success(courier.getId());
    }
}