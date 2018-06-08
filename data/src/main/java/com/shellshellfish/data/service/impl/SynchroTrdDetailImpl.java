package com.shellshellfish.data.service.impl;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.result.DeleteResult;
import com.shellshellfish.data.dao.TrdOrderDetailRepository;
import com.shellshellfish.data.model.MongoUiTrdLog;
import com.shellshellfish.data.model.TrdOrderDetail;
import com.shellshellfish.data.service.SynchroTrdDetail;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SynchroTrdDetailImpl implements SynchroTrdDetail {

  private static final Logger logger = LoggerFactory.getLogger(SynchroTrdDetailImpl.class);
  private static final String ROLLBACK_FILENAME = "rollback-161";
  private static final String ENCODING = "UTF-8";

  @Autowired
  private MongoTemplate template;

  @Autowired
  private TrdOrderDetailRepository repository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Override
  public boolean synchrodata() {
    boolean result = true;
    try {
      logger.info("synchrodata start");
      //System.out.println(getMongoTrdDetailsCount());
      long count = getMongoTrdDetailsCount();
      logger.info("init mongodb trdlogs count : {}", count);
      // List<String> orderIds = repository.getOrderIds();
      //System.out.println(Arrays.toString(orderIds.toArray()));
      System.out.println("================================================================");
      Map<String, List<TrdOrderDetail>> stringListMap = this.trdOrderConvertMap();
      // String s = JSON.toJSONString(stringListMap, true);
      List<MongoUiTrdLog> order_id = template
          .find(new Query(Criteria.where("order_id").exists(true)), MongoUiTrdLog.class,
              "ui_trdlog");
      Map<String, List<MongoUiTrdLog>> collect = order_id.stream()
          .collect(Collectors.groupingBy(o -> o.getFundCode() + "-" + o.getOrderId()));
      Set<String> allOperations = collect.keySet();
      List<MongoUiTrdLog> funds = new ArrayList<>();
      for (String operation : allOperations) {
        List<MongoUiTrdLog> mongoUiTrdLogs = collect.get(operation);
        Collections.sort(mongoUiTrdLogs, (o1, o2) -> {
          Long dateLong1 = getDateLong(o1);
          Long dateLong2 = getDateLong(o2);
//                    Long map1value = (Long) o1.get("dateLong");
//                    Long map2value = (Long) o2.get("dateLong");
//                    return map2value.compareTo(map1value);
          return dateLong2.compareTo(dateLong1);
        });
        funds.add(mongoUiTrdLogs.get(0));

      }
      Map<String, List<MongoUiTrdLog>> mongoCollect = funds.stream()
          .collect(Collectors.groupingBy(o -> o.getOrderId()));
      List<String> _ids = this.synchrodataStatus(mongoCollect, stringListMap);
      this.synchrodataFunds(mongoCollect, stringListMap, _ids);
      String fileName = "rollback";
      URL rollback = SynchroTrdDetailImpl.class.getResource("");
      String file = rollback.getFile();
      FileUtils.writeLines(new File(file, ROLLBACK_FILENAME), ENCODING, _ids, true);


    } catch (Exception e) {
      e.printStackTrace();

      result = false;
    }





    //List<MongoUiTrdLog> mongoUiTrdLogs = mongoCollect.get("622202156000001527325887283");
    //List<TrdOrderDetail> trdOrderDetails = stringListMap.get("622202156000001527325887283");

    //System.out.println(status);

    //System.out.println(i1 == i2);
    //System.out.println(stringListMap.size());
    //System.out.println(mongoCollect.size());
    //System.out.println("i1："+i1);
    //System.out.println("i2: "+i2);

//        for(String string : strings){
//            List<MongoUiTrdLog> string1 = collect1.get("string");
//            List<MongoUiTrdLog> mongoUiTrdLogs = collect.get(string);
//            for(int j=0;j<string1.size();j++){
//                MongoUiTrdLog out = string1.get(j);
//                for(int k=0;k<mongoUiTrdLogs.size();k++){
//                    MongoUiTrdLog inner = mongoUiTrdLogs.get(k);
//                    if(!out.getFundCode().equals(inner.getFundCode())){
//                            continue;
//                    }else{
//                        //判断逻辑
//                        i++;
//                    }
//                }
//                if(i==20){
//                    template.insert(adds,"ui_trdlog");
//                    i=0;
//                    adds.clear();
//                }
//            }
//        }
//       if(adds.size()>0){
//           template.insert(adds,"ui_trdlog");
//       }

    //System.out.println(s);
    //System.out.println(orderIds.size());

    return result;
  }






  private long getMongoTrdDetailsCount() {
    Criteria order_id = Criteria.where("order_id").exists(true);
    Query query = new Query(order_id);
    long count = template.count(query, "ui_trdlog");
    return count;
  }

  private Map<String, List<TrdOrderDetail>> trdOrderConvertMap() {
    List<TrdOrderDetail> trdOrderDetails = repository.findAllByTradeApplySerialIsNotNull();
    Map<String, List<TrdOrderDetail>> collect = trdOrderDetails.stream()
        .collect(Collectors.groupingBy(t -> t.getOrderId()));

    return collect;
  }

  @Override
  public void clearInsertData() {
    URL rollback = SynchroTrdDetailImpl.class.getResource("");
    String file = rollback.getFile();
    File f = new File(file, ROLLBACK_FILENAME);
    if (!f.exists()) {
      return;
    }
    List<String> datas = null;
    try {
      datas = FileUtils.readLines(f, ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
    //System.out.println(Arrays.toString(datas.toArray()));
    Criteria id = Criteria.where("_id").in(datas);
    Query query = new Query(id);
    DeleteResult ui_trdlog = template.remove(query, "ui_trdlog");
    System.out.println(ui_trdlog.getDeletedCount());
    if (f.exists()) {
      f.delete();
    }

  }


  private Long getDateLong(MongoUiTrdLog mongoUiTrdLog) {
    Long date = 0L;
    if (mongoUiTrdLog.getTradeDate() != null) {
      date = mongoUiTrdLog.getTradeDate();
    } else if (mongoUiTrdLog.getLastModifiedDate() != null) {
      date = mongoUiTrdLog.getLastModifiedDate();
    }
    return date;
  }

  private List<String> synchrodataStatus(Map<String, List<MongoUiTrdLog>> mongoData,
      Map<String, List<TrdOrderDetail>> mysqlData) {
    List<String> _ids = new LinkedList<>();
    Set<String> mongoKeys = mongoData.keySet();
    List<MongoUiTrdLog> insertData = new ArrayList<>();
    for (String mongoKey : mongoKeys) {
      List<MongoUiTrdLog> mongoUiTrdLogs = mongoData.get(mongoKey);
      if (!mysqlData.containsKey(mongoKey)) {
        throw new RuntimeException("mongodb中有mysql表中没有的orderId");
      }
      Map<String, List<TrdOrderDetail>> collect = mysqlData.get(mongoKey).stream()
          .collect(Collectors.groupingBy(o -> o.getFundCode()));
      Map<String, List<MongoUiTrdLog>> collect1 = mongoUiTrdLogs.stream()
          .collect(Collectors.groupingBy(o -> o.getFundCode()));
//      if (mongoUiTrdLogs.size() < collect.size()) {
//        Set<String> strings = collect.keySet();
//        for (String string : strings) {
//          if (!collect1.containsKey(string)) {
//            MongoUiTrdLog mongoUiTrdLog = new MongoUiTrdLog();
//            TrdOrderDetail trdOrderDetail = collect.get(string).get(0);
//            mongoUiTrdLog.setTradeDate(trdOrderDetail.getBuysellDate());
//            mongoUiTrdLog.setId(null);
//            mongo
//            //  mongoUiTrdLog.setAmount(trdOrderDetail.getPayAmount());
//            insertData.add(mongoUiTrdLog);
//          }
//        }
//
//
//      }

      for (MongoUiTrdLog mongoUiTrdLog : mongoUiTrdLogs) {
        if (!collect.containsKey(mongoUiTrdLog.getFundCode())) {
          throw new RuntimeException("mongodb中有mysql中同一操作中没有的fund_code");
        }
        int mongoTrdStatus = mongoUiTrdLog.getTradeStatus();

        int mysqlTrdStatus = collect.get(mongoUiTrdLog.getFundCode()).get(0).getOrderDetailStatus();
        if (mongoTrdStatus == 1 || mongoTrdStatus == 4) {
          mongoTrdStatus = 1;
        } else {
          mongoTrdStatus = 2;
        }
        if (mysqlTrdStatus == 1 || mysqlTrdStatus == 4) {
          mysqlTrdStatus = 1;
        } else {
          mysqlTrdStatus = 2;
        }
        if (mysqlTrdStatus > mongoTrdStatus) {
          MongoUiTrdLog lackState = new MongoUiTrdLog();
          BeanUtils.copyProperties(mongoUiTrdLog, lackState);
          lackState.setLastModifiedDate(lackState.getLastModifiedDate() + 1000);
          lackState.setId(null);
          lackState.setTradeDate(lackState.getTradeDate() + 1000);
          lackState.setTradeStatus(
              collect.get(mongoUiTrdLog.getFundCode()).get(0).getOrderDetailStatus());
          insertData.add(lackState);
        }


      }
      if (insertData.size() >= 20) {
        template.insert(insertData, "ui_trdlog");
        for (int i = 0; i < insertData.size(); i++) {
          _ids.add(insertData.get(i).getId());
        }
        insertData.clear();
      }


    }
    if (insertData.size() > 0) {
      template.insert(insertData, "ui_trdlog");
      for (int i = 0; i < insertData.size(); i++) {
        _ids.add(insertData.get(i).getId());
      }
      insertData.clear();
    }

    return _ids;
  }


  public void synchrodataFunds(Map<String, List<MongoUiTrdLog>> mongoData,
      Map<String, List<TrdOrderDetail>> mysqlData, List<String> _ids) {
    Set<String> mongoDataKeys = mongoData.keySet();
    List<MongoUiTrdLog> insertData = new LinkedList<>();
    for (String mongoDataKey : mongoDataKeys) {
      List<MongoUiTrdLog> mongoUiTrdLogs = mongoData.get(mongoDataKey);
      List<TrdOrderDetail> trdOrderDetails = mysqlData.get(mongoDataKey);
      Map<String, List<MongoUiTrdLog>> mongoCollect = mongoUiTrdLogs.stream()
          .collect(Collectors.groupingBy(o -> o.getFundCode()));
      Map<String, List<TrdOrderDetail>> mysqlCollect = trdOrderDetails.stream()
          .collect(Collectors.groupingBy(o -> o.getFundCode()));
      Set<String> mongoFunds = mongoCollect.keySet();
      Set<String> mysqlFunds = mysqlCollect.keySet();
      if (mongoUiTrdLogs.size() != trdOrderDetails.size()) {
        System.out.println(mongoDataKey);
        for (String mysqlFund : mysqlFunds) {
          if (mongoCollect.containsKey(mysqlFund)) {
            continue;
          }
          MongoUiTrdLog mongoUiTrdLog = convertTrdDetailsToMongoUiTrdLog(
              mysqlCollect.get(mysqlFund).get(0));
          insertData.add(mongoUiTrdLog);
          //System.out.println(11111);
          System.out.println(mongoUiTrdLog);
        }

      }
      if (insertData.size() >= 20) {
        template.insert(insertData, "ui_trdlog");
        for (int i = 0; i < insertData.size(); i++) {
          _ids.add(insertData.get(i).getId());
        }
        insertData.clear();
      }

    }
    if (insertData.size() > 0) {
      template.insert(insertData, "ui_trdlog");
      for (int i = 0; i < insertData.size(); i++) {
        _ids.add(insertData.get(i).getId());
      }
      insertData.clear();
    }

  }

  @Override
  public void sybchroOrderId() {

  }

  private MongoUiTrdLog convertTrdDetailsToMongoUiTrdLog(TrdOrderDetail trdOrderDetail) {
    MongoUiTrdLog mongoUiTrdLog = new MongoUiTrdLog();
    mongoUiTrdLog.setId(null);
    mongoUiTrdLog.setApplySerial(trdOrderDetail.getTradeApplySerial());
    mongoUiTrdLog.setTradeStatus(trdOrderDetail.getOrderDetailStatus());
    mongoUiTrdLog.setOperations(trdOrderDetail.getTradeType());
    mongoUiTrdLog.setTradeDate(trdOrderDetail.getBuysellDate());
    mongoUiTrdLog.setFundCode(trdOrderDetail.getFundCode());
    mongoUiTrdLog.setLastModifiedDate(trdOrderDetail.getUpdateDate());
    mongoUiTrdLog.setUserProdId(trdOrderDetail.getUserProdId());
    mongoUiTrdLog.setUserId(trdOrderDetail.getUserId());
    mongoUiTrdLog.setOrderId(trdOrderDetail.getOrderId());
    mongoUiTrdLog.setTradeConfirmShare(trdOrderDetail.getFundNumConfirmed());
    mongoUiTrdLog.setTradeConfirmSum(trdOrderDetail.getFundSumConfirmed());
    mongoUiTrdLog.setTradeTargetShare(trdOrderDetail.getFundNum());
    mongoUiTrdLog.setTradeConfirmSum(trdOrderDetail.getFundSum());
    setAmount(mongoUiTrdLog);
    return mongoUiTrdLog;
  }


  public static Long getLongNumWithMul100(String originNum) {

    BigDecimal origBigD = new BigDecimal(originNum);
    return origBigD.multiply(BigDecimal.valueOf(100)).longValue();

  }

  /**
   * 输入为有小数点的数字的BigDecimal 输出为精确到百分之一的整型
   */
  private static Long getLongNumWithMul100(BigDecimal originNum) {
    return originNum.multiply(BigDecimal.valueOf(100)).longValue();
  }


  private void setAmount(MongoUiTrdLog mongoUiTrdLog) {
    Long sumFromLog = 0L;
    if (mongoUiTrdLog.getOperations() == 1) {
      if (mongoUiTrdLog.getTradeConfirmSum() != null
          && mongoUiTrdLog.getTradeConfirmSum() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeConfirmSum();
      } else if (mongoUiTrdLog.getTradeTargetSum() != null
          && mongoUiTrdLog.getTradeTargetSum() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeTargetSum();
      } else if (mongoUiTrdLog.getTradeConfirmShare() != null
          && mongoUiTrdLog.getTradeConfirmShare() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeConfirmShare();
      } else if (mongoUiTrdLog.getTradeTargetShare() != null
          && mongoUiTrdLog.getTradeTargetShare() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeTargetShare();
      } else if (mongoUiTrdLog.getAmount() != null) {
        sumFromLog = getLongNumWithMul100(mongoUiTrdLog.getAmount());
      } else {
        logger.error(
            "havent find trade money info for userProdId:{} and " + "fundCode:{}",
            mongoUiTrdLog.getUserProdId(), mongoUiTrdLog.getFundCode());
        sumFromLog = 0L;
      }
    } else {
      //if the log is of type sell record, we sum up the num values first
      if (mongoUiTrdLog.getTradeConfirmShare() != null
          && mongoUiTrdLog.getTradeConfirmShare() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeConfirmShare();
      } else if (mongoUiTrdLog.getTradeTargetShare() != null
          && mongoUiTrdLog.getTradeTargetShare() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeTargetShare();
      } else if (mongoUiTrdLog.getTradeConfirmSum() != null
          && mongoUiTrdLog.getTradeConfirmSum() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeConfirmSum();
      } else if (mongoUiTrdLog.getTradeTargetSum() != null
          && mongoUiTrdLog.getTradeTargetSum() > 0) {
        sumFromLog = mongoUiTrdLog.getTradeTargetSum();
      } else if (mongoUiTrdLog.getAmount() != null) {
        sumFromLog = getLongNumWithMul100(mongoUiTrdLog.getAmount());
      } else {
        logger.error(
            "havent find trade quantity info for userProdId:{} and " + "fundCode:{}",
            mongoUiTrdLog.getUserProdId(), mongoUiTrdLog.getFundCode());
        sumFromLog = 0L;
      }
    }
    mongoUiTrdLog.setAmount(getBigDecimalNumWithDiv100(sumFromLog));
    //map.put("amount", getBigDecimalNumWithDiv100(sumFromLog));
  }

  private static BigDecimal getBigDecimalNumWithDiv100(Long originNum) {
    return round(BigDecimal.valueOf(originNum).divide(BigDecimal.valueOf(100)), 2, true);
  }

  private static BigDecimal round(BigDecimal d, int scale, boolean roundUp) {
    int mode = (roundUp) ? BigDecimal.ROUND_UP : BigDecimal.ROUND_DOWN;
    return d.setScale(scale, mode);
  }


}
