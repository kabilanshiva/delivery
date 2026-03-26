package microarch.delivery.core.application.jobs;

import lombok.RequiredArgsConstructor;
import microarch.delivery.core.application.commands.MoveCourierCommandHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MoveCouriersJob implements Job {
    private final MoveCourierCommandHandler useCase;

    @Override
    public void execute(JobExecutionContext context) {
        useCase.handle();
    }
}
