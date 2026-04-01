package microarch.delivery.core.application.eventhandlers;

import lombok.RequiredArgsConstructor;
import microarch.delivery.core.domain.model.order.event.OrderCompletedDomainEvent;
import microarch.delivery.core.ports.OrderEventsProducer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProduceIntegrationMessageOnOrderCompletionHandler {
    private final OrderEventsProducer producer;

    @EventListener
    public void handle(OrderCompletedDomainEvent event) throws Exception {
        producer.publish(event);
    }
}