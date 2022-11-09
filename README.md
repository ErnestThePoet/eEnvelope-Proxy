# eEnvelope-Proxy

使用Java编写的HTTP代理服务器，使用电子信封加密策略对数据传输进行加密。  
电子信封加密原理：  

<image src="https://github.com/ErnestThePoet/eEnvelope-Proxy/blob/master/principles.jpg"/>

## 使用方法

### 编辑配置文件
在开始使用前，需要编辑配置文件来设置代理服务器的运行选项。`configs`目录下的`configs_client.json`和`configs_server.json`分别是客户端和服务端的配置文件。

客户端设置选项说明如下：  
* `port`: `number`,客户端代理服务器监听的端口号；  
* `targetHostPatterns`: `Array<string>`,正则表达式字符串数组，指定了所有需要被加密代理的主机名匹配规则。所有发往其余主机的请求都会被代理服务器丢弃。

服务端设置选项说明如下：  
* `port`: `number`,服务端代理服务器监听的端口号；  
* `proxyPass`: `string`,代理转发目的主机名。对于每一个发送到服务端代理服务器的请求，其请求头中的`Host`字段都会被改写为此主机名，然后该请求会被转发到此主机。

### 编写证书提供与校验类
本项目不提供统一的证书格式标准，您需要自行实现`certificate`包中的`CertificateProvider`和`CertificateValidator`接口，实现您自己的证书提供和校验规则，以及提供私钥和从证书中提取公钥的方法。实现后，请相应地修改两个接口的`getInstance`方法使其返回您的实现类对象。

### 启动程序
本代理服务器需要在客户端和服务端同时运行。客户端运行时需要添加命令行参数`CLIENT`，服务端则添加`SERVER`。
客户端代理服务器运行后需要在系统中设置全局代理。在服务端运行一个纯HTTP后端服务，如果您的配置文件正确，那么您就可以在客户端通过`服务端IP:服务端代理服务器监听端口号`正常访问服务端的HTTP服务。

### 在同一台电脑上调试的说明
本项目也可以在同一台电脑上同时运行客户端与服务端进行调试。为了确保所有的请求都通过客户端代理服务器，而不会被意外地直接发送给服务端代理服务器，请在Windows系统的代理服务器设置中，清空“请勿对以下列条目开头的地址使用代理服务器”输入框的所有内容，并取消“请勿将代理服务器用于本地(Intranet)地址”的勾选。同时，发起HTTP请求时，主机名请使用`本机IPv4地址:服务端代理服务器监听端口号`而不要使用`localhost:服务端代理服务器监听端口号`或`127.0.0.1:服务端代理服务器监听端口号`。

### 对前端的说明
要使用此代理服务器，请确保前端页面引用的所有资源的URL全部在被代理的主机名下。所有从第三方CDN引用的资源都不会被成功加载。

### 安全性说明
此代理服务器基于电子信封的加密策略实现，不能提供前向安全性，且不能有效防范中间人攻击。基于最新的TLS1.3协议实现的代理服务器[TLS-Proxy](https://github.com/ErnestThePoet/TLS-Proxy)可以提供完善的消息加密和认证机制。