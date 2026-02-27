package com.chat.chat.Batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableScheduling
public class UsuariosReporteScheduler {

    private static final Logger log = LoggerFactory.getLogger(UsuariosReporteScheduler.class);
    private static final String PROP_EXPORT_DIR = "${app.exports.dir:./exports}";
    private static final String PROP_FILENAME_PREFIX = "${app.exports.filenamePrefix:USUARIOS}";
    private static final String CRON_SEMANAL = "0 45 2 ? * MON";
    private static final String ZONE_MADRID = "Europe/Madrid";
    private static final String PARAM_OUTPUT_PREFIX = "outputPrefix";
    private static final String PARAM_SOLO_ACTIVOS = "soloActivos";
    private static final String PARAM_TIMESTAMP = "timestamp";
    private static final String UNDERSCORE = "_";
    private static final String LOG_LANZANDO = "Lanzando exportUsuariosJob -> prefix: {}";

    private final JobLauncher jobLauncher;
    private final Job exportUsuariosJob;

    @Value(PROP_EXPORT_DIR)
    private String exportDir;

    @Value(PROP_FILENAME_PREFIX)
    private String filenamePrefix;

    public UsuariosReporteScheduler(JobLauncher jobLauncher, Job exportUsuariosJob) {
        this.jobLauncher = jobLauncher;
        this.exportUsuariosJob = exportUsuariosJob;
    }

    // Lunes a las 02:45 (menos 15 de las 3), hora de Madrid
    @Scheduled(cron = CRON_SEMANAL, zone = ZONE_MADRID)
    public void semanal() throws Exception {
        lanzar(false); // por defecto: todos
    }

    public void lanzar(boolean soloActivos) throws Exception {
        Files.createDirectories(Path.of(exportDir));

        String fecha = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        // Prefijo SIN extensión; MultiResourceItemWriter añadirá _PARTE_001.csv, etc.
        String outputPrefix = Path.of(exportDir, filenamePrefix + UNDERSCORE + fecha).toString();

        JobParameters params = new JobParametersBuilder()
                .addString(PARAM_OUTPUT_PREFIX, outputPrefix)                   // <- importante
                .addString(PARAM_SOLO_ACTIVOS, Boolean.toString(soloActivos))
                .addLong(PARAM_TIMESTAMP, System.currentTimeMillis())          // para que sea una ejecución nueva
                .toJobParameters();


        jobLauncher.run(exportUsuariosJob, params);
    }
}