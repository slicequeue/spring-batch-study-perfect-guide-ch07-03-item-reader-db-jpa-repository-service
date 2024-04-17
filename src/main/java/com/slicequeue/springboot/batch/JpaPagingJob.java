package com.slicequeue.springboot.batch;

import com.slicequeue.springboot.batch.domain.Customer;
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
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.Collections;

/**
 * 하이버네이트 대신 JPA 사용
 * - spring-bootstarter-data-jpa 를 사용하는 모든 애플리케이션에는 스프링 부트 스타터를 사용할 때는 커스텀 BatchConfigurer 구현체를 생성할 필요 조차 없음
 * - 스프링 부트가 하이버네이트 버전과 유하산 JpaTransactionManager 구성을 개발자 대신 처리함
 * - 하이버네이트 예제에서 처럼 JPA 애너테이션을 사용하여 별도 매핑작업 필요하지 않음
 * JPA 예제에서는 커서 기법의 데이터베이스 접근을 지원하지 않음. 페이징만 제공하며 JpaPagingItemReader 사용함 이 부분만 주의 하면 됨
 */
//@EnableBatchProcessing
//@SpringBootApplication
public class JpaPagingJob {

    static class CustomerByCityQueryProvider extends AbstractJpaQueryProvider {
        private String cityName;

        public Query createQuery() {
            EntityManager manager = getEntityManager();
			// JPA 쿼리 생성
            Query query = manager.createQuery(
                    "select c from Customer " +
                            "c where c.city = :city");

			query.setParameter("city", cityName);

			return query;
        }

		@Override
		public void afterPropertiesSet() throws Exception {
			Assert.notNull(cityName, "City name is required");
		}

		public void setCityName(String cityName) {
			this.cityName = cityName;
		}
	}

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public JobParametersValidator validator() {
        return new DefaultJobParametersValidator(
                new String[]{"city"},
                new String[]{"run.id"}
        );
    }


    @Bean
    @StepScope
    public JpaPagingItemReader<Customer> customerJpaPagingItemReader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{jobParameters['city']}") String city
    ) {
        return new JpaPagingItemReaderBuilder<Customer>()
                .name("customerJpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory) // 하이버네이트와 다르게 세션 만들 필요 없이 엔티티팩토리만 넘겨줌
//                .queryString("select c from Customer c where c.city = :city") // 쿼리 설정1: JPQL 문법으로 작성 하거나 또는 아래와 같이 QueryProvider 사용하여 처리
				.queryProvider(new CustomerByCityQueryProvider()) 				// 쿼리 설정2: QueryProvider 주입하거나 위와 같이 queryString 이용하여 직접 처리
                .parameterValues(Collections.singletonMap("city", city))
//                .pageSize(...) // 미지정시 private int pageSize = 10;
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
                .reader(customerJpaPagingItemReader(null, null))
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
        SpringApplication.run(JpaPagingJob.class, "city=Springfield");
    }

}
