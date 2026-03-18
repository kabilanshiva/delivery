package microarch.delivery.core.application.queries;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.application.queries.dto.CourierDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAllCouriersQueryHandlerImpl implements GetAllCouriersQueryHandler {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Result<GetAllCouriersResponse, Error> handle() {
        String jpql = """
                SELECT NEW microarch.delivery.core.application.queries.dto.CourierDto(
                    c.id, c.name, c.location
                )
                FROM Courier c
                """;

        var courierDtoList = em.createQuery(jpql, CourierDto.class)
                .getResultList();

        return Result.success(new GetAllCouriersResponse(courierDtoList));
    }
}