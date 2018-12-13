package com.pinyougou.sellergoods.service;

import com.pinyougou.pojo.TbBrand;
import com.pinyougou.servlce.BaseService;
import com.pinyougou.vo.PageResult;


import java.io.Serializable;
import java.util.List;

public interface BrandService extends BaseService<TbBrand> {
    public List<TbBrand> queryAll();

    List<TbBrand> testPage(Integer page,Integer rows);

    PageResult search(TbBrand brand, Integer page, Integer rows);

}
