package com.slicequeue.springboot.batch.repository;

import com.slicequeue.springboot.batch.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByCity(String city, Pageable pageRequest); // 리포의 메서드 페이징 사용하기 위해서는 Pageable pageRequest 매개변수를 꼭 추가해줘야함!!!

}
