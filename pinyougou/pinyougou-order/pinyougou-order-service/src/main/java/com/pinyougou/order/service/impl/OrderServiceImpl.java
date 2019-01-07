package com.pinyougou.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.common.util.IdWorker;
import com.pinyougou.mapper.OrderItemMapper;
import com.pinyougou.mapper.OrderMapper;
import com.pinyougou.mapper.PayLogMapper;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.Cart;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl extends BaseServiceImpl<TbOrder> implements OrderService {
    //redis 中购物车数据的key
    private static final String REDIS_CART_LIST = "CART_LIST";

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;

    @Override
    public PageResult search(Integer page, Integer rows, TbOrder order) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbOrder.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(order.get***())){
            criteria.andLike("***", "%" + order.get***() + "%");
        }*/

        List<TbOrder> list = orderMapper.selectByExample(example);
        PageInfo<TbOrder> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     *将购物车列表中的商品保存订单基本.明细信息和支付日志信息
     * @param order 订单基本信息
     * @return  支付业务id
     */
    @Override
    public String addOrder(TbOrder order) {
        //支付日志id,若非微信支付可以为空
        String outTradeNo = "";
        //1.获取用户对应的购物车列表
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps(REDIS_CART_LIST).get(order.getUserId());
        if (cartList != null && cartList.size() > 0){
            //2.遍历购物车列表的每个购物车对应生成一个订单和多个对应的订单明细
            double totalFee = 0.0;//本次应该支付总金额
            String orderids = "";//本次交易的订单ID集合
            for (Cart cart: cartList){

                TbOrder tbOrder = new TbOrder();
                tbOrder.setOrderId(idWorker.nextId());
                tbOrder.setSourceType(order.getSourceType());//订单来源
                tbOrder.setUserId(order.getUserId());//购买者
                tbOrder.setStatus("1");//未付款
                tbOrder.setPaymentType(order.getPaymentType());//支付类型
                tbOrder.setReceiverMobile(order.getReceiverAreaName());//收货人手机号
                tbOrder.setReceiver(order.getReceiver());//收货人
                tbOrder.setCreateTime(new Date());//订单创建时间
                tbOrder.setUpdateTime(order.getUpdateTime());//订单更新时间
                tbOrder.setSellerId(cart.getSellerId());//卖家

                //本笔订单的支付总金额
                double payment = 0.0;
                //本笔订单的明细
                for(TbOrderItem orderItem : cart.getOrderItemList()){
                    orderItem.setId(idWorker.nextId());
                    orderItem.setOrderId(tbOrder.getOrderId());
                    //累计本笔订单的总金额
                    payment += orderItem.getTotalFee().doubleValue();
                    orderItemMapper.insertSelective(orderItem);
                }

                tbOrder.setPayment(new BigDecimal(payment));
                orderMapper.insertSelective(tbOrder);

                //记录订单id
                if (orderids.length() > 0){
                    orderids += "," + tbOrder.getOrderId();
                }else {
                    orderids = tbOrder.getOrderId().toString();
                }
                //累计本次所有订单额总金额
                totalFee += payment;
            }
            //3.如果是微信支付的话则需要生成支付日志保存到数据库中和redis 中设置5分钟过期
            if ("1".equals(order.getPaymentType())){
                outTradeNo = idWorker.nextId()+"";
                TbPayLog tbPayLog = new TbPayLog();
                tbPayLog.setOutTradeNo(outTradeNo);
                tbPayLog.setTradeState("0");//未支付
                tbPayLog.setUserId(order.getUserId());
                tbPayLog.setCreateTime(new Date());
                tbPayLog.setTotalFee((long)(totalFee*100));//总金额,取整
                tbPayLog.setOrderList(orderids);//本次订单id集合
                payLogMapper.insertSelective(tbPayLog);
            }
            //删除用户对应的购物车列表
            redisTemplate.boundHashOps(REDIS_CART_LIST).delete(order.getUserId());
        }

        return outTradeNo;
    }

    @Override
    public TbPayLog findPayLogByOutTradeNo(String outTradeNo) {
        return payLogMapper.selectByPrimaryKey(outTradeNo);
    }

    /**
     * 根据支付日志id 更新订单支付状态和支付日志状态为已支付
     * @param outTradeNo 支付日志id
     * @param transaction_id 微信中对应的支付id
     */
    @Override
    public void updateOrderStatus(String outTradeNo, String transaction_id) {
            //1.更新支付日志支付状态
        TbPayLog payLog = findPayLogByOutTradeNo(outTradeNo);
        payLog.setTradeState("1");//已支付
        payLog.setPayTime(new Date());
        payLog.setTransactionId(transaction_id);
        payLogMapper.updateByPrimaryKeySelective(payLog);

        //2.更新支付日志中对应的每一笔订单的支付状态
        String[] orderIds = payLog.getOrderList().split(",");

        TbOrder order = new TbOrder();
        order.setPaymentTime(new Date());
        order.setStatus("2");

        Example example = new Example(TbOrder.class);
        example.createCriteria().andIn("orderId", Arrays.asList(orderIds));

        orderMapper.updateByExampleSelective(order,example);
    }
}
