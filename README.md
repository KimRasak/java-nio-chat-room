# java-nio-chat-room

# 参考
1. [简易的自动发消息机](https://www.cnblogs.com/snailclimb/p/9086334.html)
2. [nio聊天室](https://blog.csdn.net/weiyang000/article/details/83379740)

# 前提知识
1. 理解Selector、Channel、Buffer、SelectionKey
2. 了解建立连接、通信、断开连接时的机制

# 通信机制设计
![](https://upload-images.jianshu.io/upload_images/7547741-c3bfd6e09bd994ec.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# 功能
1. 客户端可发起对服务器的连接，登陆后服务器广播会通知其他聊天室用户
2. 客户端连接后，可发送消息给服务器，服务器广播消息至其他聊天室用户
3. 客户端断开连接，服务器可响应并通知其他聊天室用户有人离开房间
