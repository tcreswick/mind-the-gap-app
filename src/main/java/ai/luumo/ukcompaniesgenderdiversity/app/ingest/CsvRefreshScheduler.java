package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CsvRefreshScheduler {
    private final DataRefreshService dataRefreshService;

    public CsvRefreshScheduler(DataRefreshService dataRefreshService) {
        this.dataRefreshService = dataRefreshService;
    }

    @Scheduled(fixedDelayString = "${app.refresh-interval-ms}")
    public void refreshEveryThreeHours() {
        dataRefreshService.refreshFromSource();
    }
}
