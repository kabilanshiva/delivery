package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import microarch.delivery.core.domain.model.Address;
import microarch.delivery.core.domain.model.kernel.Volume;

import java.util.Objects;
import java.util.UUID;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateOrderCommand {
    private final UUID orderId;

    private final Address address;

    private final Volume volume;

    public static Result<CreateOrderCommand, Error> create(
            UUID orderId,
            String country,
            String city,
            String street,
            String house,
            String apartment,
            int volumeX,
            int volumeY,
            int volumeZ
    ) {
        if (Objects.isNull(orderId)) return Result.failure(GeneralErrors.valueIsRequired("orderId"));

        var address = Address.create(country, city, street, house, apartment);
        if (address.isFailure()) return Result.failure(address.getError());

        var volume = Volume.create(volumeX, volumeY, volumeZ);
        if (volume.isFailure()) return Result.failure(volume.getError());

        return Result.success(new CreateOrderCommand(orderId, address.getValue(), volume.getValue()));
    }
}