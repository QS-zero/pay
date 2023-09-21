package com.exercise.pay.service.impl;

import com.exercise.pay.PayApplicationTests;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;


@RunWith(SpringRunner.class)
@SpringBootTest

public class PayServiceImplTest extends PayApplicationTests {
    @Autowired
    private PayServiceImpl payServiceImpl;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    public void create() {
        payServiceImpl.create("13131", BigDecimal.valueOf(0.01), BestPayTypeEnum.WXPAY_NATIVE);

    }

    @Test
    public void sendMQMsg(){
        amqpTemplate.convertAndSend("payNotify", "Hello mall lie");
    }
}












