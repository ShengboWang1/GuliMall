# GuliMall
分布式电商秒杀项目 
此项目为前后端分离的分布式电商项目，基于 Spring Boot + Mybatis + Spring Cloud + Spring Cloud Alibaba 框架，MySQL 数据持久化，主要通过 Redis 缓存、异步与 RabbitMQ 实现系统的高并发，部署 MySQL 和 Redis 集群实现服务的高可用，最终实现商城的商品上架，购物车，订单，结算，库存与秒杀等业务 

项目亮点
• 使用 Nginx 实现动静分离，Spring Cloud Gateway 实现业务网关，配合 Sentinel 进行网关层限流
• 将业务划分成不同微服务部署，Nacos 实现配置中心与注册中心，OpenFeign 实现微服务间远程调用 • 使用 Redis 缓存热点数据，降低数据库压力并保证吞吐量;Redis 结合 Token 令牌实现幂等性
• 使用 Redisson 分布式锁解决缓存击穿，定时任务重复问题，Semaphore 模拟商品库存快速扣减
• 使用异步编排加快响应速度，使用线程池控制资源，减少开销
• 使用 RabbitMQ 可靠消息实现柔性事物最终一致性，同时实现解耦，削峰，提升系统吞吐量
• 使用 Sleuth + Zipkin 链路追踪排查找到异常点，Sentinel 针对性设置熔断与降级，维持系统稳定性
• 部署一主二从 MySQL 主从复制集群，使用 ShardingSphere 实现读写分离，提升数据库并发负载能力 • 部署三主三从的 Redis Cluster 集群，实现数据分区与 Redis 服务的高可用
