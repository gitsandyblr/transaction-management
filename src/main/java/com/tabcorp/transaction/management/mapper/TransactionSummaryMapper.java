package com.tabcorp.transaction.management.mapper;

import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryRecord;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryRecord;
import reactor.core.publisher.Mono;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionSummaryMapper {

    @Mapping(source = "customer_id", target = "customerId", qualifiedByName = "longToInteger")
    @Mapping(source = "first_name", target = "firstName")
    @Mapping(source = "last_name", target = "lastName")
    @Mapping(source = "total_cost", target = "totalCost", qualifiedByName = "doubleToBigDecimal")
    CustomerTransactionSummaryDTO customerTransactionRecordToDto(CustomerTransactionSummaryRecord record);

    @Mapping(source = "product_code", target = "productCode")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "total_cost", target = "totalCost", qualifiedByName = "doubleToBigDecimal")
    ProductTransactionSummaryDTO productTransactionRecordToDto(ProductTransactionSummaryRecord record);
    
    /**
     * Reactive version of customerTransactionRecordToDto that can be used in reactive streams.
     * This avoids blocking operations when mapping in a reactive context.
     */
    default Mono<CustomerTransactionSummaryDTO> customerTransactionRecordToDtoReactive(CustomerTransactionSummaryRecord record) {
        return Mono.justOrEmpty(record).map(this::customerTransactionRecordToDto);
    }
    
    /**
     * Reactive version of productTransactionRecordToDto that can be used in reactive streams.
     * This avoids blocking operations when mapping in a reactive context.
     */
    default Mono<ProductTransactionSummaryDTO> productTransactionRecordToDtoReactive(ProductTransactionSummaryRecord record) {
        return Mono.justOrEmpty(record).map(this::productTransactionRecordToDto);
    }
    
    @Named("longToInteger")
    default Integer longToInteger(Long value) {
        return value != null ? value.intValue() : null;
    }
    
    @Named("doubleToBigDecimal")
    default BigDecimal doubleToBigDecimal(Double value) {
        if (value == null) {
            return null;
        } else if (value == 0.0) {
            return BigDecimal.ZERO;
        } else {
            // Create a BigDecimal from the string representation to preserve decimal places
            String valueStr = value.toString();
            BigDecimal result = new BigDecimal(valueStr);
            
            // For monetary values, ensure we have at least 2 decimal places
            if (result.scale() < 2) {
                result = result.setScale(2);
            }
            return result;
        }
    }
}
