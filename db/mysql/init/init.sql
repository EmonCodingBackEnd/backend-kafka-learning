-- --------------------------------------------------------------------------------
--   ___         _  _
--  |_ _| _ __  (_)| |_
--   | | | '_ \ | || __|
--   | | | | | || || |_
--  |___||_| |_||_| \__|
--
-- 初始化数据脚本
-- 要求：
--    1、保证sql可以重复执行，也就是insert之前先delete（删除表还是某一条，根据需要而定）
--    2、sql关键字推荐大写
-- --------------------------------------------------------------------------------

-- 数据文件
insert into t_emp
values (1, '张三', 23, '男', '123456@qq.com', 1),
       (2, '李四', 32, '女', '123456@qq.com', 2),
       (3, '王五', 12, '男', '123456@qq.com', 3),
       (4, '赵六', 34, '女', '123456@qq.com', 1),
       (5, '田七', 28, '男', '123456@qq.com', 2);

insert into t_dept
values (1, 'A'),
       (2, 'B'),
       (3, 'C');

insert into mp_user(id, name, age, email)
values (1, 'Jone', 18, 'test1@baomidou.com'),
       (2, 'Jack', 20, 'test2@baomidou.com'),
       (3, 'Tom', 28, 'test3@baomidou.com'),
       (4, 'Sandy', 21, 'test4baomidou.com'),
       (5, 'Billie', 24, 'test5@baomidou.com');

insert into mp_product(id, name, price)
values (1, '外星人笔记本', 100);
