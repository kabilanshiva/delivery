package microarch.delivery.core.application.queries;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.application.queries.dto.OrderDto;
import org.springframework.stereotype.Service;

@Service
public class GetActiveOrdersQueryHandlerImpl implements GetActiveOrdersQueryHandler {
    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Result<GetActiveOrdersResponse, Error> handle() {
        String sql = """
                SELECT o.id, o.location
                FROM Order o
                WHERE o.status IN ( 'CREATED' , 'ASSIGNED')
                """;

        var orderDtos = em.createQuery(sql, OrderDto.class).getResultList();

        return Result.success(new GetActiveOrdersResponse(orderDtos));
    }
}
