package com.chat.chat.Batch;

import com.chat.chat.Batch.Listeners.CsvErroresSkipListener;
import com.chat.chat.Batch.Processors.UsuarioToReporteProcessor;
import com.chat.chat.DTO.UsuarioReporteCsvDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@Configuration
@EnableBatchProcessing
public class UsuariosReporteBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(UsuariosReporteBatchConfig.class);

    // === READER: JPA paginado por ID ===
    @Bean
    public RepositoryItemReader<UsuarioEntity> usuariosReader(UsuarioRepository repo) {
        return new RepositoryItemReaderBuilder<UsuarioEntity>()
                .name("usuariosReader")
                .repository(repo)
                .methodName("findAll")
                .pageSize(1000)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    // === PROCESSOR step-scoped (lee 'soloActivos') ===
    @Bean
    @StepScope
    public UsuarioToReporteProcessor usuarioToReporteProcessor(
            @Value("#{jobParameters['soloActivos'] ?: 'false'}") String soloActivos
    ) {
        UsuarioToReporteProcessor p = new UsuarioToReporteProcessor();
        p.setSoloActivos(Boolean.parseBoolean(soloActivos));
        return p;
    }

    // === WRITER base (para un archivo) ===
    @Bean
    public FlatFileItemWriter<UsuarioReporteCsvDTO> singleCsvWriter() {
        BeanWrapperFieldExtractor<UsuarioReporteCsvDTO> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[]{
                "id","nombre","apellido","email","activo","fechaCreacion","roles","fotoUrl"
        });

        DelimitedLineAggregator<UsuarioReporteCsvDTO> agg = new DelimitedLineAggregator<>();
        agg.setDelimiter(";");
        agg.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<UsuarioReporteCsvDTO>()
                .name("singleCsvWriter")
                .lineAggregator(agg)
                .headerCallback(w -> w.write("id;nombre;apellido;email;activo;fechaCreacion;roles;fotoUrl"))
                .encoding("UTF-8")
                .append(false)    // cada archivo nuevo empieza limpio
                .saveState(true)  // soporta restart
                .build();
    }

    // === MULTI WRITER: parte en varios archivos por límite de items ===
    @Bean
    @StepScope
    public MultiResourceItemWriter<UsuarioReporteCsvDTO> multiWriter(
            @Value("#{jobParameters['outputPrefix']}") String outputPrefix,
            FlatFileItemWriter<UsuarioReporteCsvDTO> singleCsvWriter
    ) {
        // outputPrefix ejemplo: C:/Users/PC/Desktop/USUARIOS_20250825
        return new MultiResourceItemWriterBuilder<UsuarioReporteCsvDTO>()
                .name("multiWriter")
                .delegate(singleCsvWriter)
                .itemCountLimitPerResource(50000) // ajusta el tamaño por archivo
                .resource(new FileSystemResource(outputPrefix)) // base (sin extensión)
                .resourceSuffixCreator(index -> String.format("_PARTE_%03d.csv", index + 1))
                .saveState(true)
                .build();
    }

    // === LISTENER de errores (skips) → *_errores.csv ===
    @Bean
    @StepScope
    public CsvErroresSkipListener erroresSkipListener(
            @Value("#{jobParameters['outputPrefix']}") String outputPrefix
    ) {
        Path errorPath = Paths.get(outputPrefix + "_errores.csv");
        return new CsvErroresSkipListener(errorPath);
    }

    // === STEP (chunk normal + tolerancia a fallos + multi-writer) ===
    @Bean
    public Step exportUsuariosStep(JobRepository jobRepository,
                                   PlatformTransactionManager tx,
                                   RepositoryItemReader<UsuarioEntity> reader,
                                   @Qualifier("usuarioToReporteProcessor")
                                   ItemProcessor<UsuarioEntity, UsuarioReporteCsvDTO> processor,
                                   MultiResourceItemWriter<UsuarioReporteCsvDTO> multiWriter,
                                   CsvErroresSkipListener erroresSkipListener) {

        return new StepBuilder("exportUsuariosStep", jobRepository)
                .<UsuarioEntity, UsuarioReporteCsvDTO>chunk(1000, tx)
                .reader(reader)
                .processor(processor)
                .writer(multiWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .skipLimit(1000)
                .skip(Exception.class)
                .listener(erroresSkipListener)
                .listener((StepExecutionListener) new StepExecutionListener() {
                    @Override public void beforeStep(StepExecution stepExecution) {
                        String prefix = stepExecution.getJobParameters().getString("outputPrefix");
                        log.info("➡️  Export de usuarios (prefijo de salida): {}", prefix);
                    }
                    @Override public ExitStatus afterStep(StepExecution s) { return s.getExitStatus(); }
                })
                .build();
    }

    // === JOB (métricas + conteo) ===
    @Bean
    public Job exportUsuariosJob(JobRepository jobRepository,
                                 Step exportUsuariosStep,
                                 UsuarioRepository usuarioRepository) {
        return new JobBuilder("exportUsuariosJob", jobRepository)
                .start(exportUsuariosStep)
                .listener(new JobExecutionListenerSupport() {
                    @Override public void beforeJob(JobExecution jobExecution) {
                        log.info("🚀 Iniciando exportUsuariosJob con parámetros: {}", jobExecution.getJobParameters());
                    }
                    @Override public void afterJob(JobExecution jobExecution) {
                        long read = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
                        long write = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
                        long filter = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getFilterCount).sum();
                        long total = usuarioRepository.count();
                        long activos = usuarioRepository.countByActivoTrue();
                        log.info("✅ Job finalizado: {}", jobExecution.getStatus());
                        log.info("📊 Métricas: read={}, written={}, filtered={}", read, write, filter);
                        log.info("👥 Usuarios: total={}, activos={}", total, activos);
                    }
                })
                .build();
    }
}
