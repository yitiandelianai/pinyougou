package com.pinyougou.order.service;

import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

public interface OrderItemService extends BaseService<TbOrderItem> {

    PageResult search(Integer page, Integer rows, TbOrderItem orderItem);
}