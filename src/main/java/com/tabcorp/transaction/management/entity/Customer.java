package com.tabcorp.transaction.management.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("customer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column("customer_id")
    private Integer customerId;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("age")
    private Integer age;

    @Column("email")
    private String email;

    @Column("location")
    private String location;
}
