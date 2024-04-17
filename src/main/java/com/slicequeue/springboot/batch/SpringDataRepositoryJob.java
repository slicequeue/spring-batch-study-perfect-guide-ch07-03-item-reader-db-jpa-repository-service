package com.slicequeue.springboot.batch;

import com.slicequeue.springboot.batch.domain.Customer;
import com.slicequeue.springboot.batch.repository.CustomerRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;

import java.util.Collections;

@EnableBatchProcessing
@SpringBootApplication
public class SpringDataRepositoryJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public JobParametersValidator validator() {
        return new DefaultJobParametersValidator(new String[]{"city"}, new String[]{"run.id"});
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Customer> customerRepositoryItemReader( // RepositoryItemReader<Customer> spring-data-jpa 리포 전용 아이템 리더, 아래 return 부 전용빌더 RepositoryItemReaderBuilder 로 생성
            CustomerRepository repository,
            @Value("#{jpaParameters['city']}") String city) {
        return new RepositoryItemReaderBuilder<Customer>()
                .name("customerRepositoryItemReader")                               // 1. 리더명지정 (재시작이 가능하도록)
                .arguments(Collections.singletonList(city))                         // 2. 매개변수 지정 (Pageable 제외)
                .methodName("findByCity")                                           // 3. 호출할 메서드 이름
                .repository(repository)                                             // 4. 리포지터리 구현체
                .sorts(Collections.singletonMap("lastName", Sort.Direction.ASC))    // 5. 필요한 정렬 방법 (Pageable 객체에 반영)
                .build();
    }

    @Bean
    public ItemWriter<Customer> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
                .<Customer, Customer>chunk(10)
                .reader(customerRepositoryItemReader(null, null))
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job-jpa-paging")
                .validator(validator())
                .incrementer(new RunIdIncrementer())
                .start(copyFileStep())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringDataRepositoryJob.class, "city=Springfield");
    }


}
