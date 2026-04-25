package com.chat.chat.Batch.Denuncias;

import com.chat.chat.Service.ReportWarningBatchService.ReportWarningBatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportWarningScheduler {

    private final ReportWarningBatchService reportWarningBatchService;

    @Value("${tejechat.batch.report-warning.enabled:true}")
    private boolean enabled;

    public ReportWarningScheduler(ReportWarningBatchService reportWarningBatchService) {
        this.reportWarningBatchService = reportWarningBatchService;
    }

    @Scheduled(
            cron = "${tejechat.batch.report-warning.cron:0 0 */6 * * *}",
            zone = "${tejechat.batch.report-warning.zone:Europe/Madrid}"
    )
    public void ejecutar() {
        if (!enabled) {
            return;
        }
        reportWarningBatchService.ejecutarBatchAdvertenciasPorDenuncias();
    }
}
