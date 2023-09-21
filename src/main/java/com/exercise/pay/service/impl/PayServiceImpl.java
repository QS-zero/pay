package com.exercise.pay.service.impl;

import com.exercise.pay.dao.PayInfoMapper;
import com.exercise.pay.enums.PayPlatformEnum;
import com.exercise.pay.pojo.PayInfo;
import com.exercise.pay.service.IPayService;
import com.google.gson.Gson;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 发起/创建通知
 */

@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    private final static String QUEUE_PAY_NOTIFY = "payNotify";

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private PayInfoMapper payInfoMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;



    @Override
    public PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum) {
        if (!(bestPayTypeEnum == BestPayTypeEnum.WXPAY_NATIVE)) {
            throw new RuntimeException("暂时不支持该类型支付");
        }

        //写入数据库
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),
                OrderStatusEnum.NOTPAY.name(),
                amount);
        payInfoMapper.insertSelective(payInfo);


        PayRequest request = new PayRequest();
        request.setOrderName("QS-zero-test-payment");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(bestPayTypeEnum);

        PayResponse response = bestPayService.pay(request);

        log.info("发起支付 request:{}", request);
        return response;
    }

    /**
     * 异步通知处理
     * @param notifyData
     */
    @Override
    public String asyncNotify(String notifyData) {
        //签名校验
        PayResponse response = bestPayService.asyncNotify(notifyData);
        log.info("异步通知 notifyData={}", response);

        //金额校验（从数据库查订单）
        //发生严重情况（正常不会发生的）发出报警，钉钉或者短信
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(response.getOrderId()));
        if (payInfo == null) {
            throw new RuntimeException("通过orderNo查到的数据喂null");
        }
        if (!payInfo.getPayPlatform().equals(OrderStatusEnum.SUCCESS.name())) {
            //如果订单状态不是已支付
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(response.getOrderAmount())) != 0) {
                //告警
                throw new RuntimeException("异步通知金额与数据库不一致 orderNo=" + response.getOrderId());
            }
            //修改订单状态（支付状态）
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(response.getOutTradeNo());
            payInfo.setUpdateTime(null);
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        //TODO 发送MQ消息---> pay发送,mall接收
        amqpTemplate.convertAndSend(QUEUE_PAY_NOTIFY, new Gson().toJson(payInfo));

        //告诉微信成功接受，不必再通知
        return "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
    }

    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }
}
