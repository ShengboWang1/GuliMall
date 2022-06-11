package com.shengbo.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shengbo.gulimall.product.service.CategoryBrandRelationService;
import com.shengbo.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.product.dao.CategoryDao;
import com.shengbo.gulimall.product.entity.CategoryEntity;
import com.shengbo.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryDao categoryDao;
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //要查出所有的分类
        List<CategoryEntity> categoryEntities = categoryDao.selectList(null);
        //组装成父子的树形结构
        //2.1找到所有的1级分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return menu1.getSort() - menu2.getSort();
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenusByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单是否被别的地方引用
        //逻辑删除 不是真的物理删除 而是给他添加一个标志位 1表示显示 不删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPaths = findParentPath(catelogId, paths);
        Collections.reverse(parentPaths);
        return (Long[]) parentPaths.toArray(new Long[0]);

    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @Caching(evict = {
            @CacheEvict(value = {"category"}, key = "'getLevel1Catagories'"),
            @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
    })
    //@CacheEvict(value = {"category"}, allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
        this.update();
    }

    @Cacheable(value = {"category"}, key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Catagories() {
        System.out.println("getLevel1Categories...");
        //long l = System.currentTimeMillis();
        List<CategoryEntity> list = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
        //System.out.println("耗时：" + (System.currentTimeMillis() - l));
        return list;
    }

    //TODO 堆外内存溢出
    //springboot 2.0以后使用lettuce作为操作的客户端， 它使用netty进行网络通信
    //lettuce的bug导致netty堆外内存溢出 netty若没有指定堆外内存 默认使用-Xmx的设定值 -Xmx...
    //解决方案： 不能使用 -Dio.netty.maxDirectMemory只去调大堆外内存。
    //1.升级lettuce客户端
    //2.切换使用jedis客户端


    @Cacheable(value="category", key="#root.methodName")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库。。");
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查出所有1级分类
        List<CategoryEntity> level1Catagories = getParent_cid(selectList, 0L);

        //2.封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Catagories.stream().collect(Collectors.toMap(k -> k.getCatId().toString()
            , v -> {
                //1.拿到每一个的1级分类， 查到这个1级分类的所有2级分类
                List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                //2.封装上面的结果
                List<Catalog2Vo> Catalog2Vos = null;
                if (categoryEntities != null) {
                    Catalog2Vos = categoryEntities.stream().map(l2 -> {
                        Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //3.找3级分类
                        List<CategoryEntity> category2Entities = getParent_cid(selectList, l2.getCatId());
                        List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                        if (category2Entities != null) {
                            //2.封装成指定格式
                            catalog3Vos = category2Entities.stream().map(l3 -> {
                                Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catalog3Vo;
                            }).collect(Collectors.toList());
                            catalog2Vo.setCatalog3List(catalog3Vos);
                        }

                        return catalog2Vo;
                    }).collect(Collectors.toList());
                }

                return Catalog2Vos;
            }));
            return parent_cid;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        /**
         * 1.空结果缓存 解决缓存穿透
         * 2。 设置过期时间 解决缓存雪崩
         * 3。 枷锁 解决缓存击穿
         */
        //给缓存中放json字符串 拿出的json字符串 还要逆转为可用的对象类型【序列化与反序列化】

        //1.加入缓存逻辑 缓存中存的数据是json数据串
        // json的优点：跨语言跨平台兼容
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String catalogJSON = ops.get("catalogJSON");
        if (!StringUtils.hasText(catalogJSON)) {
            //2.缓存中没有 加入数据库
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();

            return catalogJsonFromDb;
        }
        //转为我们指定的类型
        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
        return result;

    }

    /**
     * 缓存数据一致性问题：
     * 双写模式
     * 失效模式
     */

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        //锁的名字。 锁的粒度越细 越快
        //锁的粒度：具体缓存的是某个数据 11号商品：product-11-lock
        RLock lock = redisson.getLock("CatalogJson-Lock");
        lock.lock();

        System.out.println("获取分布式锁成功..");
        //加锁成功...执行业务
        Map<String, List<Catalog2Vo>> datafromDb;
        try {
            datafromDb = getDatafromDb();
        } finally {
            lock.unlock();
        }
        return datafromDb;

    }

    private Map<String, List<Catalog2Vo>> getDatafromDb() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.hasText(catalogJSON)) {
            //若缓存不为空 直接返回
            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
            return result;
        }
        System.out.println("查数据库咯。。。");
        //.优化1 将数据库的多次查询变为一次
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查出所有1级分类
        List<CategoryEntity> level1Catagories = getParent_cid(selectList, 0L);

        //2.封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Catagories.stream().collect(Collectors.toMap(k -> k.getCatId().toString()
                , v -> {
                    //1.拿到每一个的1级分类， 查到这个1级分类的所有2级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                    //2.封装上面的结果
                    List<Catalog2Vo> Catalog2Vos = null;
                    if (categoryEntities != null) {
                        Catalog2Vos = categoryEntities.stream().map(l2 -> {
                            Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //3.找3级分类
                            List<CategoryEntity> category2Entities = getParent_cid(selectList, l2.getCatId());
                            List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                            if (category2Entities != null) {
                                //2.封装成指定格式
                                catalog3Vos = category2Entities.stream().map(l3 -> {
                                    Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catalog3Vo;
                                }).collect(Collectors.toList());
                                catalog2Vo.setCatalog3List(catalog3Vos);
                            }

                            return catalog2Vo;
                        }).collect(Collectors.toList());
                    }

                    return Catalog2Vos;
                }));
        //3.将查到的数据加入缓存 将对象转为json放在缓存中
        // com.alibaba.fastjson.JSON
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    //从数据库查询并封装分类数据
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        synchronized (this) {
            //得到锁以后应该再去缓存中确定一次， 若没有才继续查询
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if (StringUtils.hasText(catalogJSON)) {
                //若缓存不为空 直接返回
                Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
                return result;
            }
            System.out.println("查数据库咯。。。");
            //.优化1 将数据库的多次查询变为一次
            List<CategoryEntity> selectList = baseMapper.selectList(null);

            //1.查出所有1级分类
            List<CategoryEntity> level1Catagories = getParent_cid(selectList, 0L);

            //2.封装数据
            Map<String, List<Catalog2Vo>> parent_cid = level1Catagories.stream().collect(Collectors.toMap(k -> k.getCatId().toString()
                    , v -> {
                        //1.拿到每一个的1级分类， 查到这个1级分类的所有2级分类
                        List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                        //2.封装上面的结果
                        List<Catalog2Vo> Catalog2Vos = null;
                        if (categoryEntities != null) {
                            Catalog2Vos = categoryEntities.stream().map(l2 -> {
                                Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                                //3.找3级分类
                                List<CategoryEntity> category2Entities = getParent_cid(selectList, l2.getCatId());
                                List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                                if (category2Entities != null) {
                                    //2.封装成指定格式
                                    catalog3Vos = category2Entities.stream().map(l3 -> {
                                        Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                        return catalog3Vo;
                                    }).collect(Collectors.toList());
                                }
                                catalog2Vo.setCatalog3List(catalog3Vos);

                                return catalog2Vo;
                            }).collect(Collectors.toList());
                        }

                        return Catalog2Vos;
                    }));
            //3.将查到的数据加入缓存 将对象转为json放在缓存中
            // com.alibaba.fastjson.JSON
            String s = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
            return parent_cid;
        }


    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid() == parent_cid;
        }).collect(Collectors.toList());
        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());

        }).collect(Collectors.toList());
        return children;
    }

}