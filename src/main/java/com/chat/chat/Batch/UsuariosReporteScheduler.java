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

    private final JobLauncher jobLauncher;
    private final Job exportUsuariosJob;

    @Value("${app.exports.dir:./exports}")
    private String exportDir;

    @Value("${app.exports.filenamePrefix:USUARIOS}")
    private String filenamePrefix;

    public UsuariosReporteScheduler(JobLauncher jobLauncher, Job exportUsuariosJob) {
        this.jobLauncher = jobLauncher;
        this.exportUsuariosJob = exportUsuariosJob;
    }

    // Lunes a las 02:45 (menos 15 de las 3), hora de Madrid
    @Scheduled(cron = "0 45 2 ? * MON", zone = "Europe/Madrid")
    public void semanal() throws Exception {
        lanzar(false); // por defecto: todos
    }

    public void lanzar(boolean soloActivos) throws Exception {
        Files.createDirectories(Path.of(exportDir));

        String fecha = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        // Prefijo SIN extensión; MultiResourceItemWriter añadirá _PARTE_001.csv, etc.
        String outputPrefix = Path.of(exportDir, filenamePrefix + "_" + fecha).toString();

        JobParameters params = new JobParametersBuilder()
                .addString("outputPrefix", outputPrefix)                   // <- importante
                .addString("soloActivos", Boolean.toString(soloActivos))
                .addLong("timestamp", System.currentTimeMillis())          // para que sea una ejecución nueva
                .toJobParameters();

        log.info("⏱️  Lanzando exportUsuariosJob → prefix: {}", outputPrefix);
        jobLauncher.run(exportUsuariosJob, params);
    }
}
