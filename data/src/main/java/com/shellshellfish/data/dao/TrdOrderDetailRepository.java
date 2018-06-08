package com.shellshellfish.data.dao;

import com.shellshellfish.data.model.TrdOrderDetail;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TrdOrderDetailRepository extends PagingAndSortingRepository<TrdOrderDetail, Long> {



  List<TrdOrderDetail> findByOrderIdInAndTradeApplySerialIsNotNull(List<String> orders);

  List<TrdOrderDetail> findAllByTradeApplySerialIsNotNull();

}
