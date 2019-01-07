package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.order.service.OrderItemService;
import com.pinyougou.vo.PageResult;
import com.pinyougou.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/orderItem")
@RestController
public class OrderItemController {

    @Reference
    private OrderItemService orderItemService;

    @RequestMapping("/findAll")
    public List<TbOrderItem> findAll() {
        return orderItemService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(value = "page", defaultValue = "1")Integer page,
                               @RequestParam(value = "rows", defaultValue = "10")Integer rows) {
        return orderItemService.findPage(page, rows);
    }

    @PostMapping("/add")
    public Result add(@RequestBody TbOrderItem orderItem) {
        try {
            orderItemService.add(orderItem);
            return Result.ok("增加成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("增加失败");
    }

    @GetMapping("/findOne")
    public TbOrderItem findOne(Long id) {
        return orderItemService.findOne(id);
    }

    @PostMapping("/update")
    public Result update(@RequestBody TbOrderItem orderItem) {
        try {
            orderItemService.update(orderItem);
            return Result.ok("修改成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            orderItemService.deleteByIds(ids);
            return Result.ok("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("删除失败");
    }

    /**
     * 分页查询列表
     * @param orderItem 查询条件
     * @param page 页号
     * @param rows 每页大小
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody  TbOrderItem orderItem, @RequestParam(value = "page", defaultValue = "1")Integer page,
                               @RequestParam(value = "rows", defaultValue = "10")Integer rows) {
        return orderItemService.search(page, rows, orderItem);
    }

}
