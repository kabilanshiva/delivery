package microarch.delivery.adapters.out.postgres;

import lombok.AllArgsConstructor;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.ports.CourierRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class CourierRepositoryImpl implements CourierRepository {

    private final CourierJpaRepository courierJpaRepository;

    @Override
    public Courier saveCourier(Courier courier) {
        return courierJpaRepository.save(courier);
    }

    @Override
    public Optional<Courier> findCourierById(UUID courierId) {
        return courierJpaRepository.findById(courierId);
    }

    @Override
    public List<Courier> findAvailableCouriers() {
        return courierJpaRepository.findAvailableCouriers();
    }
}