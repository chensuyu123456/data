package com.shellshellfish.data;

import com.shellshellfish.data.dao.TrdOrderDetailRepository;
import com.shellshellfish.data.model.MongoUiTrdLog;
import com.shellshellfish.data.model.TrdOrderDetail;
import com.shellshellfish.data.service.SynchroTrdDetail;
import java.math.BigDecimal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;


@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
public class DataApplicationTests {

  @Autowired
  MongoTemplate template;
  @Autowired
  TrdOrderDetailRepository trdOrderDetailRepository;

  @Autowired
  SynchroTrdDetail detail;

  @Test
  public void test() {
    System.out.println(template.getDb().getName());
    MongoUiTrdLog trdLog = new MongoUiTrdLog();
    trdLog.setApplySerial("1");
    trdLog.setFundCode("1");
    template.save(trdLog, "ui_trdlog");
    System.out.println(trdLog.getId());
  }

  @Test
  public void test02() {
    Optional<TrdOrderDetail> trdOrderDetail = trdOrderDetailRepository.findById(2L);
    System.out.println(trdOrderDetail.get().getUserId());
  }

  @Test
  public void test03() {
    boolean synchrodata = detail.synchrodata();
    System.out.println(synchrodata);
  }

  @Test
  public void testCleat() {
    detail.clearInsertData();
  }

  @Test
  public void test05() {
    BigDecimal bigDecimal = new BigDecimal("100");
    System.out.println(getLongNumWithMul100(bigDecimal.toString()));
  }

  public static Long getLongNumWithMul100(String originNum) {

    BigDecimal origBigD = new BigDecimal(originNum);
    return origBigD.divide(BigDecimal.valueOf(100)).longValue();

  }

  @Test
  public void test07() {
    detail.sybchroOrderId();
  }


}
