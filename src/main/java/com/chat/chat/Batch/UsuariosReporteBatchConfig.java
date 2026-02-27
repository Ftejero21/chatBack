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
    private static final String READER_NAME = "usuariosReader";
    private static final String READER_METHOD_FIND_ALL = "findAll";
    private static final String SORT_ID = "id";
    private static final String PARAM_SOLO_ACTIVOS = "soloActivos";
    private static final String PARAM_OUTPUT_PREFIX = "outputPrefix";
    private static final String SPEL_SOLO_ACTIVOS = "#{jobParameters['soloActivos'] ?: 'false'}";
    private static final String SPEL_OUTPUT_PREFIX = "#{jobParameters['outputPrefix']}";
    private static final String WRITER_SINGLE_NAME = "singleCsvWriter";
    private static final String WRITER_MULTI_NAME = "multiWriter";
    private static final String PROCESSOR_BEAN_NAME = "usuarioToReporteProcessor";
    private static final String CSV_DELIMITER = ";";
    private static final String CSV_ENCODING = "UTF-8";
    private static final String CSV_HEADER = "id;nombre;apellido;email;activo;fechaCreacion;roles;fotoUrl";
    private static final String[] CSV_FIELDS = new String[]{
            "id", "nombre", "apellido", "email", "activo", "fechaCreacion", "roles", "fotoUrl"
    };
    private static final String RESOURCE_SUFFIX_FORMAT = "_PARTE_%03d.csv";
    private static final String ERROR_SUFFIX = "_errores.csv";
    private static final String STEP_NAME = "exportUsuariosStep";
    private static final String JOB_NAME = "exportUsuariosJob";
    private static final String LOG_EXPORT_PREFIX = "Export de usuarios (prefijo de salida): {}";
    private static final String LOG_JOB_START = "Iniciando exportUsuariosJob con parámetros: {}";
    private static final String LOG_JOB_DONE = "Job finalizado: {}";
    private static final String LOG_JOB_METRICS = "Métricas: read={}, written={}, filtered={}";
    private static final String LOG_JOB_USERS = "Usuarios: total={}, activos={}";

    // === READER: JPA paginado por ID ===
    @Bean
    public RepositoryItemReader<UsuarioEntity> usuariosReader(UsuarioRepository repo) {
        return new RepositoryItemReaderBuilder<UsuarioEntity>()
                .name(READER_NAME)
                .repository(repo)
                .methodName(READER_METHOD_FIND_ALL)
                .pageSize(1000)
                .sorts(Collections.singletonMap(SORT_ID, Sort.Direction.ASC))
                .build();
    }

    // === PROCESSOR step-scoped (lee 'soloActivos') ===
    @Bean
    @StepScope
    public UsuarioToReporteProcessor usuarioToReporteProcessor(
            @Value(SPEL_SOLO_ACTIVOS) String soloActivos
    ) {
        UsuarioToReporteProcessor p = new UsuarioToReporteProcessor();
        p.setSoloActivos(Boolean.parseBoolean(soloActivos));
        return p;
    }

    // === WRITER base (para un archivo) ===
    @Bean
    public FlatFileItemWriter<UsuarioReporteCsvDTO> singleCsvWriter() {
        BeanWrapperFieldExtractor<UsuarioReporteCsvDTO> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(CSV_FIELDS);

        DelimitedLineAggregator<UsuarioReporteCsvDTO> agg = new DelimitedLineAggregator<>();
        agg.setDelimiter(CSV_DELIMITER);
        agg.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<UsuarioReporteCsvDTO>()
                .name(WRITER_SINGLE_NAME)
                .lineAggregator(agg)
                .headerCallback(w -> w.write(CSV_HEADER))
                .encoding(CSV_ENCODING)
                .append(false)    // cada archivo nuevo empieza limpio
                .saveState(true)  // soporta restart
                .build();
    }

    // === MULTI WRITER: parte en varios archivos por límite de items ===
    @Bean
    @StepScope
    public MultiResourceItemWriter<UsuarioReporteCsvDTO> multiWriter(
            @Value(SPEL_OUTPUT_PREFIX) String outputPrefix,
            FlatFileItemWriter<UsuarioReporteCsvDTO> singleCsvWriter
    ) {
        // outputPrefix ejemplo: C:/Users/PC/Desktop/USUARIOS_20250825
        return new MultiResourceItemWriterBuilder<UsuarioReporteCsvDTO>()
                .name(WRITER_MULTI_NAME)
                .delegate(singleCsvWriter)
                .itemCountLimitPerResource(50000) // ajusta el tamaño por archivo
                .resource(new FileSystemResource(outputPrefix)) // base (sin extensión)
                .resourceSuffixCreator(index -> String.format(RESOURCE_SUFFIX_FORMAT, index + 1))
                .saveState(true)
                .build();
    }

    // === LISTENER de errores (skips) → *_errores.csv ===
    @Bean
    @StepScope
    public CsvErroresSkipListener erroresSkipListener(
            @Value(SPEL_OUTPUT_PREFIX) String outputPrefix
    ) {
        Path errorPath = Paths.get(outputPrefix + ERROR_SUFFIX);
        return new CsvErroresSkipListener(errorPath);
    }

    // === STEP (chunk normal + tolerancia a fallos + multi-writer) ===
    @Bean
    public Step exportUsuariosStep(JobRepository jobRepository,
                                   PlatformTransactionManager tx,
                                   RepositoryItemReader<UsuarioEntity> reader,
                                   @Qualifier(PROCESSOR_BEAN_NAME)
                                   ItemProcessor<UsuarioEntity, UsuarioReporteCsvDTO> processor,
                                   MultiResourceItemWriter<UsuarioReporteCsvDTO> multiWriter,
                                   CsvErroresSkipListener erroresSkipListener) {

        return new StepBuilder(STEP_NAME, jobRepository)
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
                        String prefix = stepExecution.getJobParameters().getString(PARAM_OUTPUT_PREFIX);

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
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(exportUsuariosStep)
                .listener(new JobExecutionListenerSupport() {
                    @Override public void beforeJob(JobExecution jobExecution) {

                    }
                    @Override public void afterJob(JobExecution jobExecution) {
                        long read = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
                        long write = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
                        long filter = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getFilterCount).sum();
                        long total = usuarioRepository.count();
                        long activos = usuarioRepository.countByActivoTrue();



                    }
                })
                .build();
    }
}
