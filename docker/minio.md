MINIO搭建：

参考文档：
https://blog.csdn.net/BThinker/article/details/125412751

1. 创建目录

一个用来存放配置，一个用来存储上传文件的目录

外部挂载的配置文件（ /home/minio/config）
和存储上传文件的目录（ /home/minio/data）
```bash
mkdir -p /opt/app/minio/config
mkdir -p /opt/app/minio/data
chmod -R 766 /opt/app/minio
```

2. 运行容器
```bash
docker run -p 9000:9000 -p 9090:9090 \
     --net=host \
     --name minio \
     -d --restart=always \
     -e "MINIO_ACCESS_KEY=admin" \
     -e "MINIO_SECRET_KEY=123456aA" \
     -v /opt/app/minio/data:/data \
     -v /opt/app/minio/config:/root/.minio \
     minio/minio server \
     /data --console-address ":9090" -address ":9000"
```
> 说明:
> 9090端口指的是minio的客户端端口
> 
> MINIO_ACCESS_KEY ：账号(账号长度必须大于等于5)
> 
> MINIO_SECRET_KEY ：密码(密码长度必须大于等于8位)

3. 访问操作

访问：http://127.0.0.1:9090/login 

用户名/密码:  admin/123456aA 