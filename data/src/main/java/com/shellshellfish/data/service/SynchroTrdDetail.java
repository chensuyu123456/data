package com.shellshellfish.data.service;

import com.shellshellfish.data.model.MongoUiTrdLog;
import com.shellshellfish.data.model.TrdOrderDetail;
import java.util.List;
import java.util.Map;

public interface SynchroTrdDetail {

  boolean synchrodata();

  void clearInsertData();

  void synchrodataFunds(Map<String, List<MongoUiTrdLog>> mongoData,
      Map<String, List<TrdOrderDetail>> mysqlData, List<String> _ids);

  void sybchroOrderId();

}
