package com.github.zhangyucheng;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    public void start() throws IOException {

        /**
         * 1. 创建Selector
         */
        Selector selector = Selector.open();

        /**
         * 2. 创建channel通道
         */
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        /**
         * 3. 为channel通道绑定监听端口
         */
        serverSocketChannel.bind(new InetSocketAddress(8000));

        /**
         * 4. 设置channel为非阻塞
         */
        serverSocketChannel.configureBlocking(false);

        /**
         * 5. 将channel注册到selector上，监听连接事件
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器启动成功！");

        /**
         * 6. 循环等待新接入的连接
         */
        while (true) {
            /**
             * 获取可用channel数量
             */
            int readyChannels = selector.select();

            /**
             * 防止selector在类linux系统中空轮询
             */
            if (readyChannels == 0 ) continue;

            Set<SelectionKey> selectionKeys =  selector.selectedKeys();

            Iterator iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey selectionKey = (SelectionKey) iterator.next();

                /**
                 * 移除Set中的当前selectionKey
                 */
                iterator.remove();

                /**
                 * 7. 根据就绪状态，调用对应方法处理业务逻辑
                 */

                /**
                 * 如果是接入事件
                 */
                if (selectionKey.isAcceptable()) {
                    acceptHandler(serverSocketChannel, selector);
                }

                /**
                 * 如果是可读事件
                 */
                if (selectionKey.isReadable()) {
                    readHandler(selectionKey, selector);
                }
            }


        }


    }

    private void acceptHandler(ServerSocketChannel serverSocketChannel,
                               Selector selector) throws IOException {
        /**
         * 接入事件，创建socketChannel
         */
        SocketChannel socketChannel = serverSocketChannel.accept();

        /**
         * 将socketChannel设置为非阻塞工作模式
         */
        socketChannel.configureBlocking(false);

        /**
         * 将channel注册到selector上，监听可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 回复客户端提示信息
         */

        socketChannel.write(Charset.forName("UTF-8")
                .encode("你与聊天室里其他人都不是朋友关系，请注意隐私安全！"));
    }

    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException{
        /**
         * 要从selectionKey中获取到已经就绪的channel
         */

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        /**
         * 创建buffer
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        /**
         * 循环读取客户端请求信息
         */
        String request = "";
        while (socketChannel.read(byteBuffer) > 0) {
            /**
             * 切换buffer为读模式
             */
            byteBuffer.flip();

            /**
             * 读取buffer中的内容
             */
            request += Charset.forName("UTF-8").decode(byteBuffer);
        }

        /**
         * 将channel再次注册到selector上，监听他的可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);

        /**
         * 将客户端发送的请求信息，广播给其他客户端
         */
        if (request.length() > 0) {
            System.out.println("::" + request);
            broadCast(selector, socketChannel, request);
        }
    }

    private void broadCast(Selector selector, SocketChannel sourceChannel, String request) {
        /**
         * 获取所有已接入的客户端channel
         */
        Set<SelectionKey> selectionKeySet = selector.keys();
        selectionKeySet.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();

            try {
                if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel) {
                    ((SocketChannel) targetChannel).write(Charset.forName("UTF-8").encode(request));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer();
        nioServer.start();
    }
}
